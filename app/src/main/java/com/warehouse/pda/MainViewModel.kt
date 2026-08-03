package com.warehouse.pda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.warehouse.pda.data.BarcodeValidationRequest
import com.warehouse.pda.data.BarcodeValidationResult
import com.warehouse.pda.data.CurrentUser
import com.warehouse.pda.data.InboundSubmitRequest
import com.warehouse.pda.data.OutboundLineSubmitRequest
import com.warehouse.pda.data.OutboundSubmitRequest
import com.warehouse.pda.data.PendingSubmission
import com.warehouse.pda.data.PdaReleaseInfo
import com.warehouse.pda.data.StorageLocation
import com.warehouse.pda.data.SubmissionPolicy
import com.warehouse.pda.data.SubmissionReceipt
import com.warehouse.pda.data.TrackingDetailResult
import com.warehouse.pda.data.TrackingOutboundSubmitRequest
import com.warehouse.pda.data.TrackingReturnSubmitRequest
import com.warehouse.pda.data.TrackingValidationRequest
import com.warehouse.pda.data.WarehouseRepository
import com.warehouse.pda.data.WarehouseState
import com.warehouse.pda.data.shouldRetainPendingSubmission
import com.warehouse.pda.data.isSessionExpired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class OperationGroup {
  Inbound,
  Outbound
}

enum class PdaOperation(
  val title: String,
  val description: String,
  val group: OperationGroup
) {
  FactoryInbound("厂家到货", "按货品数量完成厂家到货入库", OperationGroup.Inbound),
  TerminalInbound("终端退换货", "登记生产日期并扫码回仓", OperationGroup.Inbound),
  SalesReturn("扫码回库", "未售货物与店铺退货统一扫码回仓", OperationGroup.Inbound),
  DirectOutbound("快速出库", "混合箱码连续扫描，无需选择商品", OperationGroup.Outbound)
}

enum class MainTab(val title: String) {
  Home("首页"),
  Query("查询"),
  Inbound("回库"),
  Outbound("出库"),
  Profile("我的")
}

sealed interface AppRoute {
  data object Login : AppRoute
  data class Main(val tab: MainTab) : AppRoute
  data class OperationConfig(val operation: PdaOperation) : AppRoute
  data class OperationScan(val operation: PdaOperation) : AppRoute
}

data class LoginFormState(
  val serverUrl: String = "",
  val username: String = "",
  val password: String = "",
  val rememberCredentials: Boolean = true
)

data class OperationFormState(
  val factoryWarehouseId: String = "",
  val factoryLocationId: String = "",
  val factoryGoodsId: String = "",
  val factoryQuantity: String = "1",
  val terminalWarehouseId: String = "",
  val terminalLocationId: String = "",
  val terminalGoodsId: String = "",
  val terminalStoreId: String = "",
  val terminalProductionDate: String = "",
  val directSourceWarehouseId: String = "",
  val directGoodsId: String = "",
  val directDestinationType: String = "sales",
  val directTargetWarehouseId: String = "",
  val directTargetLocationId: String = "",
  val directSalespersonId: String = "",
  val returnWarehouseId: String = "",
  val returnLocationId: String = ""
)

data class OutboundLineState(
  val goodsId: String,
  val targetQuantity: String = "",
  val barcodes: List<String> = emptyList(),
  val reviews: Map<String, ReviewState> = emptyMap(),
  val validationTokens: Map<String, String> = emptyMap()
)

data class QueryFormState(
  val barcodeInput: String = "",
  val loading: Boolean = false,
  val result: TrackingDetailResult? = null,
  val resultBarcode: String = ""
)

data class StatusMessage(
  val tone: MessageTone,
  val text: String
)

enum class MessageTone {
  Success,
  Error,
  Info
}

data class ReviewState(
  val label: String,
  val detail: String,
  val isValid: Boolean
)

enum class ScanSoundTone {
  Success,
  Error
}

data class ScanSoundCue(
  val id: String = UUID.randomUUID().toString(),
  val tone: ScanSoundTone,
  val announcement: String? = null
)

data class RecentActivity(
  val id: String,
  val title: String,
  val subtitle: String,
  val timeLabel: String,
  val tone: MessageTone
)

data class AppUpdateState(
  val checking: Boolean = false,
  val release: PdaReleaseInfo? = null
) {
  val hasUpdate: Boolean
    get() = (release?.versionCode ?: 0) > BuildConfig.VERSION_CODE
}

data class AppUiState(
  val booting: Boolean = true,
  val route: AppRoute = AppRoute.Login,
  val loginPending: Boolean = false,
  val loadingMasterData: Boolean = false,
  val currentUser: CurrentUser? = null,
  val loginForm: LoginFormState = LoginFormState(),
  val masterData: WarehouseState? = null,
  val selectedWorkWarehouseId: String = "",
  val formState: OperationFormState = OperationFormState(),
  val barcodeInputs: Map<PdaOperation, String> = PdaOperation.entries.associateWith { "" },
  val barcodeLists: Map<PdaOperation, List<String>> = PdaOperation.entries.associateWith { emptyList() },
  val barcodeReviews: Map<PdaOperation, Map<String, ReviewState>> = PdaOperation.entries.associateWith { emptyMap() },
  val barcodeValidationTokens: Map<PdaOperation, Map<String, String>> = PdaOperation.entries.associateWith { emptyMap() },
  val outboundLines: List<OutboundLineState> = emptyList(),
  val selectedOutboundGoodsId: String = "",
  val submitting: Map<PdaOperation, Boolean> = PdaOperation.entries.associateWith { false },
  val queryForm: QueryFormState = QueryFormState(),
  val inboundTodayCount: Int = 0,
  val outboundTodayCount: Int = 0,
  val message: StatusMessage? = null,
  val scanSoundCue: ScanSoundCue? = null,
  val lastSubmitSummary: String? = null,
  val recentActivities: List<RecentActivity> = emptyList(),
  val appUpdateState: AppUpdateState = AppUpdateState(),
  val pendingSubmission: PendingSubmission? = null,
  val pendingSubmissionRetrying: Boolean = false
)

class MainViewModel(
  private val repository: WarehouseRepository
) : ViewModel() {
  private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  private var sessionCheckInProgress = false

  private val _uiState = MutableStateFlow(
    AppUiState(
      loginForm = savedLoginForm(),
      pendingSubmission = repository.getPendingSubmission()
    )
  )
  val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

  init {
    observeSessionExpiration()
    bootstrap()
  }

  private fun observeSessionExpiration() {
    viewModelScope.launch {
      repository.sessionExpiredEvents.collect {
        expireSession()
      }
    }
  }

  fun verifySession() {
    val state = _uiState.value
    if (state.booting || state.currentUser == null || sessionCheckInProgress) return

    sessionCheckInProgress = true
    viewModelScope.launch {
      runCatching {
        repository.getCurrentUser(_uiState.value.loginForm.serverUrl)
      }.onFailure { error ->
        if (isSessionExpired(error)) expireSession()
      }
      sessionCheckInProgress = false
    }
  }

  private fun expireSession() {
    _uiState.update { state ->
      if (state.currentUser == null && state.route == AppRoute.Login) return@update state

      val pending = state.pendingSubmission ?: repository.getPendingSubmission()
      val hasScannedBarcodes = state.barcodeLists.values.any { it.isNotEmpty() } ||
        state.outboundLines.any { it.barcodes.isNotEmpty() }
      val message = when {
        pending != null -> "登录已失效，请重新登录；结果待确认业务已保留"
        hasScannedBarcodes -> "登录已失效，请重新登录；当前已扫条码已保留"
        else -> "登录已失效，请重新登录"
      }

      state.copy(
        route = AppRoute.Login,
        currentUser = null,
        loginPending = false,
        loadingMasterData = false,
        submitting = PdaOperation.entries.associateWith { false },
        pendingSubmission = pending,
        pendingSubmissionRetrying = false,
        queryForm = state.queryForm.copy(loading = false),
        appUpdateState = state.appUpdateState.copy(checking = false),
        message = StatusMessage(MessageTone.Info, message)
      )
    }
  }

  fun bootstrap() {
    viewModelScope.launch {
      val loginForm = savedLoginForm()
      val serverUrl = loginForm.serverUrl
      _uiState.update {
        it.copy(
          booting = true,
          loginForm = loginForm,
          message = null
        )
      }

      val healthResult = runCatching { repository.checkHealth(serverUrl) }
      val restoredUser = if (healthResult.isSuccess) {
        runCatching {
          repository.getCurrentUser(serverUrl)
        }.getOrElse {
          autoLoginIfPossible(loginForm)
        }
      } else {
        null
      }

      if (restoredUser != null) {
        _uiState.update {
          it.copy(
            currentUser = restoredUser,
            route = AppRoute.Main(MainTab.Home)
          )
        }
        loadMasterData()
      } else {
        _uiState.update {
          it.copy(
            currentUser = null,
            route = AppRoute.Login,
            message = healthResult.exceptionOrNull()?.let { error ->
              StatusMessage(MessageTone.Error, error.message ?: "服务器暂不可用")
            }
          )
        }
      }

      _uiState.update { it.copy(booting = false) }
    }
  }

  fun updateServerUrl(value: String) {
    _uiState.update { it.copy(loginForm = it.loginForm.copy(serverUrl = value)) }
  }

  fun updateUsername(value: String) {
    _uiState.update { it.copy(loginForm = it.loginForm.copy(username = value)) }
  }

  fun updatePassword(value: String) {
    _uiState.update { it.copy(loginForm = it.loginForm.copy(password = value)) }
  }

  fun updateRememberCredentials(value: Boolean) {
    _uiState.update { it.copy(loginForm = it.loginForm.copy(rememberCredentials = value)) }
  }

  fun updateQueryInput(value: String) {
    _uiState.update { it.copy(queryForm = it.queryForm.copy(barcodeInput = value)) }
  }

  fun clearQueryResult() {
    _uiState.update { it.copy(queryForm = QueryFormState(barcodeInput = it.queryForm.barcodeInput)) }
  }

  fun clearMessage() {
    _uiState.update { it.copy(message = null) }
  }

  fun checkForUpdates(manual: Boolean = true) {
    val serverUrl = _uiState.value.loginForm.serverUrl
    if (serverUrl.isBlank()) {
      if (manual) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请先配置服务器地址")) }
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(appUpdateState = it.appUpdateState.copy(checking = true)) }
      runCatching {
        repository.getPdaReleaseInfo(serverUrl)
      }.onSuccess { release ->
        _uiState.update { state ->
          state.copy(appUpdateState = AppUpdateState(checking = false, release = release))
        }
        if (manual) {
          val text = if (release.versionCode > BuildConfig.VERSION_CODE) {
            "发现新版本 ${release.versionName}"
          } else {
            "当前已是最新版本"
          }
          _uiState.update { it.copy(message = StatusMessage(MessageTone.Info, text)) }
        }
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        _uiState.update {
          it.copy(
            appUpdateState = it.appUpdateState.copy(checking = false),
            message = if (manual) StatusMessage(MessageTone.Error, error.message ?: "检查更新失败") else it.message
          )
        }
      }
    }
  }

  fun login() {
    val form = _uiState.value.loginForm
    val trimmedUsername = form.username.trim()
    viewModelScope.launch {
      _uiState.update { it.copy(loginPending = true, message = null) }
      runCatching {
        repository.checkHealth(form.serverUrl)
        repository.login(form.serverUrl, trimmedUsername, form.password).also {
          repository.saveServerUrl(form.serverUrl)
        }
      }.onSuccess { user ->
        val credentialsSaved = if (form.rememberCredentials) {
          repository.saveRememberCredentials(true)
          repository.saveCredentials(trimmedUsername, form.password).also { saved ->
            if (!saved) {
              repository.saveRememberCredentials(false)
            }
          }
        } else {
          repository.saveRememberCredentials(false)
          repository.clearCredentials()
          false
        }
        if (!credentialsSaved && form.rememberCredentials) {
          repository.clearCredentials()
        }
        _uiState.update {
          it.copy(
            currentUser = user,
            route = AppRoute.Main(MainTab.Home),
            loginPending = false,
            loginForm = it.loginForm.copy(
              username = trimmedUsername,
              password = if (credentialsSaved) form.password else "",
              rememberCredentials = credentialsSaved
            ),
            message = StatusMessage(
              if (form.rememberCredentials && !credentialsSaved) MessageTone.Info else MessageTone.Success,
              if (form.rememberCredentials && !credentialsSaved) "登录成功，但本机加密存储不可用，未保存账号密码" else "登录成功"
            )
          )
        }
        loadMasterData()
        resumePendingBarcodeValidations()
      }.onFailure { error ->
        _uiState.update {
          it.copy(
            loginPending = false,
            message = StatusMessage(MessageTone.Error, error.message ?: "登录失败")
          )
        }
      }
    }
  }

  fun logout() {
    val serverUrl = _uiState.value.loginForm.serverUrl
    viewModelScope.launch {
      runCatching { repository.logout(serverUrl) }
      _uiState.update {
        it.copy(
          route = AppRoute.Login,
          currentUser = null,
          loginForm = savedLoginForm().copy(serverUrl = it.loginForm.serverUrl),
          masterData = null,
          selectedWorkWarehouseId = "",
          formState = OperationFormState(),
          queryForm = QueryFormState(),
          barcodeLists = PdaOperation.entries.associateWith { emptyList() },
          barcodeReviews = PdaOperation.entries.associateWith { emptyMap() },
          barcodeValidationTokens = PdaOperation.entries.associateWith { emptyMap() },
          barcodeInputs = PdaOperation.entries.associateWith { "" },
          outboundLines = emptyList(),
          selectedOutboundGoodsId = "",
          inboundTodayCount = 0,
          outboundTodayCount = 0,
          lastSubmitSummary = null,
          recentActivities = emptyList(),
          message = StatusMessage(MessageTone.Info, "已退出登录")
        )
      }
    }
  }

  private fun savedLoginForm(): LoginFormState {
    val rememberCredentials = repository.getRememberCredentials()
    return LoginFormState(
      serverUrl = repository.getSavedServerUrl(),
      username = if (rememberCredentials) repository.getSavedUsername() else "",
      password = if (rememberCredentials) repository.getSavedPassword() else "",
      rememberCredentials = rememberCredentials
    )
  }

  private suspend fun autoLoginIfPossible(loginForm: LoginFormState): CurrentUser? {
    val username = loginForm.username.trim()
    if (!loginForm.rememberCredentials || loginForm.serverUrl.isBlank() || username.isBlank() || loginForm.password.isBlank()) {
      return null
    }

    return runCatching {
      repository.login(loginForm.serverUrl, username, loginForm.password)
    }.getOrNull()
  }

  fun openTab(tab: MainTab) {
    when (tab) {
      MainTab.Inbound -> openOperation(PdaOperation.SalesReturn)
      MainTab.Outbound -> openOperation(PdaOperation.DirectOutbound)
      else -> _uiState.update { it.copy(route = AppRoute.Main(tab), message = null) }
    }
  }

  fun openOperation(operation: PdaOperation) {
    if (!canOperate()) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "当前账号无权限执行扫码作业")) }
      return
    }
    _uiState.update { it.copy(route = AppRoute.OperationConfig(operation), message = null) }
  }

  fun startScanning(operation: PdaOperation) {
    if (operation == PdaOperation.FactoryInbound) {
      _uiState.update {
        it.copy(
          route = AppRoute.OperationConfig(operation),
          message = StatusMessage(MessageTone.Info, "厂家到货按数量入库，无需进入扫码页")
        )
      }
      return
    }

    val contextError = trackingContextError(operation, _uiState.value.formState)
    if (contextError != null) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, contextError)) }
      return
    }

    _uiState.update { state -> state.copy(route = AppRoute.OperationScan(operation), message = null) }
  }

  fun goBackFromOperation(operation: PdaOperation, fromScan: Boolean) {
    _uiState.update {
      it.copy(
        route = if (fromScan) AppRoute.OperationConfig(operation) else AppRoute.Main(defaultTabFor(operation)),
        message = null
      )
    }
  }

  fun cancelDirectOutbound() {
    _uiState.update {
      it.copy(
        route = AppRoute.Main(MainTab.Outbound),
        outboundLines = emptyList(),
        selectedOutboundGoodsId = "",
        barcodeInputs = it.barcodeInputs + (PdaOperation.DirectOutbound to ""),
        message = null
      )
    }
  }

  fun updateBarcodeInput(operation: PdaOperation, value: String) {
    _uiState.update { it.copy(barcodeInputs = it.barcodeInputs + (operation to value)) }
  }

  fun clearBarcodeInput(operation: PdaOperation) {
    _uiState.update { it.copy(barcodeInputs = it.barcodeInputs + (operation to "")) }
  }

  fun updateForm(transform: (OperationFormState) -> OperationFormState) {
    _uiState.update { state ->
      val next = transform(state.formState).normalized()
      val terminalLocked = state.barcodeLists[PdaOperation.TerminalInbound].orEmpty().isNotEmpty()
      val outboundLocked = state.barcodeLists[PdaOperation.DirectOutbound].orEmpty().isNotEmpty()
      val returnLocked = state.barcodeLists[PdaOperation.SalesReturn].orEmpty().isNotEmpty()
      when {
        terminalLocked && next.terminalGoodsId != state.formState.terminalGoodsId -> state.copy(
          message = StatusMessage(MessageTone.Info, "已扫码后不能更换货物，请先清空条码清单")
        )
        outboundLocked && next.outboundContext() != state.formState.outboundContext() -> state.copy(
          message = StatusMessage(MessageTone.Info, "已扫码后不能更换出库仓库或去向，请先清空条码清单")
        )
        returnLocked && next.returnWarehouseId != state.formState.returnWarehouseId -> state.copy(
          message = StatusMessage(MessageTone.Info, "已扫码后不能更换回库仓库，请先清空条码清单")
        )
        else -> state.copy(formState = next)
      }
    }
  }

  fun selectWorkWarehouse(warehouseId: String) {
    _uiState.update { state ->
      val hasUnfinishedScan = state.barcodeLists[PdaOperation.DirectOutbound].orEmpty().isNotEmpty() ||
        state.barcodeLists[PdaOperation.SalesReturn].orEmpty().isNotEmpty()
      if (hasUnfinishedScan) {
        return@update state.copy(
          message = StatusMessage(MessageTone.Info, "当前有未提交的扫码作业，请先继续处理或清空条码")
        )
      }
      val masterData = state.masterData ?: return@update state
      val selectedWarehouseId = existingWorkWarehouseId(masterData, warehouseId)
      if (selectedWarehouseId.isBlank()) {
        return@update state
      }
      state.copy(
        selectedWorkWarehouseId = selectedWarehouseId,
        formState = state.formState.withWorkWarehouseDefaults(masterData, selectedWarehouseId).normalized(),
        message = null
      )
    }
  }

  fun addBarcodes(operation: PdaOperation, rawInput: String? = null) {
    val contextError = trackingContextError(operation, _uiState.value.formState)
    if (contextError != null) {
      _uiState.update {
        it.copy(
          message = StatusMessage(MessageTone.Error, contextError),
          scanSoundCue = ScanSoundCue(
            tone = ScanSoundTone.Error,
            announcement = "扫码错误，$contextError"
          )
        )
      }
      return
    }

    val input = rawInput ?: _uiState.value.barcodeInputs[operation].orEmpty()
    val candidates = parseBarcodes(input)
    if (candidates.isEmpty()) return

    val existing = _uiState.value.barcodeLists[operation].orEmpty()
    val fresh = candidates.filterNot(existing::contains)
    if (fresh.isEmpty()) {
      _uiState.update {
        val validCount = it.validBarcodeCount(operation)
        it.copy(
          barcodeInputs = it.barcodeInputs + (operation to ""),
          message = StatusMessage(MessageTone.Info, "这些条码已在当前清单中"),
          scanSoundCue = ScanSoundCue(
            tone = ScanSoundTone.Error,
            announcement = "重复扫码，当前有效 $validCount 件"
          )
        )
      }
      return
    }

    val remainingCapacity = SubmissionPolicy.remainingCapacity(existing.size)
    if (!SubmissionPolicy.accepts(existing.size, fresh.size)) {
      _uiState.update {
        val validCount = it.validBarcodeCount(operation)
        it.copy(
          message = StatusMessage(MessageTone.Error, "单次最多 $MAX_BARCODES_PER_SUBMISSION 个条码，当前还可添加 $remainingCapacity 个"),
          scanSoundCue = ScanSoundCue(
            tone = ScanSoundTone.Error,
            announcement = "已达到单次扫码上限，当前有效 $validCount 件"
          )
        )
      }
      return
    }

    val validationToken = UUID.randomUUID().toString()

    _uiState.update {
      it.copy(
        barcodeLists = it.barcodeLists + (operation to (existing + fresh)),
        barcodeInputs = it.barcodeInputs + (operation to ""),
        barcodeValidationTokens = it.barcodeValidationTokens + (
          operation to (it.barcodeValidationTokens[operation].orEmpty() + fresh.associateWith { validationToken })
        )
      )
    }

    validateBarcodesForOperation(operation, _uiState.value.formState, fresh, validationToken)
  }

  private fun validateBarcodesForOperation(
    operation: PdaOperation,
    formState: OperationFormState,
    barcodes: List<String>,
    validationToken: String
  ) {
    viewModelScope.launch {
      runCatching {
        if (operation == PdaOperation.DirectOutbound || operation == PdaOperation.SalesReturn) {
          repository.validateTrackingBarcodes(
            _uiState.value.loginForm.serverUrl,
            buildTrackingValidationRequest(operation, formState, barcodes)
          )
        } else {
          repository.validateBarcodes(
            _uiState.value.loginForm.serverUrl,
            buildValidationRequest(operation, formState, barcodes)
          )
        }
      }.onSuccess { results ->
        mergeValidationResults(operation, validationToken, results)
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        val failureReviews = barcodes.associateWith {
          ReviewState(
            label = "校验失败",
            detail = error.message ?: "条码校验失败",
            isValid = false
          )
        }
        _uiState.update {
          val currentTokens = it.barcodeValidationTokens[operation].orEmpty()
          val currentBarcodes = it.barcodeLists[operation].orEmpty().toSet()
          val applicable = failureReviews.filterKeys { barcode ->
            barcode in currentBarcodes && currentTokens[barcode] == validationToken
          }
          val merged = it.barcodeReviews[operation].orEmpty() + applicable
          val validCount = currentBarcodes.count { barcode -> merged[barcode]?.isValid == true }
          it.copy(
            barcodeReviews = it.barcodeReviews + (operation to merged),
            message = StatusMessage(MessageTone.Error, error.message ?: "条码校验失败"),
            scanSoundCue = ScanSoundCue(
              tone = ScanSoundTone.Error,
              announcement = "扫码校验失败，当前有效 $validCount 件"
            )
          )
        }
      }
    }
  }

  private fun resumePendingBarcodeValidations() {
    PdaOperation.entries.forEach { operation ->
      if (operation == PdaOperation.FactoryInbound) return@forEach

      val state = _uiState.value
      val pendingBarcodes = state.barcodeLists[operation].orEmpty().filter { barcode ->
        state.barcodeReviews[operation].orEmpty()[barcode] == null
      }
      if (pendingBarcodes.isEmpty()) return@forEach
      if (trackingContextError(operation, state.formState) != null) return@forEach

      val validationToken = UUID.randomUUID().toString()
      _uiState.update {
        it.copy(
          barcodeValidationTokens = it.barcodeValidationTokens + (
            operation to (it.barcodeValidationTokens[operation].orEmpty() + pendingBarcodes.associateWith { validationToken })
          )
        )
      }
      validateBarcodesForOperation(operation, state.formState, pendingBarcodes, validationToken)
    }
  }

  fun selectOutboundLine(goodsId: String) {
    _uiState.update { it.copy(selectedOutboundGoodsId = goodsId, message = null) }
  }

  fun addOrSelectOutboundLine(goodsId: String? = null) {
    val resolvedGoodsId = goodsId ?: _uiState.value.formState.directGoodsId
    if (resolvedGoodsId.isBlank()) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请先选择货物")) }
      return
    }

    _uiState.update { state ->
      val exists = state.outboundLines.any { it.goodsId == resolvedGoodsId }
      state.copy(
        outboundLines = if (exists) state.outboundLines else state.outboundLines + OutboundLineState(goodsId = resolvedGoodsId),
        selectedOutboundGoodsId = resolvedGoodsId,
        message = if (exists) StatusMessage(MessageTone.Error, "该货品已添加，请直接进入扫码") else null
      )
    }
  }

  fun updateOutboundLineTarget(goodsId: String, targetQuantity: String) {
    val cleaned = targetQuantity.filter(Char::isDigit).trimStart('0')
    _uiState.update { state ->
      state.copy(
        outboundLines = state.outboundLines.map { line ->
          if (line.goodsId == goodsId) line.copy(targetQuantity = cleaned) else line
        }
      )
    }
  }

  fun removeOutboundLine(goodsId: String) {
    _uiState.update { state ->
      val nextLines = state.outboundLines.filterNot { it.goodsId == goodsId }
      val nextSelected = if (state.selectedOutboundGoodsId == goodsId) {
        nextLines.firstOrNull()?.goodsId.orEmpty()
      } else {
        state.selectedOutboundGoodsId
      }
      state.copy(outboundLines = nextLines, selectedOutboundGoodsId = nextSelected)
    }
  }

  private fun addOutboundBarcodes(rawInput: String? = null) {
    val state = _uiState.value
    val input = rawInput ?: state.barcodeInputs[PdaOperation.DirectOutbound].orEmpty()
    val candidates = parseBarcodes(input)
    if (candidates.isEmpty()) return

    val selectedGoodsId = state.selectedOutboundGoodsId.ifBlank { state.formState.directGoodsId }
    if (selectedGoodsId.isBlank()) {
      _uiState.update {
        it.copy(
          message = StatusMessage(MessageTone.Error, "请先选择当前货品行"),
          scanSoundCue = ScanSoundCue(tone = ScanSoundTone.Error)
        )
      }
      return
    }

    val allExisting = state.outboundLines.flatMap { it.barcodes }.toSet()
    val fresh = candidates.filterNot(allExisting::contains)
    if (fresh.isEmpty()) {
      _uiState.update {
        it.copy(
          barcodeInputs = it.barcodeInputs + (PdaOperation.DirectOutbound to ""),
          message = StatusMessage(MessageTone.Info, "这些条码已在当前作业单中"),
          scanSoundCue = ScanSoundCue(tone = ScanSoundTone.Error)
        )
      }
      return
    }

    val remainingCapacity = SubmissionPolicy.remainingCapacity(allExisting.size)
    if (!SubmissionPolicy.accepts(allExisting.size, fresh.size)) {
      _uiState.update {
        it.copy(
          barcodeInputs = it.barcodeInputs + (PdaOperation.DirectOutbound to ""),
          message = StatusMessage(MessageTone.Error, "单次最多 $MAX_BARCODES_PER_SUBMISSION 个条码，当前还可添加 $remainingCapacity 个"),
          scanSoundCue = ScanSoundCue(tone = ScanSoundTone.Error)
        )
      }
      return
    }

    val validationToken = UUID.randomUUID().toString()

    _uiState.update { current ->
      val ensuredLines = if (current.outboundLines.any { it.goodsId == selectedGoodsId }) {
        current.outboundLines
      } else {
        current.outboundLines + OutboundLineState(goodsId = selectedGoodsId)
      }
      current.copy(
        outboundLines = ensuredLines.map { line ->
          if (line.goodsId == selectedGoodsId) {
            line.copy(
              barcodes = line.barcodes + fresh,
              validationTokens = line.validationTokens + fresh.associateWith { validationToken }
            )
          } else line
        },
        selectedOutboundGoodsId = selectedGoodsId,
        barcodeInputs = current.barcodeInputs + (PdaOperation.DirectOutbound to "")
      )
    }

    val validationRequest = BarcodeValidationRequest(
      mode = "warehouse_outbound",
      barcodes = fresh,
      goodsId = selectedGoodsId,
      warehouseId = _uiState.value.formState.directSourceWarehouseId
    )
    viewModelScope.launch {
      runCatching {
        repository.validateBarcodes(_uiState.value.loginForm.serverUrl, validationRequest)
      }.onSuccess { results ->
        mergeOutboundValidationResults(selectedGoodsId, validationToken, results)
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        val failureReviews = fresh.associateWith {
          ReviewState(
            label = "校验失败",
            detail = error.message ?: "条码校验失败",
            isValid = false
          )
        }
        _uiState.update { current ->
          current.copy(
            outboundLines = current.outboundLines.map { line ->
              if (line.goodsId == selectedGoodsId) {
                val currentBarcodes = line.barcodes.toSet()
                val applicable = failureReviews.filterKeys { barcode ->
                  barcode in currentBarcodes && line.validationTokens[barcode] == validationToken
                }
                line.copy(reviews = line.reviews + applicable)
              } else line
            },
            message = StatusMessage(MessageTone.Error, error.message ?: "条码校验失败"),
            scanSoundCue = ScanSoundCue(tone = ScanSoundTone.Error)
          )
        }
      }
    }
  }

  fun removeBarcode(operation: PdaOperation, barcode: String) {
    _uiState.update {
      val nextList = it.barcodeLists[operation].orEmpty().filterNot { item -> item == barcode }
      val nextReviews = it.barcodeReviews[operation].orEmpty().toMutableMap().also { map -> map.remove(barcode) }
      val nextTokens = it.barcodeValidationTokens[operation].orEmpty().toMutableMap().also { map -> map.remove(barcode) }
      it.copy(
        barcodeLists = it.barcodeLists + (operation to nextList),
        barcodeReviews = it.barcodeReviews + (operation to nextReviews),
        barcodeValidationTokens = it.barcodeValidationTokens + (operation to nextTokens)
      )
    }
  }

  fun clearBarcodes(operation: PdaOperation) {
    _uiState.update {
      it.copy(
        barcodeLists = it.barcodeLists + (operation to emptyList()),
        barcodeReviews = it.barcodeReviews + (operation to emptyMap()),
        barcodeValidationTokens = it.barcodeValidationTokens + (operation to emptyMap()),
        barcodeInputs = it.barcodeInputs + (operation to "")
      )
    }
  }

  fun removeOutboundBarcode(barcode: String) {
    _uiState.update { state ->
      state.copy(
        outboundLines = state.outboundLines.map { line ->
          if (barcode in line.barcodes) {
            line.copy(
              barcodes = line.barcodes.filterNot { it == barcode },
              reviews = line.reviews.toMutableMap().also { it.remove(barcode) },
              validationTokens = line.validationTokens.toMutableMap().also { it.remove(barcode) }
            )
          } else {
            line
          }
        }
      )
    }
  }

  fun queryBarcode(rawInput: String? = null) {
    val barcode = (rawInput ?: _uiState.value.queryForm.barcodeInput).trim()
    if (barcode.isBlank()) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请输入条码")) }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(queryForm = it.queryForm.copy(loading = true, result = null, resultBarcode = barcode)) }
      runCatching {
        repository.getTrackingDetail(_uiState.value.loginForm.serverUrl, barcode)
      }.onSuccess { result ->
        _uiState.update {
          it.copy(
            queryForm = it.queryForm.copy(loading = false, result = result, resultBarcode = barcode),
            recentActivities = pushRecentActivity(
              it.recentActivities,
              RecentActivity(
                id = UUID.randomUUID().toString(),
                title = "扫码查询 $barcode",
                subtitle = describeTrackingResult(result),
                timeLabel = nowLabel(),
                tone = MessageTone.Info
              )
            )
          )
        }
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        _uiState.update {
          it.copy(
            queryForm = it.queryForm.copy(loading = false, result = null, resultBarcode = barcode),
            message = StatusMessage(MessageTone.Error, error.message ?: "查询失败")
          )
        }
      }
    }
  }

  fun submit(operation: PdaOperation) {
    if (_uiState.value.submitting[operation] == true || _uiState.value.pendingSubmissionRetrying) return
    if (_uiState.value.pendingSubmission != null) {
      retryPendingSubmission()
      return
    }
    viewModelScope.launch {
      yield()

      val latestState = _uiState.value
      val normalizedForm = latestState.formState.normalized()

      val barcodes = latestState.barcodeLists[operation].orEmpty()
      val reviews = latestState.barcodeReviews[operation].orEmpty()

      val contextError = trackingContextError(operation, normalizedForm)
      if (contextError != null) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, contextError)) }
        return@launch
      }

      if (operation != PdaOperation.FactoryInbound && barcodes.isEmpty()) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请先扫描条码")) }
        return@launch
      }
      if (barcodes.size > MAX_BARCODES_PER_SUBMISSION) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "单次最多提交 $MAX_BARCODES_PER_SUBMISSION 个条码")) }
        return@launch
      }
      if (barcodes.any { reviews[it] == null }) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Info, "仍有条码正在校验，请稍后提交")) }
        return@launch
      }
      if (barcodes.any { reviews[it]?.isValid == false }) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "存在异常条码，不能提交")) }
        return@launch
      }
      if (operation == PdaOperation.TerminalInbound && normalizedForm.terminalProductionDate.isBlank()) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "终端店铺退换货入库必须登记生产日期")) }
        return@launch
      }
      if (operation == PdaOperation.FactoryInbound && normalizePositiveInt(normalizedForm.factoryQuantity) == null) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "厂家到货入库数量必须为正整数")) }
        return@launch
      }
      if (normalizedForm != latestState.formState) {
        _uiState.update { it.copy(formState = normalizedForm) }
      }
      _uiState.update {
        it.copy(
          submitting = it.submitting + (operation to true),
          message = null
        )
      }

      runCatching {
        submitByOperation(operation, normalizedForm, barcodes, UUID.randomUUID().toString())
      }.onSuccess { result ->
        repository.clearPendingSubmission()
        val itemCount = result.quantity
        _uiState.update { state ->
          state.copy(
            route = AppRoute.Main(MainTab.Home),
            submitting = state.submitting + (operation to false),
            barcodeLists = state.barcodeLists + (operation to emptyList()),
            barcodeReviews = state.barcodeReviews + (operation to emptyMap()),
            barcodeInputs = state.barcodeInputs + (operation to ""),
            inboundTodayCount = state.inboundTodayCount + if (operation.group == OperationGroup.Inbound) itemCount else 0,
            outboundTodayCount = state.outboundTodayCount + if (operation.group == OperationGroup.Outbound) itemCount else 0,
            lastSubmitSummary = "${operation.title}：单号 ${result.orderId}，共 $itemCount 件",
            recentActivities = pushRecentActivity(
              state.recentActivities,
              RecentActivity(
                id = UUID.randomUUID().toString(),
                title = operation.title,
                subtitle = "单号 ${result.orderId}，共 $itemCount 件",
                timeLabel = nowLabel(),
                tone = MessageTone.Success
              )
            ),
            message = StatusMessage(MessageTone.Success, "${operation.title}提交成功，单号 ${result.orderId}"),
            pendingSubmission = null
          )
        }
        loadMasterData()
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        val retainPending = shouldRetainPendingSubmission(error)
        if (!retainPending) repository.clearPendingSubmission()
        _uiState.update {
          it.copy(
            submitting = it.submitting + (operation to false),
            pendingSubmission = if (retainPending) repository.getPendingSubmission() else null,
            message = StatusMessage(
              if (retainPending) MessageTone.Info else MessageTone.Error,
              if (retainPending) "提交结果暂未确认，已安全保留本次业务，请稍后重试" else submitErrorMessage(operation, error)
            )
          )
        }
        if (!retainPending && error.message.orEmpty().contains("库存不足")) loadMasterData()
      }
    }
  }

  fun retryPendingSubmission() {
    val pending = _uiState.value.pendingSubmission ?: repository.getPendingSubmission()
    if (pending == null) {
      _uiState.update { it.copy(pendingSubmission = null, message = StatusMessage(MessageTone.Info, "没有待确认业务")) }
      return
    }
    if (_uiState.value.currentUser == null) {
      _uiState.update { it.copy(pendingSubmission = pending, message = StatusMessage(MessageTone.Info, "请先重新登录，再确认待处理业务")) }
      return
    }
    if (_uiState.value.pendingSubmissionRetrying) return

    viewModelScope.launch {
      _uiState.update { it.copy(pendingSubmission = pending, pendingSubmissionRetrying = true, message = null) }
      runCatching {
        repository.retryPendingSubmission(_uiState.value.loginForm.serverUrl, pending)
      }.onSuccess { result ->
        repository.clearPendingSubmission()
        val itemCount = result.quantity
        _uiState.update {
          it.copy(
            route = AppRoute.Main(MainTab.Home),
            pendingSubmission = null,
            pendingSubmissionRetrying = false,
            lastSubmitSummary = "${pending.summary}：单号 ${result.orderId}，共 $itemCount 件",
            message = StatusMessage(MessageTone.Success, "${pending.summary}已确认成功，单号 ${result.orderId}")
          )
        }
        loadMasterData()
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        val retainPending = shouldRetainPendingSubmission(error)
        if (!retainPending) repository.clearPendingSubmission()
        _uiState.update {
          it.copy(
            pendingSubmission = if (retainPending) pending else null,
            pendingSubmissionRetrying = false,
            message = StatusMessage(
              if (retainPending) MessageTone.Info else MessageTone.Error,
              if (retainPending) "服务器尚未明确确认结果，请稍后继续重试" else (error.message ?: "待确认业务提交失败")
            )
          )
        }
        if (!retainPending) loadMasterData()
      }
    }
  }

  private fun submitErrorMessage(operation: PdaOperation, error: Throwable): String {
    val rawMessage = error.message ?: return "提交失败"
    if (operation == PdaOperation.FactoryInbound && rawMessage.contains("条码")) {
      return "当前服务器仍是旧版入库接口，厂家数量入库需要先部署新版后端"
    }
    if (
      operation == PdaOperation.DirectOutbound &&
      (rawMessage.contains("prisma", ignoreCase = true) ||
        rawMessage.contains("findUnique", ignoreCase = true) ||
        rawMessage.contains("uuid", ignoreCase = true))
    ) {
      return "出库参数不完整，请检查出库仓库、出库去向和库存后再提交"
    }
    return rawMessage
  }

  private suspend fun submitOutboundWorkOrder(
    formState: OperationFormState,
    lines: List<OutboundLineState>
  ) {
    val masterData = _uiState.value.masterData
    if (masterData == null) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "基础数据未加载完成，请稍后重试")) }
      return
    }

    val sourceWarehouse = masterData.warehouses.firstOrNull {
      it.id == formState.directSourceWarehouseId && it.status == "enabled"
    }
    if (sourceWarehouse == null) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请选择有效的出库仓库")) }
      return
    }

    val activeLines = lines.filter { it.barcodes.isNotEmpty() || it.targetQuantity.isNotBlank() }
    if (activeLines.isEmpty()) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请先添加货品行并扫描条码")) }
      return
    }
    val totalBarcodes = activeLines.sumOf { it.barcodes.size }
    if (totalBarcodes > MAX_BARCODES_PER_SUBMISSION) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "单次最多提交 $MAX_BARCODES_PER_SUBMISSION 个条码")) }
      return
    }
    if (formState.directDestinationType == "warehouse") {
      if (formState.directTargetWarehouseId.isBlank()) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请选择目标仓库")) }
        return
      }
      if (formState.directTargetWarehouseId == formState.directSourceWarehouseId) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "目标仓库不能与出库仓库相同")) }
        return
      }
    } else if (formState.directSalespersonId.isBlank()) {
      _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "请选择销售人员")) }
      return
    }

    for (line in activeLines) {
      val targetQuantity = line.targetQuantity.toIntOrNull()
      if (line.barcodes.isEmpty()) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "存在未扫码的货品行，不能提交")) }
        return
      }
      if (line.barcodes.any { line.reviews[it]?.isValid == false }) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "存在异常条码，不能提交")) }
        return
      }
      if (line.barcodes.any { line.reviews[it] == null }) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Info, "仍有条码正在校验，请稍后提交")) }
        return
      }
      if (line.targetQuantity.isNotBlank() && targetQuantity == null) {
        _uiState.update { it.copy(message = StatusMessage(MessageTone.Error, "目标数量必须为正整数")) }
        return
      }
      if (targetQuantity != null && line.barcodes.size != targetQuantity) {
        _uiState.update {
          it.copy(message = StatusMessage(MessageTone.Error, "目标 ${targetQuantity} 件，当前已扫 ${line.barcodes.size} 件，未扫满不能提交"))
        }
        return
      }

      val availableQuantity = availableStockQuantity(masterData, sourceWarehouse.id, line.goodsId)
      if (line.barcodes.size > availableQuantity) {
        val goodsName = goodsDisplayName(masterData, line.goodsId)
        _uiState.update {
          it.copy(
            message = StatusMessage(
              MessageTone.Error,
              "$goodsName 可用库存 $availableQuantity 件，当前出库 ${line.barcodes.size} 件，库存不足"
            )
          )
        }
        return
      }
    }

    _uiState.update {
      it.copy(
        submitting = it.submitting + (PdaOperation.DirectOutbound to true),
        message = null
      )
    }

    val serverUrl = _uiState.value.loginForm.serverUrl
    val requestId = UUID.randomUUID().toString()
    val request = OutboundSubmitRequest(
      type = "direct",
      sourceWarehouseId = formState.directSourceWarehouseId,
      targetWarehouseId = if (formState.directDestinationType == "warehouse") formState.directTargetWarehouseId else null,
      targetLocationId = if (formState.directDestinationType == "warehouse") formState.directTargetLocationId else null,
      salespersonId = if (formState.directDestinationType == "sales") formState.directSalespersonId else null,
      lines = activeLines.map { line ->
        OutboundLineSubmitRequest(
          goodsId = line.goodsId,
          targetQuantity = line.targetQuantity.toIntOrNull(),
          barcodes = line.barcodes
        )
      },
      clientRequestId = requestId
    )
    runCatching {
      val pending = repository.savePendingSubmission(
        WarehouseRepository.PENDING_OUTBOUND,
        requestId,
        request,
        PdaOperation.DirectOutbound.title
      )
      _uiState.update { it.copy(pendingSubmission = pending) }
      repository.submitOutbound(serverUrl, request)
    }.onSuccess { result ->
      repository.clearPendingSubmission()
      val itemCount = result.quantity ?: result.items.size
      _uiState.update { state ->
        state.copy(
          route = AppRoute.Main(MainTab.Home),
          submitting = state.submitting + (PdaOperation.DirectOutbound to false),
          outboundLines = emptyList(),
          selectedOutboundGoodsId = "",
          barcodeInputs = state.barcodeInputs + (PdaOperation.DirectOutbound to ""),
          outboundTodayCount = state.outboundTodayCount + itemCount,
          lastSubmitSummary = "${PdaOperation.DirectOutbound.title}：单号 ${result.orderId}，共 $itemCount 件",
          recentActivities = pushRecentActivity(
            state.recentActivities,
            RecentActivity(
              id = UUID.randomUUID().toString(),
              title = PdaOperation.DirectOutbound.title,
              subtitle = "单号 ${result.orderId}，共 $itemCount 件",
              timeLabel = nowLabel(),
              tone = MessageTone.Success
            )
          ),
          message = StatusMessage(MessageTone.Success, "${PdaOperation.DirectOutbound.title}提交成功，单号 ${result.orderId}"),
          pendingSubmission = null
        )
      }
      loadMasterData()
    }.onFailure { error ->
      if (isSessionExpired(error)) {
        expireSession()
        return@onFailure
      }
      val retainPending = shouldRetainPendingSubmission(error)
      if (!retainPending) repository.clearPendingSubmission()
      _uiState.update {
        it.copy(
          submitting = it.submitting + (PdaOperation.DirectOutbound to false),
          pendingSubmission = if (retainPending) repository.getPendingSubmission() else null,
          message = StatusMessage(
            if (retainPending) MessageTone.Info else MessageTone.Error,
            if (retainPending) "提交结果暂未确认，已安全保留本次出库，请稍后重试" else (error.message ?: "提交失败")
          )
        )
      }
      if (!retainPending) loadMasterData()
    }
  }

  private fun availableStockQuantity(
    masterData: WarehouseState,
    warehouseId: String,
    goodsId: String
  ): Int {
    return masterData.warehouseStocks.orEmpty().firstOrNull {
      it.warehouseId == warehouseId && it.goodsId == goodsId
    }?.quantity ?: 0
  }

  private fun goodsDisplayName(masterData: WarehouseState, goodsId: String): String {
    val goods = masterData.goods.firstOrNull { it.id == goodsId }
    return when {
      goods == null -> "该货品"
      goods.code.isBlank() -> goods.name
      else -> "${goods.name}（${goods.code}）"
    }
  }

  fun refreshMasterData() {
    val state = _uiState.value
    if (state.currentUser == null || state.loadingMasterData) return
    loadMasterData(showSuccess = true)
  }

  fun loadMasterData(showSuccess: Boolean = false) {
    val serverUrl = _uiState.value.loginForm.serverUrl
    viewModelScope.launch {
      _uiState.update { it.copy(loadingMasterData = true) }
      runCatching {
        repository.getMasterData(serverUrl)
      }.onSuccess { masterData ->
        _uiState.update { state ->
          val workWarehouseId = existingWorkWarehouseId(masterData, state.selectedWorkWarehouseId)
          state.copy(
            masterData = masterData,
            loadingMasterData = false,
            selectedWorkWarehouseId = workWarehouseId,
            formState = ensureDefaults(
              masterData,
              state.formState.withWorkWarehouseDefaults(masterData, workWarehouseId)
            ).normalized(),
            message = if (showSuccess) {
              StatusMessage(MessageTone.Success, "数据已刷新")
            } else {
              state.message
            }
          )
        }
      }.onFailure { error ->
        if (isSessionExpired(error)) {
          expireSession()
          return@onFailure
        }
        _uiState.update {
          it.copy(
            loadingMasterData = false,
            message = StatusMessage(MessageTone.Error, error.message ?: "读取基础数据失败")
          )
        }
      }
    }
  }

  private suspend fun submitByOperation(
    operation: PdaOperation,
    formState: OperationFormState,
    barcodes: List<String>,
    clientRequestId: String
  ): SubmissionReceipt {
    val serverUrl = _uiState.value.loginForm.serverUrl
    return when (operation) {
      PdaOperation.FactoryInbound -> {
        val request = InboundSubmitRequest(
          source = "factory",
          warehouseId = formState.factoryWarehouseId,
          locationId = formState.factoryLocationId,
          goodsId = formState.factoryGoodsId,
          quantity = normalizePositiveInt(formState.factoryQuantity),
          barcodes = emptyList(),
          clientRequestId = clientRequestId
        )
        val pending = repository.savePendingSubmission(WarehouseRepository.PENDING_INBOUND, clientRequestId, request, operation.title)
        _uiState.update { it.copy(pendingSubmission = pending) }
        repository.submitInbound(serverUrl, request).let { result ->
          SubmissionReceipt(result.orderId, result.quantity ?: result.items.size)
        }
      }

      PdaOperation.TerminalInbound -> {
        val request = InboundSubmitRequest(
          source = "terminal_return",
          warehouseId = formState.terminalWarehouseId,
          locationId = formState.terminalLocationId,
          goodsId = formState.terminalGoodsId,
          terminalStoreId = formState.terminalStoreId,
          productionDate = formState.terminalProductionDate,
          barcodes = barcodes,
          clientRequestId = clientRequestId
        )
        val pending = repository.savePendingSubmission(WarehouseRepository.PENDING_INBOUND, clientRequestId, request, operation.title)
        _uiState.update { it.copy(pendingSubmission = pending) }
        repository.submitInbound(serverUrl, request).let { result ->
          SubmissionReceipt(result.orderId, result.quantity ?: result.items.size)
        }
      }

      PdaOperation.DirectOutbound -> {
        val request = TrackingOutboundSubmitRequest(
          sourceWarehouseId = formState.directSourceWarehouseId,
          destinationType = if (formState.directDestinationType == "warehouse") "warehouse" else "salesperson",
          salespersonId = formState.directSalespersonId.takeIf { formState.directDestinationType != "warehouse" },
          targetWarehouseId = formState.directTargetWarehouseId.takeIf { formState.directDestinationType == "warehouse" },
          barcodes = barcodes,
          clientRequestId = clientRequestId
        )
        val pending = repository.savePendingSubmission(
          WarehouseRepository.PENDING_TRACKING_OUTBOUND,
          clientRequestId,
          request,
          operation.title
        )
        _uiState.update { it.copy(pendingSubmission = pending) }
        repository.submitTrackingOutbound(serverUrl, request).let { result ->
          SubmissionReceipt(result.orderId, result.quantity)
        }
      }

      PdaOperation.SalesReturn -> {
        val request = TrackingReturnSubmitRequest(
          returnWarehouseId = formState.returnWarehouseId,
          barcodes = barcodes,
          clientRequestId = clientRequestId
        )
        val pending = repository.savePendingSubmission(
          WarehouseRepository.PENDING_TRACKING_RETURN,
          clientRequestId,
          request,
          operation.title
        )
        _uiState.update { it.copy(pendingSubmission = pending) }
        repository.submitTrackingReturn(serverUrl, request).let { result ->
          SubmissionReceipt(result.orderId, result.quantity)
        }
      }
    }
  }

  private fun mergeValidationResults(
    operation: PdaOperation,
    validationToken: String,
    results: List<BarcodeValidationResult>
  ) {
    val mapped = results.associate {
      it.barcode to ReviewState(
        label = it.label,
        detail = it.detail,
        isValid = it.ok
      )
    }
    _uiState.update {
      val currentTokens = it.barcodeValidationTokens[operation].orEmpty()
      val currentBarcodes = it.barcodeLists[operation].orEmpty().toSet()
      val expectedBarcodes = currentTokens.filterValues { token -> token == validationToken }.keys.intersect(currentBarcodes)
      val missing = (expectedBarcodes - mapped.keys).associateWith {
        ReviewState(
          label = "校验失败",
          detail = "服务器未返回该条码的校验结果，请删除后重新扫描",
          isValid = false
        )
      }
      val applicable = (mapped + missing).filterKeys { barcode ->
        barcode in currentBarcodes && currentTokens[barcode] == validationToken
      }
      val nextReviews = it.barcodeReviews[operation].orEmpty() + applicable
      val validCount = currentBarcodes.count { barcode -> nextReviews[barcode]?.isValid == true }
      val hasError = applicable.values.any { review -> !review.isValid }
      it.copy(
        barcodeReviews = it.barcodeReviews + (operation to nextReviews),
        scanSoundCue = if (applicable.isEmpty()) {
          it.scanSoundCue
        } else {
          ScanSoundCue(
            tone = if (hasError) ScanSoundTone.Error else ScanSoundTone.Success,
            announcement = scanAnnouncement(validCount, hasError)
          )
        }
      )
    }
  }

  private fun mergeOutboundValidationResults(
    goodsId: String,
    validationToken: String,
    results: List<BarcodeValidationResult>
  ) {
    val mapped = results.associate {
      it.barcode to ReviewState(
        label = it.label,
        detail = it.detail,
        isValid = it.ok
      )
    }
    _uiState.update { state ->
      state.copy(
        outboundLines = state.outboundLines.map { line ->
          if (line.goodsId == goodsId) {
            val currentBarcodes = line.barcodes.toSet()
            val applicable = mapped.filterKeys { barcode ->
              barcode in currentBarcodes && line.validationTokens[barcode] == validationToken
            }
            line.copy(reviews = line.reviews + applicable)
          } else line
        },
        scanSoundCue = ScanSoundCue(
          tone = if (results.all { result -> result.ok }) ScanSoundTone.Success else ScanSoundTone.Error
        )
      )
    }
  }

  private fun buildValidationRequest(
    operation: PdaOperation,
    formState: OperationFormState,
    barcodes: List<String>
  ): BarcodeValidationRequest {
    return when (operation) {
      PdaOperation.FactoryInbound -> BarcodeValidationRequest(
        mode = "factory_inbound",
        barcodes = barcodes
      )

      PdaOperation.TerminalInbound -> BarcodeValidationRequest(
        mode = "terminal_return_inbound",
        barcodes = barcodes,
        goodsId = formState.terminalGoodsId
      )

      PdaOperation.DirectOutbound -> BarcodeValidationRequest(
        mode = "warehouse_outbound",
        barcodes = barcodes,
        goodsId = formState.directGoodsId,
        warehouseId = formState.directSourceWarehouseId
      )

      PdaOperation.SalesReturn -> BarcodeValidationRequest(
        mode = "sales_return",
        barcodes = barcodes
      )
    }
  }

  private fun buildTrackingValidationRequest(
    operation: PdaOperation,
    formState: OperationFormState,
    barcodes: List<String>
  ): TrackingValidationRequest {
    return when (operation) {
      PdaOperation.DirectOutbound -> TrackingValidationRequest(
        mode = "outbound",
        barcodes = barcodes,
        sourceWarehouseId = formState.directSourceWarehouseId
      )
      PdaOperation.SalesReturn -> TrackingValidationRequest(
        mode = "return",
        barcodes = barcodes,
        returnWarehouseId = formState.returnWarehouseId
      )
      else -> error("该业务不使用流向追踪校验")
    }
  }

  fun canOperate(): Boolean {
    val roles = _uiState.value.currentUser?.roles.orEmpty().map { it.code }
    return roles.contains("SUPER_ADMIN") || roles.contains("WAREHOUSE_ADMIN")
  }

  companion object {
    const val MAX_BARCODES_PER_SUBMISSION = SubmissionPolicy.MAX_BARCODES

    fun factory(repository: WarehouseRepository): ViewModelProvider.Factory {
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return MainViewModel(repository) as T
        }
      }
    }

    private fun parseBarcodes(input: String): List<String> {
      return input
        .split(Regex("[\\s,，;；]+"))
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    }

    private fun defaultTabFor(@Suppress("UNUSED_PARAMETER") operation: PdaOperation): MainTab {
      return MainTab.Home
    }

    private fun pushRecentActivity(
      current: List<RecentActivity>,
      next: RecentActivity
    ): List<RecentActivity> {
      return listOf(next) + current.take(7)
    }

    private fun describeTrackingResult(result: TrackingDetailResult): String {
      val ownerLabel = when (result.item.currentOwnerType) {
        "warehouse" -> "仓库"
        "terminal_store" -> result.item.terminalStoreName ?: "终端店铺"
        else -> "销售人员"
      }
      val receiptLabel = when (result.item.receiptStatus) {
        "signed" -> "已签收"
        "exception" -> "签收异常"
        else -> "待签收"
      }
      return "${result.item.barcode}，当前归属 $ownerLabel，$receiptLabel"
    }

    private fun normalizePositiveInt(value: String): Int? {
      return value.trim().toIntOrNull()?.takeIf { it > 0 }
    }

    private fun existingWorkWarehouseId(masterData: WarehouseState, currentId: String): String {
      val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
      return warehouses.firstOrNull { it.id == currentId }?.id ?: warehouses.firstOrNull()?.id.orEmpty()
    }

    private fun OperationFormState.withWorkWarehouseDefaults(
      masterData: WarehouseState,
      warehouseId: String
    ): OperationFormState {
      if (warehouseId.isBlank()) return this

      val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
      val locations = masterData.locations.filter { it.status == "enabled" }

      fun firstLocation(targetWarehouseId: String): String {
        return locations.firstOrNull { it.warehouseId == targetWarehouseId }?.id.orEmpty()
      }

      val directTargetWarehouseId = warehouses
        .firstOrNull { it.id == this.directTargetWarehouseId && it.id != warehouseId }
        ?.id
        ?: warehouses.firstOrNull { it.id != warehouseId }?.id.orEmpty()

      return copy(
        factoryWarehouseId = warehouseId,
        factoryLocationId = firstLocation(warehouseId),
        terminalWarehouseId = warehouseId,
        terminalLocationId = firstLocation(warehouseId),
        directSourceWarehouseId = warehouseId,
        directTargetWarehouseId = directTargetWarehouseId,
        directTargetLocationId = firstLocation(directTargetWarehouseId),
        returnWarehouseId = warehouseId,
        returnLocationId = firstLocation(warehouseId)
      )
    }

    private fun ensureDefaults(
      masterData: WarehouseState,
      current: OperationFormState
    ): OperationFormState {
      val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
      val goods = masterData.goods.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
      val terminalStores = masterData.terminalStores.filter { it.status == "enabled" }
      val salespeople = masterData.salespeople.filter { it.status == "enabled" }
      val locations = masterData.locations.filter { it.status == "enabled" }

      fun firstLocation(warehouseId: String): String {
        return locations.firstOrNull { it.warehouseId == warehouseId }?.id.orEmpty()
      }

      fun existingWarehouse(id: String): String = warehouses.firstOrNull { it.id == id }?.id ?: warehouses.firstOrNull()?.id.orEmpty()
      fun existingGoods(id: String): String = goods.firstOrNull { it.id == id }?.id ?: goods.firstOrNull()?.id.orEmpty()
      fun existingStore(id: String): String = terminalStores.firstOrNull { it.id == id }?.id ?: terminalStores.firstOrNull()?.id.orEmpty()
      fun existingSales(id: String): String = salespeople.firstOrNull { it.id == id }?.id ?: salespeople.firstOrNull()?.id.orEmpty()
      fun existingLocation(warehouseId: String, locationId: String): String {
        return locations.firstOrNull { it.warehouseId == warehouseId && it.id == locationId }?.id ?: firstLocation(warehouseId)
      }

      val factoryWarehouseId = existingWarehouse(current.factoryWarehouseId)
      val terminalWarehouseId = existingWarehouse(current.terminalWarehouseId.ifBlank { factoryWarehouseId })
      val directSourceWarehouseId = existingWarehouse(current.directSourceWarehouseId.ifBlank { factoryWarehouseId })
      val directTargetWarehouseId = warehouses.firstOrNull { it.id == current.directTargetWarehouseId && it.id != directSourceWarehouseId }?.id
        ?: warehouses.firstOrNull { it.id != directSourceWarehouseId }?.id.orEmpty()
      val returnWarehouseId = existingWarehouse(current.returnWarehouseId.ifBlank { factoryWarehouseId })

      return current.copy(
        factoryWarehouseId = factoryWarehouseId,
        factoryLocationId = existingLocation(factoryWarehouseId, current.factoryLocationId),
        factoryGoodsId = existingGoods(current.factoryGoodsId),
        terminalWarehouseId = terminalWarehouseId,
        terminalLocationId = existingLocation(terminalWarehouseId, current.terminalLocationId),
        terminalGoodsId = existingGoods(current.terminalGoodsId),
        terminalStoreId = existingStore(current.terminalStoreId),
        directSourceWarehouseId = directSourceWarehouseId,
        directGoodsId = existingGoods(current.directGoodsId),
        directDestinationType = if (current.directDestinationType == "warehouse") "warehouse" else "sales",
        directTargetWarehouseId = directTargetWarehouseId,
        directTargetLocationId = existingLocation(directTargetWarehouseId, current.directTargetLocationId),
        directSalespersonId = existingSales(current.directSalespersonId),
        returnWarehouseId = returnWarehouseId,
        returnLocationId = existingLocation(returnWarehouseId, current.returnLocationId)
      )
    }
  }

  private fun nowLabel(): String = LocalTime.now().format(timeFormatter)
}

private fun OperationFormState.normalized(): OperationFormState {
  return copy(
    factoryQuantity = factoryQuantity.trim(),
    terminalProductionDate = terminalProductionDate.trim()
  )
}

private fun OperationFormState.outboundContext(): List<String> {
  return listOf(
    directSourceWarehouseId,
    directDestinationType,
    directTargetWarehouseId.takeIf { directDestinationType == "warehouse" }.orEmpty(),
    directSalespersonId.takeIf { directDestinationType != "warehouse" }.orEmpty()
  )
}

internal fun trackingContextError(operation: PdaOperation, form: OperationFormState): String? {
  return when (operation) {
    PdaOperation.DirectOutbound -> when {
      form.directSourceWarehouseId.isBlank() -> "请选择出库仓库"
      form.directDestinationType == "warehouse" && form.directTargetWarehouseId.isBlank() -> "请选择目标仓库"
      form.directDestinationType == "warehouse" && form.directTargetWarehouseId == form.directSourceWarehouseId -> "目标仓库不能与出库仓库相同"
      form.directDestinationType != "warehouse" && form.directSalespersonId.isBlank() -> "请选择销售人员"
      else -> null
    }
    PdaOperation.SalesReturn -> if (form.returnWarehouseId.isBlank()) "请选择回库仓库" else null
    else -> null
  }
}

internal fun scanAnnouncement(validCount: Int, hasError: Boolean): String {
  return if (hasError) {
    "扫码错误，当前有效 $validCount 件"
  } else {
    "已扫 $validCount 件"
  }
}

private fun AppUiState.validBarcodeCount(operation: PdaOperation): Int {
  val reviews = barcodeReviews[operation].orEmpty()
  return barcodeLists[operation].orEmpty().count { barcode -> reviews[barcode]?.isValid == true }
}

package com.warehouse.pda

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warehouse.pda.data.InventoryDetailResult
import com.warehouse.pda.data.StorageLocation
import com.warehouse.pda.data.WarehouseRecord
import com.warehouse.pda.data.WarehouseState
import com.warehouse.pda.ui.theme.PdaBackground
import com.warehouse.pda.ui.theme.PdaOnSurface
import com.warehouse.pda.ui.theme.PdaOnSurfaceVariant
import com.warehouse.pda.ui.theme.PdaOutlineVariant
import com.warehouse.pda.ui.theme.PdaPrimaryStrong
import com.warehouse.pda.ui.theme.PdaSurface
import com.warehouse.pda.ui.theme.PdaSurfaceVariant
import com.warehouse.pda.ui.theme.WarehousePdaTheme
import java.time.LocalDate

private val BluePrimary = Color(0xFF0052CC)
private val BlueSoft = Color(0xFFE8EDFF)
private val OrangeAccent = Color(0xFFFF7A1A)
private val MintAccent = Color(0xFF20B46A)
private val AppSurface = Color(0xFFFAF8FF)
private val CardBorder = Color(0xFFC3C6D6)
private val MutedText = Color(0xFF434654)
private val DangerRed = Color(0xFFBA1A1A)
private val SuccessGreen = Color(0xFF16B37E)

@Composable
fun WarehousePdaRoot(viewModel: MainViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90) }

  DisposableEffect(Unit) {
    onDispose { toneGenerator.release() }
  }

  LaunchedEffect(uiState.message) {
    val text = uiState.message?.text ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(text)
    viewModel.clearMessage()
  }

  LaunchedEffect(uiState.scanSoundCue?.id) {
    when (uiState.scanSoundCue?.tone) {
      ScanSoundTone.Success -> toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
      ScanSoundTone.Error -> toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180)
      null -> Unit
    }
  }

  WarehousePdaTheme {
    Scaffold(
      containerColor = AppSurface,
      contentWindowInsets = WindowInsets(0),
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        val route = uiState.route
        if (route is AppRoute.Main) {
          MainBottomBar(currentTab = route.tab, onSelect = viewModel::openTab)
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (val route = uiState.route) {
          AppRoute.Login -> LoginScreen(uiState, viewModel)
          is AppRoute.Main -> MainSectionScreen(uiState, route.tab, viewModel)
          is AppRoute.OperationConfig -> OperationConfigScreen(uiState, route.operation, viewModel)
          is AppRoute.OperationScan -> OperationScanScreen(uiState, route.operation, viewModel)
        }
      }
    }
  }
}

@Composable
private fun LoginScreen(uiState: AppUiState, viewModel: MainViewModel) {
  var showServerDialog by remember { mutableStateOf(false) }
  var passwordVisible by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 28.dp, vertical = 18.dp),
    contentAlignment = Alignment.Center
  ) {
    IconButton(
      onClick = { showServerDialog = true },
      modifier = Modifier.align(Alignment.TopEnd)
    ) {
      Icon(
        imageVector = Icons.Outlined.Settings,
        contentDescription = "修改服务器",
        tint = MutedText
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(130.dp)
          .background(BluePrimary, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "W",
          color = Color.White,
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Black
        )
      }

      Spacer(modifier = Modifier.height(28.dp))
      Text(
        "仓库管理系统",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        color = Color(0xFF172033)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        "欢迎登录仓库管理系统",
        style = MaterialTheme.typography.headlineSmall,
        color = MutedText
      )
      Spacer(modifier = Modifier.height(44.dp))

      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        OutlinedTextField(
          value = uiState.loginForm.username,
          onValueChange = viewModel::updateUsername,
          placeholder = { Text("用户名", color = MutedText) },
          singleLine = true,
          leadingIcon = {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MutedText)
          },
          shape = RoundedCornerShape(4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
        )
        OutlinedTextField(
          value = uiState.loginForm.password,
          onValueChange = viewModel::updatePassword,
          placeholder = { Text("密码", color = MutedText) },
          singleLine = true,
          leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MutedText)
          },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                tint = MutedText
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          shape = RoundedCornerShape(4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) { viewModel.updateRememberCredentials(!uiState.loginForm.rememberCredentials) },
            verticalAlignment = Alignment.CenterVertically
          ) {
            Checkbox(
              checked = uiState.loginForm.rememberCredentials,
              onCheckedChange = viewModel::updateRememberCredentials
            )
            Text("记住账号", color = MutedText, style = MaterialTheme.typography.bodyLarge)
          }
          Spacer(modifier = Modifier.weight(1f))
          TextButton(onClick = {}) {
            Text("忘记密码?", color = BluePrimary, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = viewModel::login,
          enabled = !uiState.loginPending &&
            uiState.loginForm.serverUrl.isNotBlank() &&
            uiState.loginForm.username.isNotBlank() &&
            uiState.loginForm.password.isNotBlank(),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
          shape = RoundedCornerShape(4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
        ) {
          if (uiState.loginPending) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
          } else {
            Text("登录", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    }

    if (showServerDialog) {
      AlertDialog(
        onDismissRequest = { showServerDialog = false },
        title = {
          Text("服务器设置", fontWeight = FontWeight.Bold)
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("修改后将用于后续登录和接口请求。", color = MutedText)
            OutlinedTextField(
              value = uiState.loginForm.serverUrl,
              onValueChange = viewModel::updateServerUrl,
              label = { Text("服务器地址") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = { showServerDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
          ) {
            Text("完成")
          }
        },
        dismissButton = {
          TextButton(onClick = { showServerDialog = false }) {
            Text("取消")
          }
        }
      )
    }
  }
}

@Composable
private fun MainSectionScreen(uiState: AppUiState, tab: MainTab, viewModel: MainViewModel) {
  when (tab) {
    MainTab.Home -> DashboardScreen(uiState, viewModel)
    MainTab.Query -> QueryScreen(uiState, viewModel)
    MainTab.Inbound -> OperationHubScreen(
      title = "入库作业",
      subtitle = "选择本次要执行的入库任务",
      operations = PdaOperation.entries.filter { it.group == OperationGroup.Inbound },
      accentColor = BluePrimary,
      viewModel = viewModel
    )
    MainTab.Outbound -> OperationHubScreen(
      title = "出库作业",
      subtitle = "创建出库单并进入扫码作业",
      operations = listOf(PdaOperation.DirectOutbound),
      accentColor = OrangeAccent,
      viewModel = viewModel
    )
    MainTab.Profile -> ProfileScreen(uiState, viewModel)
  }
}

@Composable
private fun DashboardScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val currentUser = uiState.currentUser ?: return
  val warehouses = uiState.masterData
    ?.warehouses
    ?.filter { it.status == "enabled" }
    ?.sortedBy { it.sortOrder }
    .orEmpty()
  val selectedWarehouseId = uiState.selectedWorkWarehouseId.ifBlank { warehouses.firstOrNull()?.id.orEmpty() }
  val warehouseLabel = warehouses
    .firstOrNull { it.id == selectedWarehouseId }
    ?.name
    ?: "选择仓库"
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(PdaBackground)
      .statusBarsPadding()
      .padding(horizontal = 20.dp)
  ) {
    PdaTopBrandBar()
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        HomeGreetingHeader(
          displayName = currentUser.displayName,
          warehouseLabel = warehouseLabel,
          warehouses = warehouses,
          selectedWarehouseId = selectedWarehouseId,
          onWarehouseSelect = viewModel::selectWorkWarehouse
        )
      }
      item {
        PdaHomeTaskCard(
          title = "扫码查询",
          subtitle = "扫描条码快速定位库存归属",
          icon = Icons.Outlined.QrCodeScanner,
          accentColor = BluePrimary,
          tintColor = Color(0xFFF0F4FF),
          onClick = { viewModel.openTab(MainTab.Query) }
        )
      }
      item {
        PdaHomeTaskCard(
          title = "入库作业",
          subtitle = "执行采购收货、退货入库",
          icon = Icons.Outlined.Login,
          accentColor = BluePrimary,
          tintColor = Color(0xFFF0F4FF),
          onClick = { viewModel.openTab(MainTab.Inbound) }
        )
      }
      item {
        PdaHomeTaskCard(
          title = "出库作业",
          subtitle = "订单拣货、打包发货与调拨",
          icon = Icons.Outlined.Logout,
          accentColor = OrangeAccent,
          tintColor = Color(0xFFFFF7F1),
          onClick = { viewModel.openTab(MainTab.Outbound) }
        )
      }
      uiState.lastSubmitSummary?.let { summary ->
        item {
          StatusBanner(summary, MessageTone.Success)
        }
      }
      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }
}

@Composable
private fun QueryScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val masterData = uiState.masterData
  val result = uiState.queryForm.result
  QueryPageShell(title = "扫码查询", subtitle = "扫描条码确认库存归属") {
    QueryCommandCard(
      value = uiState.queryForm.barcodeInput,
      onValueChange = viewModel::updateQueryInput,
      loading = uiState.queryForm.loading,
      onSearch = { viewModel.queryBarcode(it) },
      onClear = viewModel::clearQueryResult
    )
    if (uiState.queryForm.loading) {
      CenterLoadingCard("正在读取条码详情")
    } else if (result != null && masterData != null) {
      InventoryResultCard(result, masterData)
    } else {
      QueryEmptyStateCard()
    }
  }
}

@Composable
private fun OperationHubScreen(
  title: String,
  subtitle: String,
  operations: List<PdaOperation>,
  accentColor: Color,
  viewModel: MainViewModel
) {
  QueryPageShell(title = title, subtitle = subtitle) {
    operations.forEach { operation ->
      PdaHomeTaskCard(
        title = operation.title,
        subtitle = operation.description,
        icon = operationIcon(operation),
        accentColor = accentColor,
        tintColor = if (accentColor == OrangeAccent) Color(0xFFFFF4EC) else Color(0xFFF0F4FF),
        onClick = { viewModel.openOperation(operation) }
      )
    }
  }
}

@Composable
private fun ProfileScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val currentUser = uiState.currentUser ?: return
  val context = LocalContext.current
  val updateState = uiState.appUpdateState
  val release = updateState.release
  QueryPageShell(title = "我的", subtitle = "账号与版本") {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .background(BlueSoft, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = BluePrimary)
          }
          Column {
            Text(currentUser.displayName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text(currentUser.roles.joinToString("、") { it.name }, color = MutedText)
          }
        }
        StatusChip("终端在线", Color(0xFFE8FBF3), MintAccent)
        Text("用于扫码作业与扫码查询。", color = MutedText)
      }
    }
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("版本更新", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("当前版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = MutedText)
        when {
          updateState.checking -> Text("正在检查更新...", color = BluePrimary, fontWeight = FontWeight.SemiBold)
          release == null -> Text("暂未检查版本信息。", color = MutedText)
          updateState.hasUpdate -> {
            Text("发现新版本 ${release.versionName} (${release.versionCode})", color = SuccessGreen, fontWeight = FontWeight.Bold)
            if (release.notes.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                release.notes.forEach { note ->
                  Text("• $note", color = Color(0xFF172033))
                }
              }
            }
            Text(
              if (release.apkUrl.isNullOrBlank()) "服务器已发布版本信息，但还没有配置安装包下载地址。" else "点击下方按钮可下载最新安装包。",
              color = MutedText
            )
          }
          else -> Text("当前已是最新版本。", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = { viewModel.checkForUpdates() },
            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("检查更新", color = Color.White, fontWeight = FontWeight.Bold)
          }
          if (updateState.hasUpdate && !release?.apkUrl.isNullOrBlank()) {
            OutlinedButton(
              onClick = { release?.apkUrl?.let { openExternalUrl(context, it) } },
              shape = RoundedCornerShape(14.dp)
            ) {
              Text("下载新版本", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
    Button(
      onClick = viewModel::logout,
      modifier = Modifier.fillMaxWidth().height(58.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.size(8.dp))
      Text("退出登录", color = Color.White, fontWeight = FontWeight.Bold)
    }
  }
}

private fun openExternalUrl(context: Context, url: String) {
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  }
  context.startActivity(intent)
}

@Composable
private fun OperationConfigScreen(uiState: AppUiState, operation: PdaOperation, viewModel: MainViewModel) {
  val masterData = uiState.masterData ?: return
  val form = uiState.formState
  if (operation == PdaOperation.DirectOutbound) {
    DirectOutboundOrderScreen(uiState = uiState, viewModel = viewModel)
    return
  }

  OperationPageShell(
    title = "仓库管理系统",
    onBack = { viewModel.goBackFromOperation(operation, fromScan = false) },
    trailing = {}
  ) {
    Text(operation.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
    if (operation != PdaOperation.DirectOutbound) {
      Text(
        when (operation) {
          PdaOperation.FactoryInbound -> "请选择作业参数并填写入库数量。"
          else -> "请选择作业参数并确认目标仓库，准备扫码。"
        },
        color = MutedText
      )
    }
    OperationFields(
      operation = operation,
      form = form,
      masterData = masterData,
      operatorName = uiState.currentUser?.displayName.orEmpty(),
      onUpdate = viewModel::updateForm
    )
    Button(
      onClick = { if (operation == PdaOperation.FactoryInbound) viewModel.submit(operation) else viewModel.startScanning(operation) },
      enabled = uiState.submitting[operation] != true,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      shape = RoundedCornerShape(16.dp)
    ) {
      if (uiState.submitting[operation] == true) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
      } else {
        Icon(
        when (operation) {
          PdaOperation.FactoryInbound -> Icons.Outlined.CheckCircle
          PdaOperation.DirectOutbound -> Icons.Outlined.Inventory2
          else -> Icons.Outlined.QrCodeScanner
        },
        contentDescription = null,
        tint = Color.White
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(
        when (operation) {
          PdaOperation.FactoryInbound -> "确认入库"
          PdaOperation.DirectOutbound -> "填写货品明细"
          else -> "开始扫码"
        },
        color = Color.White,
        fontWeight = FontWeight.Black
      )
      }
    }
  }
}

@Composable
private fun DirectOutboundOrderScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val masterData = uiState.masterData ?: return
  val form = uiState.formState
  val goods = masterData.goods.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val salespeople = masterData.salespeople.filter { it.status == "enabled" }
  val locations = masterData.locations.filter { it.status == "enabled" }
  val totalScanned = uiState.outboundLines.sumOf { it.barcodes.size }
  val invalidCount = uiState.outboundLines.sumOf { line -> line.barcodes.count { line.reviews[it]?.isValid == false } }
  val totalSku = uiState.outboundLines.size
  val completedSku = uiState.outboundLines.count { outboundLineComplete(it) }
  val targetMismatch = uiState.outboundLines.any { line ->
    val target = line.targetQuantity.toIntOrNull()
    target != null && line.barcodes.size != target
  }

  Scaffold(
    containerColor = AppSurface,
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
        .navigationBarsPadding()
        .background(Color.White)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        OutlinedButton(
          onClick = viewModel::cancelDirectOutbound,
          enabled = uiState.submitting[PdaOperation.DirectOutbound] != true,
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
          border = BorderStroke(2.dp, BluePrimary),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("取消出库", color = BluePrimary, fontWeight = FontWeight.Black)
        }
        Button(
          onClick = { viewModel.submit(PdaOperation.DirectOutbound) },
          enabled = totalScanned > 0 &&
            invalidCount == 0 &&
            !targetMismatch &&
            uiState.submitting[PdaOperation.DirectOutbound] != true &&
            viewModel.canOperate(),
          modifier = Modifier
            .weight(2f)
            .fillMaxHeight(),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
          shape = RoundedCornerShape(14.dp)
        ) {
          if (uiState.submitting[PdaOperation.DirectOutbound] == true) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
          } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
              Text("确认出库 $completedSku/$totalSku", color = Color.White, fontWeight = FontWeight.Black, maxLines = 1)
              Text("已扫 $totalScanned · 异常 $invalidCount", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
            }
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .background(AppSurface)
        .statusBarsPadding()
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        OutboundTopBar(
          title = "出库单",
          subtitle = outboundContextLabel(form, masterData),
          onBack = { viewModel.goBackFromOperation(PdaOperation.DirectOutbound, fromScan = false) },
          showBack = true,
          showMore = false
        )
      }
      item {
        OutboundSectionHeader(icon = Icons.Outlined.Info, title = "基础信息")
      }
      item {
        OutboundBaseInfoCard(
          form = form,
          warehouses = warehouses,
          salespeople = salespeople,
          locations = locations,
          onUpdate = viewModel::updateForm
        )
      }
      item {
        OutboundSectionHeader(icon = Icons.Outlined.Inventory2, title = "货品明细")
      }
      item {
        OutboundOrderGoodsCard(
          selectedGoodsId = form.directGoodsId,
          lines = uiState.outboundLines,
          goods = goods,
          availableQuantity = { goodsId -> availableQuantity(masterData, form.directSourceWarehouseId, goodsId) },
          onSelectGoods = { selected -> viewModel.updateForm { current -> current.copy(directGoodsId = selected) } },
          onAddGoods = { viewModel.addOrSelectOutboundLine() },
          onEnterScan = { goodsId ->
            viewModel.selectOutboundLine(goodsId)
            viewModel.startScanning(PdaOperation.DirectOutbound)
          },
          onRemove = viewModel::removeOutboundLine
        )
      }
      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun OperationScanScreen(uiState: AppUiState, operation: PdaOperation, viewModel: MainViewModel) {
  if (operation == PdaOperation.DirectOutbound) {
    DirectOutboundScanScreen(uiState = uiState, viewModel = viewModel)
    return
  }
  if (operation == PdaOperation.FactoryInbound) {
    LaunchedEffect(operation) {
      viewModel.goBackFromOperation(operation, fromScan = true)
    }
    CenterLoadingCard("厂家到货按数量入库，无需扫码")
    return
  }

  val masterData = uiState.masterData ?: return
  val form = uiState.formState
  val barcodeInput = uiState.barcodeInputs[operation].orEmpty()
  val barcodeList = uiState.barcodeLists[operation].orEmpty()
  val barcodeReviews = uiState.barcodeReviews[operation].orEmpty()
  val invalidCount = barcodeList.count { code -> barcodeReviews[code]?.isValid == false }

  Scaffold(
    containerColor = AppSurface,
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      Button(
        onClick = { viewModel.submit(operation) },
        enabled = barcodeList.isNotEmpty() &&
          invalidCount == 0 &&
          uiState.submitting[operation] != true &&
          viewModel.canOperate(),
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 14.dp)
          .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        shape = RoundedCornerShape(14.dp)
      ) {
        if (uiState.submitting[operation] == true) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
          Text("确认${submitLabel(operation)} (${barcodeList.size})", color = Color.White, fontWeight = FontWeight.Black)
        }
      }
    }
  ) { innerPadding ->
    OperationPageShell(
      title = "仓库管理系统",
      onBack = { viewModel.goBackFromOperation(operation, fromScan = true) },
      modifier = Modifier.padding(innerPadding),
      trailing = {
        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MutedText)
      }
    ) {
      ContextStrip(operation, form, masterData)
      ScanStatusCard(
        countLabel = "已扫",
        countValue = barcodeList.size.toString(),
        subtitle = "请直接使用扫码头录入，或在下方输入条码。"
      )
      ScanInputRow(
        value = barcodeInput,
        onValueChange = { viewModel.updateBarcodeInput(operation, it) },
        onAdd = { viewModel.addBarcodes(operation, it) },
        onClear = { viewModel.clearBarcodeInput(operation) }
      )
      ScanListCard(
        barcodeList = barcodeList,
        barcodeReviews = barcodeReviews,
        onRemove = { viewModel.removeBarcode(operation, it) }
      )
    }
  }
}

@Composable
private fun DirectOutboundScanScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val masterData = uiState.masterData ?: return
  val form = uiState.formState
  val goods = masterData.goods.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val selectedLine = uiState.outboundLines.firstOrNull { it.goodsId == uiState.selectedOutboundGoodsId }
  val barcodeInput = uiState.barcodeInputs[PdaOperation.DirectOutbound].orEmpty()

  Scaffold(
    containerColor = AppSurface,
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      Button(
        onClick = { viewModel.goBackFromOperation(PdaOperation.DirectOutbound, fromScan = true) },
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 14.dp)
          .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(if (selectedLine == null) "返回出库单" else "完成本货品", color = Color.White, fontWeight = FontWeight.Black)
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .background(AppSurface)
        .statusBarsPadding()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        OutboundTopBar(
          subtitle = outboundContextLabel(form, masterData),
          onBack = { viewModel.goBackFromOperation(PdaOperation.DirectOutbound, fromScan = true) }
        )
      }
      if (selectedLine == null) {
        item {
          EmptyStateCard(
            "未选择货品",
            "请返回出库单选择货品后再进入扫码。"
          )
        }
      } else {
        item {
          OutboundCurrentGoodsSummaryStrip(
            line = selectedLine,
            goods = goods,
            availableQuantity = availableQuantity(masterData, form.directSourceWarehouseId, selectedLine.goodsId),
            onTargetChange = viewModel::updateOutboundLineTarget
          )
        }
        item {
          OutboundScanInputCard(
            value = barcodeInput,
            onValueChange = { viewModel.updateBarcodeInput(PdaOperation.DirectOutbound, it) },
            onAdd = { viewModel.addBarcodes(PdaOperation.DirectOutbound, it) },
            onClear = { viewModel.clearBarcodeInput(PdaOperation.DirectOutbound) }
          )
        }
        item {
          OutboundScanRecordCard(
            line = selectedLine,
            onRemove = viewModel::removeOutboundBarcode
          )
        }
      }
      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun OutboundTopBar(
  title: String = "扫码出库",
  subtitle: String,
  onBack: () -> Unit,
  showBack: Boolean = true,
  showMore: Boolean = true
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 10.dp, bottom = 2.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (showBack) {
        IconButton(onClick = onBack) {
          Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color(0xFF172033))
        }
        }
      }
      Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF172033))
      Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        if (showMore) {
          Text("•••", color = Color(0xFF172033), fontWeight = FontWeight.Black)
        }
      }
    }
    Text(
      subtitle,
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      color = Color(0xFF687086),
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun OutboundBaseInfoCard(
  form: OperationFormState,
  warehouses: List<com.warehouse.pda.data.WarehouseRecord>,
  salespeople: List<com.warehouse.pda.data.Salesperson>,
  locations: List<StorageLocation>,
  onUpdate: ((OperationFormState) -> OperationFormState) -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      OutboundSelectField(
        label = "出库类型",
        selectedId = form.directDestinationType,
        options = listOf("sales" to "分销销售", "warehouse" to "调拨出库"),
        placeholder = "请选择出库类型"
      ) { selected ->
        onUpdate { current -> current.copy(directDestinationType = selected) }
      }
      if (form.directDestinationType == "warehouse") {
        OutboundSelectField(
          label = "出库去向",
          selectedId = form.directTargetWarehouseId,
          options = warehouses.filter { it.id != form.directSourceWarehouseId }.map { it.id to it.name },
          placeholder = "请选择出库去向"
        ) { selected ->
          onUpdate { current ->
            current.copy(
              directTargetWarehouseId = selected,
              directTargetLocationId = firstLocationId(selected, locations)
            )
          }
        }
      } else {
        OutboundSelectField(
          label = "出库去向",
          selectedId = form.directSalespersonId,
          options = salespeople.map { it.id to "${it.name} (${it.code})" },
          placeholder = "请选择出库去向"
        ) { selected ->
          onUpdate { current -> current.copy(directSalespersonId = selected) }
        }
      }
    }
  }
}

@Composable
private fun OutboundSelectField(
  label: String,
  selectedId: String,
  options: List<Pair<String, String>>,
  placeholder: String = "请选择",
  onSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: placeholder

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, color = Color(0xFF2B3140), fontSize = 18.sp, fontWeight = FontWeight.Black)
    Box {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .background(Color.White, RoundedCornerShape(16.dp))
          .border(1.dp, CardBorder.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
          .clickable(enabled = options.isNotEmpty()) { expanded = true }
          .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          selectedLabel,
          modifier = Modifier.weight(1f),
          color = if (options.any { it.first == selectedId }) Color(0xFF172033) else MutedText,
          fontWeight = FontWeight.Medium,
          fontSize = 18.sp,
          maxLines = 1
        )
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
          Icon(
            Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
            tint = MutedText,
            modifier = Modifier.size(28.dp)
          )
        }
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
          DropdownMenuItem(
            text = { Text(option.second) },
            onClick = {
              expanded = false
              onSelect(option.first)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun CompactInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 9.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, color = MutedText, style = MaterialTheme.typography.bodyLarge)
    Text(value, color = Color(0xFF172033), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge)
  }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(CardBorder)
  )
}

@Composable
private fun CompactDropdownRow(
  label: String,
  selectedId: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: "请选择"
  Box {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { expanded = true }
        .padding(vertical = 9.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(label, color = MutedText, style = MaterialTheme.typography.bodyLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(selectedLabel, color = Color(0xFF172033), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge)
        Text("›", color = MutedText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
      }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.second) },
          onClick = {
            expanded = false
            onSelect(option.first)
          }
        )
      }
    }
  }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(CardBorder)
  )
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .background(BlueSoft, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
    }
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF172033))
  }
}

@Composable
private fun CompactSectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(30.dp)
        .background(BlueSoft, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
    }
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF172033))
  }
}

@Composable
private fun OutboundSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .background(BlueSoft, RoundedCornerShape(10.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
    }
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF111827))
  }
}

@Composable
private fun OutboundOrderGoodsCard(
  selectedGoodsId: String,
  lines: List<OutboundLineState>,
  goods: List<com.warehouse.pda.data.Goods>,
  availableQuantity: (String) -> Int,
  onSelectGoods: (String) -> Unit,
  onAddGoods: () -> Unit,
  onEnterScan: (String) -> Unit,
  onRemove: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    lines.forEach { line ->
      OutboundOrderGoodsLine(
        line = line,
        goods = goods,
        availableQuantity = availableQuantity(line.goodsId),
        onEnterScan = onEnterScan,
        onRemove = onRemove
      )
    }

    OutboundAddGoodsPanel(
      selectedGoodsId = selectedGoodsId,
      goods = goods,
      onSelectGoods = onSelectGoods,
      onAddGoods = onAddGoods
    )
  }
}

@Composable
private fun OutboundAddGoodsPanel(
  selectedGoodsId: String,
  goods: List<com.warehouse.pda.data.Goods>,
  onSelectGoods: (String) -> Unit,
  onAddGoods: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text("添加货品", color = Color(0xFF172033), fontSize = 22.sp, fontWeight = FontWeight.Medium)
      OutboundSelectField(
        label = "选择货品",
        selectedId = selectedGoodsId,
        options = goods.map { it.id to "${it.name} (${it.code})" },
        placeholder = "请选择货品",
        onSelect = onSelectGoods
      )
      Button(
        onClick = onAddGoods,
        enabled = selectedGoodsId.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("添加", color = Color.White, fontWeight = FontWeight.Black)
      }
    }
  }
}

@Composable
private fun OutboundOrderGoodsLine(
  line: OutboundLineState,
  goods: List<com.warehouse.pda.data.Goods>,
  availableQuantity: Int,
  onEnterScan: (String) -> Unit,
  onRemove: (String) -> Unit
) {
  val statusLabel = outboundLineStatusLabel(line)
  val statusColor = outboundLineStatusColor(line)

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            goodsName(goods, line.goodsId),
            color = Color(0xFF111827),
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp
          )
          Text("代码: ${goodsCode(goods, line.goodsId)}", color = MutedText, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        IconButton(onClick = { onRemove(line.goodsId) }, modifier = Modifier.size(40.dp)) {
          Icon(Icons.Outlined.Close, contentDescription = "删除货品", tint = Color(0xFF424756), modifier = Modifier.size(28.dp))
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFDFDFF), RoundedCornerShape(16.dp))
          .border(1.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutboundLineStat(
          label = "可用库存",
          value = availableQuantity.toString(),
          dotColor = SuccessGreen,
          modifier = Modifier.weight(1f)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("状态", color = MutedText, fontWeight = FontWeight.Black, fontSize = 16.sp)
          CompactStatusChip(statusLabel, statusColor)
        }
      }

      OutlinedButton(
        onClick = { onEnterScan(line.goodsId) },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BluePrimary),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = BlueSoft, contentColor = BluePrimary)
      ) {
        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("进入扫码", color = BluePrimary, fontWeight = FontWeight.Black)
      }
    }
  }
}

@Composable
private fun OutboundLineStat(
  label: String,
  value: String,
  dotColor: Color,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, color = MutedText, fontWeight = FontWeight.Black, fontSize = 16.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
      Text(value, color = Color(0xFF111827), fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
  }
}

@Composable
private fun CompactStatusChip(label: String, background: Color) {
  Box(
    modifier = Modifier
      .background(background, RoundedCornerShape(999.dp))
      .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
  }
}

@Composable
private fun OutboundOrderProgressCard(lines: List<OutboundLineState>, invalidCount: Int) {
  val total = lines.size
  val completed = lines.count { outboundLineComplete(it) }
  val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()

  Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(BlueSoft, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.CallSplit, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
        }
        Text("出库进度", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF172033))
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(completed.toString(), color = BluePrimary, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
          Text(" / $total", color = Color(0xFF111726), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        }
        Text("SKU $completed/$total  |  异常 $invalidCount", color = MutedText, fontWeight = FontWeight.SemiBold)
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .background(Color(0xFFE9ECF3), RoundedCornerShape(99.dp))
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(progress.coerceIn(0f, 1f))
            .height(10.dp)
            .background(BluePrimary, RoundedCornerShape(99.dp))
        )
      }
      Text("${(progress * 100).toInt()}%", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = MutedText)
    }
  }
}

@Composable
private fun OutboundGoodsPickerCard(
  selectedGoodsId: String,
  activeGoodsId: String,
  lines: List<OutboundLineState>,
  goods: List<com.warehouse.pda.data.Goods>,
  onSelectGoods: (String) -> Unit,
  onSelectLine: (String) -> Unit,
  onAddGoods: () -> Unit
) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      DropdownField("添加货品", selectedGoodsId, goods.map { it.id to "${it.name} (${it.code})" }, onSelectGoods)
      Button(
        onClick = onAddGoods,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text("添加到出库单", color = Color.White, fontWeight = FontWeight.Black)
      }
      if (lines.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("出库品类", color = MutedText, fontWeight = FontWeight.SemiBold)
          lines.forEach { line ->
            val active = line.goodsId == activeGoodsId
            OutlinedButton(
              onClick = { onSelectLine(line.goodsId) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(goodsName(goods, line.goodsId), color = if (active) BluePrimary else Color(0xFF172033), fontWeight = FontWeight.Black)
                Text(progressLabel(line), color = MutedText, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OutboundCurrentGoodsSummaryStrip(
  line: OutboundLineState,
  goods: List<com.warehouse.pda.data.Goods>,
  availableQuantity: Int,
  onTargetChange: (String, String) -> Unit
) {
  val scannedCount = line.barcodes.size
  val invalidCount = line.barcodes.count { line.reviews[it]?.isValid == false }
  val targetQuantity = line.targetQuantity.toIntOrNull()
  val remainingLabel = targetQuantity?.let { (it - scannedCount).coerceAtLeast(0).toString() } ?: "-"

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            goodsName(goods, line.goodsId),
            color = Color(0xFF111827),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
          )
          Text("代码: ${goodsCode(goods, line.goodsId)}", color = MutedText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        StatusChip("库存 $availableQuantity", Color(0xFFE8FBF3), SuccessGreen)
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFFDFDFF), RoundedCornerShape(16.dp))
          .border(1.dp, CardBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
          .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutboundScanTargetMetric(
          value = line.targetQuantity,
          onValueChange = { onTargetChange(line.goodsId, it) },
          modifier = Modifier.weight(1f)
        )
        OutboundScanSummaryMetric("已扫", scannedCount.toString(), BluePrimary, Modifier.weight(1f))
        OutboundScanSummaryMetric("剩余", remainingLabel, modifier = Modifier.weight(1f))
        OutboundScanSummaryMetric("异常", invalidCount.toString(), if (invalidCount > 0) DangerRed else MutedText, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun OutboundScanTargetMetric(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("应出", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text("未设", fontSize = 14.sp, fontWeight = FontWeight.Black) },
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = Color(0xFF111827),
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      shape = RoundedCornerShape(12.dp),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    )
  }
}

@Composable
private fun OutboundScanSummaryMetric(
  label: String,
  value: String,
  valueColor: Color = Color(0xFF111827),
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
  }
}

@Composable
private fun OutboundCurrentGoodsCard(
  line: OutboundLineState,
  goods: List<com.warehouse.pda.data.Goods>,
  availableQuantity: Int,
  onTargetChange: (String, String) -> Unit,
  onRemove: (String) -> Unit
) {
  val target = line.targetQuantity.toIntOrNull()
  val remaining = target?.let { (it - line.barcodes.size).coerceAtLeast(0) }
  Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(BlueSoft, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
          }
          Text("当前货品", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF172033))
        }
        StatusChip("库存 $availableQuantity", Color(0xFFE8FBF3), SuccessGreen)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
          Text(goodsName(goods, line.goodsId), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF172033))
          Text(goodsCode(goods, line.goodsId), color = MutedText, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = { onRemove(line.goodsId) }) {
          Icon(Icons.Outlined.Close, contentDescription = "删除货品", tint = MutedText)
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = line.targetQuantity,
          onValueChange = { onTargetChange(line.goodsId, it) },
          label = { Text("应出") },
          placeholder = { Text("可选") },
          singleLine = true,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Box(
          modifier = Modifier
            .widthIn(min = 1.dp)
            .height(72.dp)
            .background(CardBorder)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("已扫 / 剩余", color = MutedText, fontWeight = FontWeight.SemiBold)
          Row(verticalAlignment = Alignment.Bottom) {
            Text(line.barcodes.size.toString(), color = BluePrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(" / ${remaining?.toString() ?: "-"}", color = Color(0xFF172033), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
          }
        }
      }
    }
  }
}

@Composable
private fun OutboundScanInputCard(
  value: String,
  onValueChange: (String) -> Unit,
  onAdd: (String) -> Unit,
  onClear: () -> Unit
) {
  Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(BlueSoft, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
        }
        Text("扫描条码", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF172033))
      }
      ScanInputRow(value = value, onValueChange = onValueChange, onAdd = onAdd, onClear = onClear)
    }
  }
}

@Composable
private fun OutboundScanRecordCard(
  line: OutboundLineState,
  onRemove: (String) -> Unit
) {
  val invalidCount = line.barcodes.count { line.reviews[it]?.isValid == false }
  Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .background(BlueSoft, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
          }
          Text("扫描记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF172033))
        }
        Text("已扫 ${line.barcodes.size}  异常 $invalidCount", color = MutedText, fontWeight = FontWeight.SemiBold)
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(CardBorder)
      )
      if (line.barcodes.isEmpty()) {
        Text(
          "暂无扫描记录",
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
          textAlign = TextAlign.Center,
          color = MutedText
        )
      } else {
        line.barcodes.forEachIndexed { index, barcode ->
          ScanRow(barcode, line.reviews[barcode], onRemove)
          if (index != line.barcodes.lastIndex) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PdaTopBrandBar() {
  Text(
    "仓库管理系统",
    color = PdaPrimaryStrong,
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Black,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 22.dp, bottom = 26.dp),
    textAlign = TextAlign.Center
  )
}

@Composable
private fun HomeGreetingHeader(
  displayName: String,
  warehouseLabel: String,
  warehouses: List<WarehouseRecord>,
  selectedWarehouseId: String,
  onWarehouseSelect: (String) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
      Text(
        "你好，$displayName",
        color = PdaOnSurfaceVariant,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium
      )
      Text(
        "请选择作业任务",
        color = PdaOnSurface,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Black
      )
    }
    WarehousePill(
      label = warehouseLabel,
      warehouses = warehouses,
      selectedWarehouseId = selectedWarehouseId,
      onWarehouseSelect = onWarehouseSelect
    )
  }
}

@Composable
private fun WarehousePill(
  label: String,
  warehouses: List<WarehouseRecord>,
  selectedWarehouseId: String,
  onWarehouseSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    Row(
      modifier = Modifier
        .widthIn(min = 154.dp, max = 190.dp)
        .background(PdaSurfaceVariant, RoundedCornerShape(12.dp))
        .border(1.dp, PdaOutlineVariant, RoundedCornerShape(12.dp))
        .clickable(enabled = warehouses.isNotEmpty()) { expanded = true }
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PdaPrimaryStrong, modifier = Modifier.size(22.dp))
      Text(
        label,
        color = PdaOnSurface,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        modifier = Modifier.weight(1f)
      )
      Icon(
        Icons.Outlined.KeyboardArrowDown,
        contentDescription = "选择作业仓库",
        tint = PdaOnSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      warehouses.forEach { warehouse ->
        DropdownMenuItem(
          text = {
            Row(
              modifier = Modifier.widthIn(min = 180.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (warehouse.id == selectedWarehouseId) {
                Icon(
                  Icons.Outlined.CheckCircle,
                  contentDescription = null,
                  tint = PdaPrimaryStrong,
                  modifier = Modifier.size(18.dp)
                )
              } else {
                Spacer(Modifier.size(18.dp))
              }
              Text(warehouse.name, fontWeight = FontWeight.SemiBold, color = PdaOnSurface)
            }
          },
          onClick = {
            expanded = false
            onWarehouseSelect(warehouse.id)
          }
        )
      }
    }
  }
}

@Composable
private fun PdaHomeTaskCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  tintColor: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(150.dp),
    onClick = onClick,
    shape = RoundedCornerShape(30.dp),
    colors = CardDefaults.cardColors(containerColor = PdaSurface),
    border = BorderStroke(1.dp, Color(0xFFE7E7F2))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(230.dp)
          .height(180.dp)
          .background(tintColor, RoundedCornerShape(topStart = 120.dp, bottomStart = 120.dp))
      )
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .background(accentColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = PdaOnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
              subtitle,
              color = PdaOnSurfaceVariant,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1
            )
          }
        }
        Text("›", color = Color(0xFF6E7280), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
      }
    }
  }
}

@Composable
private fun MainBottomBar(currentTab: MainTab, onSelect: (MainTab) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(PdaSurface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
      .navigationBarsPadding()
      .padding(horizontal = 10.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val items = listOf(
      Triple(MainTab.Home, "首页", Icons.Outlined.Home),
      Triple(MainTab.Query, "扫描", Icons.Outlined.QrCodeScanner),
      Triple(MainTab.Inbound, "入库", Icons.Outlined.Login),
      Triple(MainTab.Outbound, "出库", Icons.Outlined.Logout),
      Triple(MainTab.Profile, "我的", Icons.Outlined.AccountCircle)
    )
    items.forEach { (tab, label, icon) ->
      PdaBottomNavItem(
        selected = tab == currentTab,
        label = label,
        icon = icon,
        onClick = { onSelect(tab) }
      )
    }
  }
}

@Composable
private fun PdaBottomNavItem(
  selected: Boolean,
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(72.dp)
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .height(42.dp)
        .width(if (selected) 64.dp else 42.dp)
        .background(if (selected) BluePrimary else Color.Transparent, RoundedCornerShape(999.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        icon,
        contentDescription = label,
        tint = if (selected) Color.White else PdaOnSurfaceVariant,
        modifier = Modifier.size(26.dp)
      )
    }
    Text(
      label,
      color = if (selected) BluePrimary else PdaOnSurface,
      fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
      style = MaterialTheme.typography.labelLarge
    )
  }
}

@Composable
private fun AccountHeroCard(name: String) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(84.dp)
            .background(BluePrimary, RoundedCornerShape(22.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text("12", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
      }
      StatusChip("在线", Color(0xFFE6FBF3), MintAccent)
    }
  }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = MutedText, style = MaterialTheme.typography.titleMedium)
        Icon(Icons.Outlined.CallReceived, contentDescription = null, tint = accent)
      }
      Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color(0xFF111726))
    }
  }
}

@Composable
private fun BigActionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  background: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Card(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .padding(20.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(56.dp))
      Spacer(modifier = Modifier.size(20.dp))
      Text(title, color = contentColor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
private fun WideActionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 22.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(icon, contentDescription = null, tint = Color(0xFF161C2C))
      Spacer(modifier = Modifier.size(10.dp))
      Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
private fun RecentActivityRow(activity: RecentActivity) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(18.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(BlueSoft, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = BluePrimary)
      }
      Column {
        Text(activity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(activity.subtitle, color = MutedText)
      }
    }
    Text(activity.timeLabel, color = MutedText)
  }
}

@Composable
private fun StatusChip(label: String, background: Color, foreground: Color) {
  Row(
    modifier = Modifier
      .background(background, RoundedCornerShape(999.dp))
      .padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(modifier = Modifier.size(12.dp).background(foreground, CircleShape))
    Text(label, color = foreground, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun StatusBanner(text: String, tone: MessageTone) {
  val background = when (tone) {
    MessageTone.Success -> Color(0xFFE5F9EE)
    MessageTone.Error -> Color(0xFFFFECEC)
    MessageTone.Info -> BlueSoft
  }
  val foreground = when (tone) {
    MessageTone.Success -> SuccessGreen
    MessageTone.Error -> DangerRed
    MessageTone.Info -> BluePrimary
  }
  Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = background)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(Icons.Outlined.Info, contentDescription = null, tint = foreground)
      Text(text, color = Color(0xFF1D2235), fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun QueryPageShell(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(PdaBackground)
      .statusBarsPadding()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column(
        modifier = Modifier.padding(top = 34.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          title,
          color = PdaOnSurface,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Black
        )
        Text(
          subtitle,
          color = PdaOnSurfaceVariant,
          style = MaterialTheme.typography.bodyLarge
        )
      }
    }
    item {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
    item { Spacer(modifier = Modifier.height(96.dp)) }
  }
}

@Composable
private fun OperationPageShell(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  trailing: @Composable () -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(AppSurface)
      .statusBarsPadding()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color(0xFF3C4154))
        }
        Text(title, color = BluePrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) { trailing() }
      }
    }
    item {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun ConfigSectionCard(
  title: String,
  content: @Composable ColumnScope.() -> Unit
) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        title,
        modifier = Modifier.padding(18.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(CardBorder)
      )
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
  }
}

@Composable
private fun RadioLikeRow(label: String, selected: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFFF8F8FC), RoundedCornerShape(16.dp))
      .border(1.dp, if (selected) BluePrimary else CardBorder, RoundedCornerShape(16.dp))
      .padding(horizontal = 14.dp, vertical = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .border(2.dp, if (selected) BluePrimary else MutedText, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      if (selected) {
        Box(modifier = Modifier.size(12.dp).background(BluePrimary, CircleShape))
      }
    }
    Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun OperationFields(
  operation: PdaOperation,
  form: OperationFormState,
  masterData: WarehouseState,
  operatorName: String,
  onUpdate: ((OperationFormState) -> OperationFormState) -> Unit
) {
  val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val goods = masterData.goods.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val salespeople = masterData.salespeople.filter { it.status == "enabled" }
  val terminalStores = masterData.terminalStores.filter { it.status == "enabled" }
  val locations = masterData.locations.filter { it.status == "enabled" }

  when (operation) {
    PdaOperation.FactoryInbound -> {
      ConfigSectionCard("2. 选择仓库") {
        DropdownField("选择目标仓库", form.factoryWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            current.copy(
              factoryWarehouseId = selected,
              factoryLocationId = firstLocationId(selected, locations)
            )
          }
        }
      }
      ConfigSectionCard("3. 选择入库商品") {
        DropdownField("选择入库商品", form.factoryGoodsId, goods.map { it.id to "${it.name} (${it.code})" }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(factoryGoodsId = selected) }
        }
        DropdownField("选择目标库位", form.factoryLocationId, locations.filter { it.warehouseId == form.factoryWarehouseId }.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(factoryLocationId = selected) }
        }
        OutlinedTextField(
          value = form.factoryQuantity,
          onValueChange = { value -> onUpdate { current -> current.copy(factoryQuantity = value.filter(Char::isDigit)) } },
          label = { Text("入库数量") },
          placeholder = { Text("请输入本次到货数量") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp)
        )
        StatusBanner("厂家到货按数量增加库存，不再要求逐条码扫码。", MessageTone.Info)
      }
    }

    PdaOperation.TerminalInbound -> {
      ConfigSectionCard("2. 选择仓库") {
        DropdownField("选择目标仓库", form.terminalWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            current.copy(
              terminalWarehouseId = selected,
              terminalLocationId = firstLocationId(selected, locations)
            )
          }
        }
        DropdownField("选择目标库位", form.terminalLocationId, locations.filter { it.warehouseId == form.terminalWarehouseId }.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(terminalLocationId = selected) }
        }
      }
      ConfigSectionCard("3. 补充回仓信息") {
        DropdownField("选择货物", form.terminalGoodsId, goods.map { it.id to "${it.name} (${it.code})" }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(terminalGoodsId = selected) }
        }
        DropdownField("终端店铺", form.terminalStoreId, terminalStores.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(terminalStoreId = selected) }
        }
        OutlinedTextField(
          value = form.terminalProductionDate,
          onValueChange = { value -> onUpdate { current -> current.copy(terminalProductionDate = value) } },
          label = { Text("生产日期") },
          placeholder = { Text(LocalDate.now().toString()) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp)
        )
        val terminalGoods = goods.firstOrNull { it.id == form.terminalGoodsId }
        StatusBanner(
          when {
            form.terminalProductionDate.isBlank() -> "录入生产日期后再判断保质期"
            terminalGoods?.category == "health_wine" -> "保质期至 ${addYears(form.terminalProductionDate, 3)}"
            terminalGoods?.category == "baijiu" -> "白酒默认无保质期"
            else -> "请选择货物"
          },
          MessageTone.Info
        )
      }
    }

    PdaOperation.Transfer -> {
      ConfigSectionCard("2. 仓间调拨配置") {
        DropdownField("源仓库", form.transferSourceWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            val target = warehouses.firstOrNull { record -> record.id != selected }?.id.orEmpty()
            current.copy(
              transferSourceWarehouseId = selected,
              transferTargetWarehouseId = if (current.transferTargetWarehouseId == selected) target else current.transferTargetWarehouseId,
              transferTargetLocationId = firstLocationId(
                if (current.transferTargetWarehouseId == selected) target else current.transferTargetWarehouseId,
                locations
              )
            )
          }
        }
        DropdownField("目标仓库", form.transferTargetWarehouseId, warehouses.filter { it.id != form.transferSourceWarehouseId }.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            current.copy(
              transferTargetWarehouseId = selected,
              transferTargetLocationId = firstLocationId(selected, locations)
            )
          }
        }
        DropdownField("目标库位", form.transferTargetLocationId, locations.filter { it.warehouseId == form.transferTargetWarehouseId }.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(transferTargetLocationId = selected) }
        }
      }
    }

    PdaOperation.SalesOutbound -> {
      ConfigSectionCard("2. 销售出库配置") {
        DropdownField("出库仓库", form.salesWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(salesWarehouseId = selected) }
        }
        DropdownField("销售人员", form.salesSalespersonId, salespeople.map { it.id to "${it.name} (${it.code})" }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(salesSalespersonId = selected) }
        }
      }
    }

    PdaOperation.DirectOutbound -> {
      ConfigSectionCard("出库信息") {
        KeyValueRow("操作人员", operatorName.ifBlank { "-" })
        DropdownField("出库仓库", form.directSourceWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            val target = warehouses.firstOrNull { record -> record.id != selected }?.id.orEmpty()
            current.copy(
              directSourceWarehouseId = selected,
              directTargetWarehouseId = if (current.directTargetWarehouseId == selected) target else current.directTargetWarehouseId,
              directTargetLocationId = firstLocationId(
                if (current.directTargetWarehouseId == selected) target else current.directTargetWarehouseId,
                locations
              )
            )
          }
        }
        DropdownField(
          "出库去向",
          form.directDestinationType,
          listOf("sales" to "分配销售人员", "warehouse" to "发往目标仓库")
        ) { selected ->
          onUpdate { current: OperationFormState -> current.copy(directDestinationType = selected) }
        }
        if (form.directDestinationType == "warehouse") {
          DropdownField("目标仓库", form.directTargetWarehouseId, warehouses.filter { it.id != form.directSourceWarehouseId }.map { it.id to it.name }) { selected ->
            onUpdate { current: OperationFormState ->
              current.copy(
                directTargetWarehouseId = selected,
                directTargetLocationId = firstLocationId(selected, locations)
              )
            }
          }
          DropdownField("目标库位", form.directTargetLocationId, locations.filter { it.warehouseId == form.directTargetWarehouseId }.map { it.id to it.name }) { selected ->
            onUpdate { current: OperationFormState -> current.copy(directTargetLocationId = selected) }
          }
        } else {
          DropdownField("销售人员", form.directSalespersonId, salespeople.map { it.id to "${it.name} (${it.code})" }) { selected ->
            onUpdate { current: OperationFormState -> current.copy(directSalespersonId = selected) }
          }
        }
      }
    }

    PdaOperation.SalesReturn -> {
      ConfigSectionCard("2. 回流仓库配置") {
        DropdownField("回流仓库", form.returnWarehouseId, warehouses.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState ->
            current.copy(
              returnWarehouseId = selected,
              returnLocationId = firstLocationId(selected, locations)
            )
          }
        }
        DropdownField("回流库位", form.returnLocationId, locations.filter { it.warehouseId == form.returnWarehouseId }.map { it.id to it.name }) { selected ->
          onUpdate { current: OperationFormState -> current.copy(returnLocationId = selected) }
        }
      }
    }
  }
}

@Composable
private fun ContextStrip(operation: PdaOperation, form: OperationFormState, masterData: WarehouseState) {
  val locations = masterData.locations.filter { it.status == "enabled" }
  val warehouses = masterData.warehouses.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val goods = masterData.goods.filter { it.status == "enabled" }.sortedBy { it.sortOrder }
  val summary = when (operation) {
    PdaOperation.FactoryInbound -> listOf(
      operation.title,
      warehouseName(warehouses, form.factoryWarehouseId),
      locationName(locations, form.factoryLocationId),
      goodsName(goods, form.factoryGoodsId)
    )
    PdaOperation.TerminalInbound -> listOf(
      operation.title,
      warehouseName(warehouses, form.terminalWarehouseId),
      locationName(locations, form.terminalLocationId),
      goodsName(goods, form.terminalGoodsId)
    )
    PdaOperation.Transfer -> listOf(
      operation.title,
      warehouseName(warehouses, form.transferSourceWarehouseId),
      warehouseName(warehouses, form.transferTargetWarehouseId)
    )
    PdaOperation.SalesOutbound -> listOf(
      operation.title,
      warehouseName(warehouses, form.salesWarehouseId)
    )
    PdaOperation.DirectOutbound -> listOf(
      operation.title,
      warehouseName(warehouses, form.directSourceWarehouseId),
      if (form.directDestinationType == "warehouse") warehouseName(warehouses, form.directTargetWarehouseId) else "分配销售"
    )
    PdaOperation.SalesReturn -> listOf(
      operation.title,
      warehouseName(warehouses, form.returnWarehouseId),
      locationName(locations, form.returnLocationId)
    )
  }.filter { it.isNotBlank() }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White, RoundedCornerShape(14.dp))
      .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF394053))
    Text(summary.joinToString(" | "), color = Color(0xFF2B3142))
  }
}

@Composable
private fun ScanHero(
  countLabel: String?,
  countValue: String?,
  title: String,
  subtitle: String
) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Outlined.QrCode2, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(56.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(subtitle, color = MutedText, textAlign = TextAlign.Center)
      }
      if (!countLabel.isNullOrBlank() && !countValue.isNullOrBlank()) {
        Box(
          modifier = Modifier
            .padding(14.dp)
            .align(Alignment.TopEnd)
            .background(BlueSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text("$countLabel: $countValue", color = BluePrimary, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ScanStatusCard(
  countLabel: String?,
  countValue: String?,
  subtitle: String
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        .padding(horizontal = 18.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("扫码状态", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(subtitle, color = MutedText)
      }
      if (!countLabel.isNullOrBlank() && !countValue.isNullOrBlank()) {
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(countLabel, color = MutedText, style = MaterialTheme.typography.labelLarge)
          Text(
            countValue,
            color = BluePrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun QueryCommandCard(
  value: String,
  onValueChange: (String) -> Unit,
  loading: Boolean,
  onSearch: (String) -> Unit,
  onClear: () -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    keyboardController?.hide()
  }

  Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .background(BlueSoft, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(30.dp))
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              "等待扫码",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Black,
              color = Color(0xFF151B2D)
            )
            Text("扫码枪回车后自动查询", color = MutedText)
          }
        }
      }

      OutlinedTextField(
        value = value,
        onValueChange = { nextValue ->
          handleScannerTextChange(nextValue, onValueChange, onSearch)
        },
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester)
          .submitOnScannerEnter(value, onSearch),
        label = { Text("条码") },
        placeholder = { Text("扫描或手动输入条码") },
        shape = RoundedCornerShape(18.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, showKeyboardOnFocus = false),
        keyboardActions = KeyboardActions(onSearch = { onSearch(value) }, onDone = { onSearch(value) }),
        trailingIcon = {
          if (value.isNotBlank()) {
            IconButton(onClick = onClear) {
              Icon(Icons.Outlined.Close, contentDescription = "清空")
            }
          }
        }
      )
      Button(
        onClick = { onSearch(value) },
        enabled = !loading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
      ) {
        if (loading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
          Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.size(8.dp))
          Text("立即查询", color = Color.White, fontWeight = FontWeight.Black)
        }
      }
    }
  }
}

@Composable
private fun QueryInputRow(
  value: String,
  onValueChange: (String) -> Unit,
  loading: Boolean,
  onSearch: (String) -> Unit,
  onClear: () -> Unit
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = value,
      onValueChange = { nextValue ->
        handleScannerTextChange(nextValue, onValueChange, onSearch)
      },
      modifier = Modifier
        .weight(1f)
        .submitOnScannerEnter(value, onSearch),
      label = { Text("输入条码") },
      placeholder = { Text("扫码枪输入后可直接查询") },
      shape = RoundedCornerShape(16.dp),
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch(value) }, onDone = { onSearch(value) }),
      trailingIcon = {
        if (value.isNotBlank()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "清空")
          }
        }
      }
    )
    Button(
      onClick = { onSearch(value) },
      enabled = !loading,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      modifier = Modifier.height(56.dp)
    ) {
      if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
      } else {
        Text("查询", color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ScanInputRow(
  value: String,
  onValueChange: (String) -> Unit,
  onAdd: (String) -> Unit,
  onClear: () -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    keyboardController?.hide()
  }

  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = value,
      onValueChange = { nextValue ->
        handleScannerTextChange(nextValue, onValueChange, onAdd)
      },
      modifier = Modifier
        .weight(1f)
        .focusRequester(focusRequester)
        .submitOnScannerEnter(value, onAdd),
      label = { Text("输入条码") },
      shape = RoundedCornerShape(12.dp),
      placeholder = { Text("输入条码...") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { onAdd(value) }),
      trailingIcon = {
        if (value.isNotBlank()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "清空")
          }
        }
      }
    )
    Button(
      onClick = { onAdd(value) },
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      modifier = Modifier.height(56.dp)
    ) {
      Text("加入", color = Color.White, fontWeight = FontWeight.Bold)
    }
  }
}

private fun Modifier.submitOnScannerEnter(value: String, onSubmit: (String) -> Unit): Modifier {
  return onPreviewKeyEvent { event ->
    if (
      event.type == KeyEventType.KeyDown &&
      (event.key == Key.Enter || event.key == Key.NumPadEnter)
    ) {
      onSubmit(value)
      true
    } else {
      false
    }
  }
}

private fun handleScannerTextChange(
  nextValue: String,
  onValueChange: (String) -> Unit,
  onSubmit: (String) -> Unit
) {
  val cleaned = nextValue.replace("\r", "").replace("\n", "").trim()
  if (nextValue.contains('\n') || nextValue.contains('\r')) {
    onValueChange(cleaned)
    onSubmit(cleaned)
  } else {
    onValueChange(nextValue)
  }
}

@Composable
private fun ScanListCard(
  barcodeList: List<String>,
  barcodeReviews: Map<String, ReviewState>,
  onRemove: (String) -> Unit
) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column {
      Column(modifier = Modifier.padding(18.dp)) {
        Text("已扫描明细", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("成功 ${barcodeList.count { barcodeReviews[it]?.isValid != false }} 条，异常 ${barcodeList.count { barcodeReviews[it]?.isValid == false }} 条", color = MutedText)
      }
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(CardBorder)
      )
      if (barcodeList.isEmpty()) {
        Text("扫码后条码会显示在这里", modifier = Modifier.padding(18.dp), color = MutedText)
      } else {
        barcodeList.forEachIndexed { index, barcode ->
          val review = barcodeReviews[barcode]
          ScanRow(barcode, review, onRemove)
          if (index != barcodeList.lastIndex) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanRow(
  barcode: String,
  review: ReviewState?,
  onRemove: (String) -> Unit
) {
  val isError = review?.isValid == false
  val rowTint = if (isError) DangerRed else SuccessGreen
  val rowBackground = if (isError) Color(0xFFFFF4F4) else Color.White
  val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
      if (value == SwipeToDismissBoxValue.EndToStart) {
        onRemove(barcode)
        true
      } else {
        false
      }
    }
  )

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = false,
    enableDismissFromEndToStart = true,
    backgroundContent = {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(DangerRed)
          .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.White)
          Text("删除", color = Color.White, fontWeight = FontWeight.Black)
        }
      }
    }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(rowBackground)
        .padding(horizontal = 16.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(
        if (isError) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
        contentDescription = null,
        tint = rowTint
      )
      Column {
        Text(
          barcode,
          fontWeight = FontWeight.Bold,
          color = if (isError) DangerRed else Color(0xFF181E2E)
        )
        if (!review?.detail.isNullOrBlank()) {
          Text(review?.detail.orEmpty(), color = if (isError) DangerRed else MutedText)
        }
      }
      }
      IconButton(onClick = { onRemove(barcode) }) {
        Icon(Icons.Outlined.Close, contentDescription = "删除", tint = if (isError) DangerRed else Color(0xFFC4C7D8))
      }
    }
  }
}

@Composable
private fun InventoryResultCard(result: InventoryDetailResult, masterData: WarehouseState) {
  val item = result.item
  val isInStock = item.status == "in_stock"
  val ownerTitle = ownerTitle(item, masterData)
  Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            item.barcode,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF151B2D)
          )
          Text(ownerTitle, color = MutedText)
        }
        StatusChip(
          if (isInStock) "库存中" else "随销售",
          if (isInStock) Color(0xFFE8FBF3) else Color(0xFFFFF2E5),
          if (isInStock) SuccessGreen else OrangeAccent
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF7F8FE), RoundedCornerShape(20.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        QueryInfoRow("货品", goodsName(masterData.goods, item.goodsId).ifBlank { compactCode(item.goodsId) })
        QueryInfoRow("当前归属", ownerTitle)
        QueryInfoRow("生产日期", item.productionDate ?: "-")
        QueryInfoRow("保质期", item.shelfLifeDate ?: "-")
        QueryInfoRow("条码来源", inboundSourceLabel(item.inboundSource))
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(BlueSoft, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Outlined.CallSplit, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
        }
        Text("最近流转", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF151B2D))
      }

      if (result.movements.isEmpty()) {
        Text("暂无流转记录", color = MutedText)
      } else {
        result.movements.take(4).forEach { movement ->
          MovementTimelineItem(movement)
        }
      }
    }
  }
}

@Composable
private fun QueryInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Text(label, color = MutedText, fontWeight = FontWeight.SemiBold)
    Text(
      value,
      modifier = Modifier.padding(start = 12.dp),
      textAlign = TextAlign.End,
      color = Color(0xFF1D2438),
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
private fun MovementTimelineItem(movement: com.warehouse.pda.data.StockMovement) {
  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .background(BluePrimary, CircleShape)
      )
      Box(
        modifier = Modifier
          .widthIn(min = 1.dp)
          .height(54.dp)
          .background(CardBorder)
      )
    }
    Card(
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FC))
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(movement.note, fontWeight = FontWeight.Black, color = Color(0xFF172033))
        Text("${movement.fromLabel} -> ${movement.toLabel}", color = Color(0xFF4F566B))
        Text("${movement.operator} · ${movement.occurredAt}", color = MutedText, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun QueryEmptyStateCard() {
  Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(68.dp)
          .background(BlueSoft, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(36.dp))
      }
      Text("尚未查询", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
      Text("扫描后显示条码状态和当前归属。", textAlign = TextAlign.Center, color = MutedText)
    }
  }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(Icons.Outlined.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(44.dp))
      Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
      Text(subtitle, textAlign = TextAlign.Center, color = MutedText)
    }
  }
}

@Composable
private fun CenterLoadingCard(text: String) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      CircularProgressIndicator(color = BluePrimary)
      Text(text, color = MutedText)
    }
  }
}

private fun operationIcon(operation: PdaOperation): androidx.compose.ui.graphics.vector.ImageVector {
  return when (operation) {
    PdaOperation.FactoryInbound -> Icons.Outlined.Login
    PdaOperation.TerminalInbound -> Icons.Outlined.CallReceived
    PdaOperation.SalesReturn -> Icons.Outlined.CallReceived
    PdaOperation.Transfer -> Icons.Outlined.CallSplit
    PdaOperation.SalesOutbound -> Icons.Outlined.Logout
    PdaOperation.DirectOutbound -> Icons.Outlined.Logout
  }
}

@Composable
private fun HubOperationCard(
  operation: PdaOperation,
  accentColor: Color,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            when (operation) {
              PdaOperation.FactoryInbound -> Icons.Outlined.Login
              PdaOperation.TerminalInbound -> Icons.Outlined.CallReceived
              PdaOperation.SalesReturn -> Icons.Outlined.CallReceived
              PdaOperation.Transfer -> Icons.Outlined.CallSplit
              PdaOperation.SalesOutbound -> Icons.Outlined.Logout
              PdaOperation.DirectOutbound -> Icons.Outlined.Logout
            },
            contentDescription = null,
            tint = accentColor
          )
        }
        Column {
          Text(operation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          Text(operation.description, color = MutedText)
        }
      }
      Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = accentColor)
    }
  }
}

@Composable
private fun OutboundLineCards(
  lines: List<OutboundLineState>,
  selectedGoodsId: String,
  goods: List<com.warehouse.pda.data.Goods>,
  availableQuantity: (String) -> Int,
  onSelect: (String) -> Unit,
  onTargetChange: (String, String) -> Unit,
  onRemove: (String) -> Unit
) {
  if (lines.isEmpty()) {
    Text("还没有货品行。添加后扫码会按当前选中的货品归类。", color = MutedText)
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    lines.forEach { line ->
      val selected = line.goodsId == selectedGoodsId
      val invalidCount = line.barcodes.count { line.reviews[it]?.isValid == false }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(if (selected) BlueSoft else Color(0xFFF8F8FC), RoundedCornerShape(20.dp))
          .border(1.dp, if (selected) BluePrimary else CardBorder, RoundedCornerShape(20.dp))
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(
              goodsName(goods, line.goodsId),
              fontWeight = FontWeight.Black,
              color = Color(0xFF172033),
              style = MaterialTheme.typography.titleLarge
            )
            Text("可用 ${availableQuantity(line.goodsId)} 件", color = MutedText, style = MaterialTheme.typography.bodyMedium)
          }
          IconButton(onClick = { onRemove(line.goodsId) }) {
            Icon(Icons.Outlined.Close, contentDescription = "删除货品行", tint = MutedText)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { onSelect(line.goodsId) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text(if (selected) "正在扫码" else "开始扫码", fontWeight = FontWeight.Bold)
          }
          OutlinedTextField(
            value = line.targetQuantity,
            onValueChange = { onTargetChange(line.goodsId, it) },
            label = { Text("数量") },
            placeholder = { Text("可选") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("已扫 ${line.barcodes.size}", color = Color(0xFF172033), fontWeight = FontWeight.Bold)
          Text(remainingLabel(line), color = MutedText)
          Text("异常 $invalidCount", color = if (invalidCount > 0) DangerRed else MutedText)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
  label: String,
  selectedId: String,
  options: List<Pair<String, String>>,
  onSelect: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: "请选择"

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2A3040))
    OutlinedButton(
      onClick = { expanded = true },
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp)
    ) {
      Text(selectedLabel, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.second) },
          onClick = {
            expanded = false
            onSelect(option.first)
          }
        )
      }
    }
  }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, color = MutedText)
    Text(value, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
  }
}

private fun firstLocationId(warehouseId: String, locations: List<StorageLocation>): String {
  return locations.firstOrNull { it.warehouseId == warehouseId }?.id.orEmpty()
}

private fun warehouseName(warehouses: List<com.warehouse.pda.data.WarehouseRecord>, id: String): String {
  return warehouses.firstOrNull { it.id == id }?.name.orEmpty()
}

private fun locationName(locations: List<StorageLocation>, id: String): String {
  return locations.firstOrNull { it.id == id }?.name.orEmpty()
}

private fun goodsName(goods: List<com.warehouse.pda.data.Goods>, id: String): String {
  return goods.firstOrNull { it.id == id }?.name.orEmpty()
}

private fun goodsCode(goods: List<com.warehouse.pda.data.Goods>, id: String): String {
  return goods.firstOrNull { it.id == id }?.code.orEmpty()
}

private fun outboundContextLabel(form: OperationFormState, masterData: WarehouseState): String {
  val warehouse = warehouseName(masterData.warehouses, form.directSourceWarehouseId)
  val destination = if (form.directDestinationType == "warehouse") "调拨出库" else "分销销售"
  return listOf(warehouse, destination).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun outboundLineComplete(line: OutboundLineState): Boolean {
  val target = line.targetQuantity.toIntOrNull() ?: return false
  if (target <= 0) return false
  if (line.barcodes.any { line.reviews[it]?.isValid == false }) return false
  return line.barcodes.size >= target
}

private fun outboundLineStatusLabel(line: OutboundLineState): String {
  return when {
    outboundLineComplete(line) -> "已完成"
    line.barcodes.isNotEmpty() -> "进行中"
    else -> "待扫码"
  }
}

private fun outboundLineStatusColor(line: OutboundLineState): Color {
  return when (outboundLineStatusLabel(line)) {
    "已完成" -> SuccessGreen
    "进行中" -> BluePrimary
    else -> OrangeAccent
  }
}

private fun ownerTitle(item: com.warehouse.pda.data.InventoryItem, masterData: WarehouseState): String {
  if (item.ownerType == "warehouse") {
    val warehouse = warehouseName(masterData.warehouses, item.warehouseId.orEmpty()).ifBlank { "未知仓库" }
    val location = locationName(masterData.locations, item.locationId.orEmpty())
    return if (location.isBlank()) warehouse else "$warehouse / $location"
  }
  return salespersonName(masterData.salespeople, item.salespersonId.orEmpty()).ifBlank { "未知销售人员" }
}

private fun inboundSourceLabel(source: String): String {
  return when (source) {
    "factory" -> "厂家到货"
    "terminal_return" -> "终端退换货"
    else -> source.ifBlank { "-" }
  }
}

private fun compactCode(value: String): String {
  if (value.length <= 12) return value.ifBlank { "-" }
  return "${value.take(6)}…${value.takeLast(4)}"
}

private fun availableQuantity(masterData: WarehouseState, warehouseId: String, goodsId: String): Int {
  return masterData.warehouseStocks.orEmpty().firstOrNull {
    it.warehouseId == warehouseId && it.goodsId == goodsId
  }?.quantity ?: 0
}

private fun salespersonName(salespeople: List<com.warehouse.pda.data.Salesperson>, id: String): String {
  return salespeople.firstOrNull { it.id == id }?.name.orEmpty()
}

private fun progressLabel(line: OutboundLineState): String {
  val target = line.targetQuantity.toIntOrNull()
  return if (target == null) "${line.barcodes.size}" else "${line.barcodes.size}/$target"
}

private fun remainingLabel(line: OutboundLineState): String {
  val target = line.targetQuantity.toIntOrNull() ?: return "未设目标"
  val remaining = (target - line.barcodes.size).coerceAtLeast(0)
  return if (remaining == 0) "已完成" else "剩余 $remaining 件"
}

private fun submitLabel(operation: PdaOperation): String {
  return when (operation) {
    PdaOperation.FactoryInbound,
    PdaOperation.TerminalInbound -> "入库"
    PdaOperation.Transfer -> "挪仓"
    PdaOperation.SalesOutbound -> "出库"
    PdaOperation.DirectOutbound -> "出库"
    PdaOperation.SalesReturn -> "回流"
  }
}

private fun addYears(date: String, years: Int): String {
  return runCatching {
    LocalDate.parse(date).plusYears(years.toLong()).toString()
  }.getOrDefault("")
}

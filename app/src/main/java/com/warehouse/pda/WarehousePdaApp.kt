package com.warehouse.pda

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.warehouse.pda.data.InventoryDetailResult
import com.warehouse.pda.data.StorageLocation
import com.warehouse.pda.data.WarehouseState
import com.warehouse.pda.ui.theme.WarehousePdaTheme
import java.time.LocalDate

private val BluePrimary = Color(0xFF3651E3)
private val BlueSoft = Color(0xFFE8EDFF)
private val OrangeAccent = Color(0xFFCC6B11)
private val MintAccent = Color(0xFF20B46A)
private val BlackPanel = Color(0xFF080808)
private val AppSurface = Color(0xFFF7F7FB)
private val CardBorder = Color(0xFFE7E8F1)
private val MutedText = Color(0xFF7B8094)
private val DangerRed = Color(0xFFE64A4A)
private val SuccessGreen = Color(0xFF16B37E)

@Composable
fun WarehousePdaRoot(viewModel: MainViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.message) {
    val text = uiState.message?.text ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(text)
    viewModel.clearMessage()
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
      title = "入库中心",
      subtitle = "选择业务并完成配置后开始扫码",
      operations = PdaOperation.entries.filter { it.group == OperationGroup.Inbound },
      accentColor = BluePrimary,
      viewModel = viewModel
    )
    MainTab.Outbound -> OperationHubScreen(
      title = "出库中心",
      subtitle = "选择出库业务并进入扫码作业",
      operations = PdaOperation.entries.filter { it.group == OperationGroup.Outbound },
      accentColor = OrangeAccent,
      viewModel = viewModel
    )
    MainTab.Profile -> ProfileScreen(uiState, viewModel)
  }
}

@Composable
private fun DashboardScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val currentUser = uiState.currentUser ?: return
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BlackPanel)
      .statusBarsPadding()
      .padding(horizontal = 20.dp)
  ) {
    Text(
      "WMS 终端",
      color = Color.White,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Black,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 18.dp),
      textAlign = TextAlign.Center
    )
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      item {
        AccountHeroCard(currentUser.displayName)
      }
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          MetricCard("今日入库", uiState.inboundTodayCount.toString(), BluePrimary, Modifier.weight(1f))
          MetricCard("今日出库", uiState.outboundTodayCount.toString(), OrangeAccent, Modifier.weight(1f))
        }
      }
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          BigActionCard(
            title = "入库",
            icon = Icons.Outlined.Login,
            background = BluePrimary,
            contentColor = Color.White,
            modifier = Modifier.weight(1f)
          ) { viewModel.openTab(MainTab.Inbound) }
          BigActionCard(
            title = "出库",
            icon = Icons.Outlined.Logout,
            background = Color.White,
            contentColor = OrangeAccent,
            modifier = Modifier.weight(1f)
          ) { viewModel.openTab(MainTab.Outbound) }
        }
      }
      item {
        WideActionCard(
          title = "扫码查询",
          icon = Icons.Outlined.QrCodeScanner
        ) { viewModel.openTab(MainTab.Query) }
      }
      uiState.lastSubmitSummary?.let { summary ->
        item {
          StatusBanner(summary, MessageTone.Success)
        }
      }
      if (uiState.recentActivities.isNotEmpty()) {
        item {
          Text("最近操作", color = Color(0xFF1D2235), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        item {
          Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
              uiState.recentActivities.forEachIndexed { index, activity ->
                RecentActivityRow(activity)
                if (index != uiState.recentActivities.lastIndex) {
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
      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
private fun QueryScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val result = uiState.queryForm.result
  QueryPageShell(title = "扫码查询", subtitle = "扫描或输入条码，查看当前库存归属与流转记录") {
    ScanHero(
      countLabel = if (uiState.queryForm.resultBarcode.isNotBlank()) "最近查询" else null,
      countValue = uiState.queryForm.resultBarcode.ifBlank { null },
      title = "准备查询",
      subtitle = "将条码对准扫码枪或在此处输入"
    )
    QueryInputRow(
      value = uiState.queryForm.barcodeInput,
      onValueChange = viewModel::updateQueryInput,
      loading = uiState.queryForm.loading,
      onSearch = viewModel::queryBarcode,
      onClear = viewModel::clearQueryResult
    )
    if (uiState.queryForm.loading) {
      CenterLoadingCard("正在读取条码详情")
    } else if (result != null) {
      InventoryResultCard(result)
    } else {
      EmptyStateCard("尚未查询", "扫码后会展示库存归属、生产日期和最近流转记录。")
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
      HubOperationCard(operation = operation, accentColor = accentColor) {
        viewModel.openOperation(operation)
      }
    }
  }
}

@Composable
private fun ProfileScreen(uiState: AppUiState, viewModel: MainViewModel) {
  val currentUser = uiState.currentUser ?: return
  val context = LocalContext.current
  val updateState = uiState.appUpdateState
  val release = updateState.release
  QueryPageShell(title = "我的", subtitle = "当前账号、角色与终端说明") {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
        Text("该终端用于扫码作业、扫码查询和最近作业回看，不承担高风险系统维护。", color = MutedText)
      }
    }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
      modifier = Modifier.fillMaxWidth(),
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
  OperationPageShell(
    title = "仓库管理系统",
    onBack = { viewModel.goBackFromOperation(operation, fromScan = false) },
    trailing = {}
  ) {
    Text(operation.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
    Text("请选择作业参数并确认目标仓库，准备扫码。", color = MutedText)
    ConfigSectionCard("1. 作业类型") {
      if (operation.group == OperationGroup.Inbound) {
        RadioLikeRow("厂家到货", operation == PdaOperation.FactoryInbound)
        RadioLikeRow("终端退换货", operation == PdaOperation.TerminalInbound)
        RadioLikeRow("销售退回", operation == PdaOperation.SalesReturn)
      } else {
        RadioLikeRow("挪仓", operation == PdaOperation.Transfer)
        RadioLikeRow("销售出库", operation == PdaOperation.SalesOutbound)
      }
    }
    OperationFields(
      operation = operation,
      form = form,
      masterData = masterData,
      onUpdate = viewModel::updateForm
    )
    Button(
      onClick = { viewModel.startScanning(operation) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = Color.White)
      Spacer(modifier = Modifier.size(8.dp))
      Text("开始扫码", color = Color.White, fontWeight = FontWeight.Black)
    }
  }
}

@Composable
private fun OperationScanScreen(uiState: AppUiState, operation: PdaOperation, viewModel: MainViewModel) {
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
      ScanHero(
        countLabel = "已扫",
        countValue = barcodeList.size.toString(),
        title = "准备扫描",
        subtitle = "请将条码对准扫码枪或在此处输入"
      )
      ScanInputRow(
        value = barcodeInput,
        onValueChange = { viewModel.updateBarcodeInput(operation, it) },
        onAdd = { viewModel.addBarcodes(operation) },
        onClear = { viewModel.clearBarcodes(operation) }
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
private fun MainBottomBar(currentTab: MainTab, onSelect: (MainTab) -> Unit) {
  NavigationBar(containerColor = Color.White) {
    val items = listOf(
      Triple(MainTab.Home, "首页", Icons.Outlined.Home),
      Triple(MainTab.Query, "扫描", Icons.Outlined.QrCodeScanner),
      Triple(MainTab.Inbound, "入库", Icons.Outlined.Login),
      Triple(MainTab.Outbound, "出库", Icons.Outlined.Logout),
      Triple(MainTab.Profile, "我的", Icons.Outlined.AccountCircle)
    )
    items.forEach { (tab, label, icon) ->
      NavigationBarItem(
        selected = tab == currentTab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
      )
    }
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
      .background(BlackPanel)
      .statusBarsPadding()
      .padding(horizontal = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitle, color = Color(0xFFC1C5D4))
      }
    }
    item {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
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
  onUpdate: ((OperationFormState) -> OperationFormState) -> Unit
) {
  val warehouses = masterData.warehouses.filter { it.status == "enabled" }
  val goods = masterData.goods.filter { it.status == "enabled" }
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
  val warehouses = masterData.warehouses.filter { it.status == "enabled" }
  val goods = masterData.goods.filter { it.status == "enabled" }
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
private fun QueryInputRow(
  value: String,
  onValueChange: (String) -> Unit,
  loading: Boolean,
  onSearch: () -> Unit,
  onClear: () -> Unit
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
      label = { Text("输入条码") },
      placeholder = { Text("扫码枪输入后可直接查询") },
      shape = RoundedCornerShape(16.dp),
      trailingIcon = {
        if (value.isNotBlank()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "清空")
          }
        }
      }
    )
    Button(
      onClick = onSearch,
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

@Composable
private fun ScanInputRow(
  value: String,
  onValueChange: (String) -> Unit,
  onAdd: () -> Unit,
  onClear: () -> Unit
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
      label = { Text("输入条码") },
      shape = RoundedCornerShape(12.dp),
      placeholder = { Text("输入条码...") },
      trailingIcon = {
        if (value.isNotBlank()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Close, contentDescription = "清空")
          }
        }
      }
    )
    Button(
      onClick = onAdd,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
      modifier = Modifier.height(56.dp)
    ) {
      Text("加入", color = Color.White, fontWeight = FontWeight.Bold)
    }
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

@Composable
private fun ScanRow(
  barcode: String,
  review: ReviewState?,
  onRemove: (String) -> Unit
) {
  val isError = review?.isValid == false
  val rowTint = if (isError) DangerRed else SuccessGreen
  val rowBackground = if (isError) Color(0xFFFFF4F4) else Color.White
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

@Composable
private fun InventoryResultCard(result: InventoryDetailResult) {
  Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text(result.item.barcode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
          Text(if (result.item.ownerType == "warehouse") "当前在仓库存中" else "当前在销售人员名下", color = MutedText)
        }
        StatusChip(
          if (result.item.status == "in_stock") "库存中" else "随销售",
          if (result.item.status == "in_stock") Color(0xFFE8FBF3) else Color(0xFFFFF2E5),
          if (result.item.status == "in_stock") SuccessGreen else OrangeAccent
        )
      }
      KeyValueRow("货物 ID", result.item.goodsId)
      KeyValueRow("仓库 ID", result.item.warehouseId ?: "-")
      KeyValueRow("库位 ID", result.item.locationId ?: "-")
      KeyValueRow("生产日期", result.item.productionDate ?: "-")
      KeyValueRow("保质期", result.item.shelfLifeDate ?: "-")
      Text("最近流转", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      result.movements.take(4).forEach { movement ->
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FC))
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(movement.note, fontWeight = FontWeight.SemiBold)
            Text("${movement.fromLabel} -> ${movement.toLabel}", color = MutedText)
            Text("${movement.operator} · ${movement.occurredAt}", color = MutedText)
          }
        }
      }
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

private fun submitLabel(operation: PdaOperation): String {
  return when (operation) {
    PdaOperation.FactoryInbound,
    PdaOperation.TerminalInbound -> "入库"
    PdaOperation.Transfer -> "挪仓"
    PdaOperation.SalesOutbound -> "出库"
    PdaOperation.SalesReturn -> "回流"
  }
}

private fun addYears(date: String, years: Int): String {
  return runCatching {
    LocalDate.parse(date).plusYears(years.toLong()).toString()
  }.getOrDefault("")
}

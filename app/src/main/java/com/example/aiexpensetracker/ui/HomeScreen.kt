package com.example.aiexpensetracker.ui

import android.app.DatePickerDialog
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog // 🟢 必须导入这个
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.common.api.ApiException
import android.app.Activity
import android.content.Context
import android.content.Intent
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Notifications
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check // 🟢 修复 'Check' 图标报错
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Cloud // 如果报错，看下面的解决方案
import androidx.compose.material.icons.filled.Refresh // 备用图标
import com.example.aiexpensetracker.R
// 🟢 修复 Restore 图标报红
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.*
import com.example.aiexpensetracker.database.ExpenseEntity
import com.example.aiexpensetracker.database.SubscriptionEntity
import com.example.aiexpensetracker.viewmodel.AccountBalance
import com.example.aiexpensetracker.viewmodel.HomeViewModel
import com.example.aiexpensetracker.network.ReceiptAnalysis
import com.example.aiexpensetracker.viewmodel.BreakdownItem // 确保你有定义这个
import com.example.aiexpensetracker.utils.VipUtils
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.layout.Spacer // 确保导入了
import androidx.compose.foundation.layout.width // 确保导入了
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aiexpensetracker.viewmodel.SubscriptionSuggestion
import kotlin.math.abs
import kotlin.math.roundToInt // 🟢 用于凑整计算
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen { Home, Stats, Budget, Learning, Logs, About, Subscriptions, Feedback, Premium }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkTheme: Boolean,
    accountList: List<String>,
    onToggleTheme: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    val scannedReceipt by viewModel.scannedReceipt.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()

    LaunchedEffect(Unit) {
        // 每次启动主页，静默检查一次更新（不弹 Toast）
        viewModel.checkForUpdates(context, showToastIfLatest = false)
    }

    // 🟢 1. 新增：控制“选择来源”弹窗的状态
    var showSourceSelectionDialog by remember { mutableStateOf(false) }

    // 🟢 2. 新增：临时变量，用于存储相机拍照后的 URI
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // 🟢 3. 新增：创建临时文件 URI 的辅助函数 (用于相机)
    fun createTempPictureUri(): android.net.Uri {
        val tempFile = File.createTempFile("receipt_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
        // 注意：这里的 authority 必须和 AndroidManifest.xml 里的保持一致
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
    }

    // --- Launchers ---

    // A. 相册选择器 (保持不变)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.scanReceipt(context, it) {
                showAddExpenseDialog = true
            }
        }
    }

    // 🟢 B. 新增：相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            // 拍照成功，开始扫描，并传入图片 URI
            viewModel.scanReceipt(context, tempCameraUri!!) {
                showAddExpenseDialog = true
            }
        }
    }

    // 2. 在 AddExpenseDialog 打开时，填充数据
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            expenseToEdit = expenseToEdit,
            scannedReceipt = scannedReceipt,

            // 🟢 关键：传入 ViewModel 里的临时图片路径 (无论是相册还是相机选的)
            scannedImageUri = viewModel.currentScannedUri?.toString(),

            accountList = accountList,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
                viewModel.clearScannedReceipt()
            },
            onConfirm = { amt, merch, cat, type, ts, nt, acc, target, imgPath ->
                val newExpense = ExpenseEntity(
                    id = expenseToEdit?.id ?: 0,
                    amount = amt,
                    merchant = merch,
                    category = cat,
                    type = type,
                    timestamp = ts,
                    note = nt,
                    accountName = acc,
                    targetAccountName = target,
                    originalText = if (scannedReceipt != null) "OCR Scan" else (expenseToEdit?.originalText ?: "Manual"),
                    imagePath = imgPath
                )
                viewModel.addExpense(newExpense)
                showAddExpenseDialog = false
                expenseToEdit = null
                viewModel.clearScannedReceipt()
            }
        )
    }

    var isTracking by remember { mutableStateOf(prefs.getBoolean("tracking_enabled", true)) }
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var isBalanceVisible by remember { mutableStateOf(prefs.getBoolean("balance_visible", true)) }

    BackHandler(enabled = currentScreen != AppScreen.Home) { currentScreen = AppScreen.Home }

    val currentMonthMillis by viewModel.currentMonth.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var isCalendarView by remember { mutableStateOf(false) }

    val accountBalances by viewModel.accountBalances.collectAsState(initial = emptyList())
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val subscriptionSuggestions by viewModel.subscriptionSuggestions.collectAsState()
    val currentSuggestion = subscriptionSuggestions.firstOrNull() // 永远只显示队列里的第一个
    var suggestionToEdit by remember { mutableStateOf<SubscriptionSuggestion?>(null) } // 控制编辑弹窗

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var showSettingsSheet by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.initializeDriveHelper(context, account)
                Toast.makeText(context, "Login Success: ${account.email}", Toast.LENGTH_SHORT).show()
                viewModel.performCloudRestore(context)
            } catch (e: ApiException) {
                Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { viewModel.exportCsvForExcel(context, it) } }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { viewModel.backupData(context, it) } }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.restoreData(context, it) } }

    DisposableEffect(Unit) {
        val listener = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) isTracking = prefs.getBoolean("tracking_enabled", true)
        }
        val lifecycle = (context as androidx.lifecycle.LifecycleOwner).lifecycle
        lifecycle.addObserver(listener)
        onDispose { lifecycle.removeObserver(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery, onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), singleLine = true,
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        LaunchedEffect(Unit) { focusRequester.requestFocus() }
                    } else {
                        Column {
                            val titleText = when(currentScreen) {
                                AppScreen.Home -> stringResource(R.string.app_title)
                                AppScreen.Stats -> stringResource(R.string.stats_title)
                                AppScreen.Budget -> stringResource(R.string.budget_title)
                                AppScreen.Learning -> stringResource(R.string.settings_memory)
                                AppScreen.Logs -> stringResource(R.string.settings_ignored)
                                AppScreen.About -> stringResource(R.string.settings_about)
                                AppScreen.Subscriptions -> stringResource(R.string.settings_subscriptions)
                                AppScreen.Feedback -> stringResource(R.string.settings_feedback)
                                AppScreen.Premium -> stringResource(R.string.settings_premium)

                            }
                            Text(titleText, style = MaterialTheme.typography.titleMedium)
                            if (currentScreen == AppScreen.Home) {
                                Text(text = if (isTracking) stringResource(R.string.service_running) else stringResource(R.string.service_paused), style = MaterialTheme.typography.labelSmall, color = if (isTracking) Color(0xFF4CAF50) else Color.LightGray)
                            }
                        }
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false; viewModel.onSearchQueryChanged("") }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                    } else if (currentScreen == AppScreen.Home) {
                        IconButton(onClick = { isBalanceVisible = !isBalanceVisible; prefs.edit().putBoolean("balance_visible", isBalanceVisible).apply() }) { Icon(painter = painterResource(id = if (isBalanceVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_secure), contentDescription = "Privacy") }
                        IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, contentDescription = "Search") }
                        IconButton(onClick = { isCalendarView = !isCalendarView }) { Icon(imageVector = if (isCalendarView) Icons.AutoMirrored.Filled.List else Icons.Default.DateRange, contentDescription = "View") }
                        Switch(checked = isTracking, onCheckedChange = { isChecked -> isTracking = isChecked; prefs.edit().putBoolean("tracking_enabled", isChecked).apply(); val action = if (isChecked) "com.example.aiexpensetracker.RESUME_SERVICE" else "com.example.aiexpensetracker.STOP_SERVICE"; val intent = Intent(action).apply { setPackage(context.packageName) }; context.sendBroadcast(intent) }, modifier = Modifier.scale(0.8f))
                        IconButton(onClick = { showSettingsSheet = true }) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = null) }, label = { Text(stringResource(R.string.tab_transactions)) }, selected = currentScreen == AppScreen.Home, onClick = { currentScreen = AppScreen.Home })
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PieChart, null) },
                    label = { Text(stringResource(R.string.budget)) },
                    selected = currentScreen == AppScreen.Budget,
                    onClick = { currentScreen = AppScreen.Budget }
                )
                NavigationBarItem(icon = { Icon(Icons.Default.Info, contentDescription = null) }, label = { Text(stringResource(R.string.tab_stats)) }, selected = currentScreen == AppScreen.Stats, onClick = { currentScreen = AppScreen.Stats })
            }
        },
        floatingActionButton = {
            // 🟢 4. 修改 FAB：不再展开子菜单，而是点击后显示“选择来源”弹窗
            if (currentScreen == AppScreen.Home) {
                FloatingActionButton(
                    onClick = {
                        // 点击打开选择弹窗
                        showSourceSelectionDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.Home -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(visible = currentSuggestion != null, enter = fadeIn(), exit = fadeOut()) {
                            currentSuggestion?.let { suggestion ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("AI Detected Subscription / AI检测到订阅", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Add '${suggestion.expense.merchant}' (RM ${String.format("%.2f", suggestion.expense.amount)}) as a monthly subscription?", style = MaterialTheme.typography.bodyMedium)

                                        Spacer(Modifier.height(12.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            // 按钮 1：永久拉黑
                                            TextButton(onClick = { viewModel.dismissSuggestion(context, suggestion, ignoreMerchantForever = true) }) {
                                                Text("Never Ask", color = Color.Gray, fontSize = 12.sp)
                                            }
                                            Row {
                                                // 按钮 2：仅忽略本次
                                                TextButton(onClick = { viewModel.dismissSuggestion(context, suggestion, ignoreMerchantForever = false) }) {
                                                    Text("Not This Time")
                                                }
                                                // 按钮 3：添加并编辑
                                                Button(onClick = { suggestionToEdit = suggestion }) {
                                                    Text("Yes, Add")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        TransactionListScreen(viewModel, isSearchActive, isCalendarView, accountBalances.map { it.name }, accountBalances, isBalanceVisible, subscriptions)
                    }
                }
                AppScreen.Budget -> BudgetScreen(viewModel = viewModel)
                AppScreen.Stats -> StatsScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = {
                        currentScreen = AppScreen.Home // 🟢 点击时，把当前页面切回到主页(流水列表)
                    }
                )
                AppScreen.Learning -> LearningListScreen(viewModel)
                AppScreen.Logs -> IgnoredLogsScreen(viewModel)
                AppScreen.About -> AboutScreen()
                AppScreen.Subscriptions -> SubscriptionScreen(viewModel) { currentScreen = AppScreen.Home }
                AppScreen.Feedback -> FeedbackScreen(viewModel = viewModel, onBack = { currentScreen = AppScreen.Home })
                AppScreen.Premium -> PremiumScreen(viewModel = viewModel, onBack = { currentScreen = AppScreen.Home })

            }
        }

        // 🟢 5. 新增：来源选择弹窗 (手动 / 拍照 / 相册)
        if (showSourceSelectionDialog) {
            AlertDialog(
                onDismissRequest = { showSourceSelectionDialog = false },
                title = { Text(stringResource(R.string.add_trans)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 选项 1: 手动
                        OutlinedButton(
                            onClick = {
                                showSourceSelectionDialog = false
                                expenseToEdit = null // 清空旧数据
                                viewModel.clearScannedReceipt()
                                showAddExpenseDialog = true // 直接打开弹窗
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.manual_input))
                        }

                        Divider()

                        // 选项 2: 拍照
                        OutlinedButton(
                            onClick = {
                                showSourceSelectionDialog = false
                                // 创建临时 URI 并启动相机
                                tempCameraUri = createTempPictureUri()
                                cameraLauncher.launch(tempCameraUri!!)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.camera))
                        }

                        // 选项 3: 相册
                        OutlinedButton(
                            onClick = {
                                showSourceSelectionDialog = false
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.gallery))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSourceSelectionDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }
    }

    if (showSettingsSheet) {
        val settingsSheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = settingsSheetState, // 🟢 2. 把上面定义的状态绑定到这里！
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 48.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(stringResource(R.string.general), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                val currentLocale = androidx.core.os.LocaleListCompat.getAdjustedDefault()[0]
                val isEnglish = currentLocale?.language == "en"
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    trailingContent = {
                        Switch(
                            checked = isEnglish,
                            onCheckedChange = { checked ->
                                showSettingsSheet = false
                                onLanguageChange(if (checked) "en" else "zh")
                            }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dark_mode)) },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { checked -> onToggleTheme(checked) }
                        )
                    }
                )

                var currentPersona by remember { mutableStateOf(viewModel.getAiPersona(context)) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_persona)) },
                    supportingContent = {
                        val label = when(currentPersona) {
                            "STRICT" -> stringResource(R.string.persona_toxic)
                            "GENTLE" -> stringResource(R.string.persona_gentle)
                            else -> stringResource(R.string.persona_neutral)
                        }
                        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            val next = when(currentPersona) {
                                "NEUTRAL"->"STRICT"
                                "STRICT"->"GENTLE"
                                else->"NEUTRAL"
                            }
                            currentPersona = next
                            viewModel.setAiPersona(context, next)
                        }) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        currentScreen = AppScreen.Premium
                        showSettingsSheet = false
                    }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(R.string.premium_title1), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.settings_ai_config_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // 1. VIP 状态显示卡片
                val isVip = VipUtils.isUserVip(context)

                // 🟢 动态计算剩余免费次数
                val maxFreeQuota = 5
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val lastUsageDate = prefs.getString("last_ai_usage_date", "")
                // 如果最后使用日期是今天，就读取真实使用量，否则就是 0
                val currentUsage = if (todayStr == lastUsageDate) prefs.getInt("daily_ai_usage", 0) else 0
                val remainingQuota = maxOf(0, maxFreeQuota - currentUsage)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVip) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = if (isVip) Color(0xFFFFB300) else Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isVip) stringResource(R.string.vip_activated) else stringResource(R.string.vip_free_plan),
                                fontWeight = FontWeight.Bold
                            )
                            // 🟢 如果是 VIP，显示无限；如果不是，显示剩余次数
                            Text(
                                text = if (isVip) stringResource(R.string.vip_unlimited) else stringResource(R.string.vip_remaining_quota, remainingQuota, maxFreeQuota),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVip) Color.Gray else MaterialTheme.colorScheme.primary // 非VIP时颜色加深，吸引注意力
                            )
                        }
                    }
                }

                // 2. 自定义 API Key 和 Model 输入 (加入密码掩码)
                var customApiKeyInput by remember { mutableStateOf(prefs.getString("custom_api_key", "") ?: "") }
                var customModelInput by remember { mutableStateOf(prefs.getString("custom_model_name", "") ?: "") }
                var isApiKeyVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = customApiKeyInput,
                    onValueChange = { customApiKeyInput = it },
                    label = { Text(stringResource(R.string.settings_custom_api_key)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (isApiKeyVisible) stringResource(R.string.desc_hide_key) else stringResource(R.string.desc_show_key)
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = customModelInput,
                    onValueChange = { customModelInput = it },
                    label = { Text(stringResource(R.string.settings_custom_model)) },
                    placeholder = { Text(stringResource(R.string.settings_custom_model_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = stringResource(R.string.settings_ai_leave_blank),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // 3. 明确的保存按钮
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("custom_api_key", customApiKeyInput.trim())
                            .putString("custom_model_name", customModelInput.trim())
                            .apply()

                        // 注意：onClick 里面不能直接用 stringResource，必须用 context.getString
                        Toast.makeText(context, context.getString(R.string.toast_ai_config_saved), Toast.LENGTH_SHORT).show()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.btn_save_config))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = stringResource(R.string.cloud_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.driveServiceHelper == null) {
                    // 按钮：连接 Google Drive
                    Button(
                        onClick = {
                            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                                .build()
                            val client = GoogleSignIn.getClient(context, signInOptions)
                            googleSignInLauncher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.AccountCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cloud_connect_btn)) // "Connect Google Drive"
                    }
                } else {
                    // 已连接状态卡片
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                // 状态：已连接 (自动同步中)
                                Text(
                                    text = stringResource(R.string.cloud_connected_msg),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 备份按钮
                                OutlinedButton(
                                    onClick = { viewModel.performCloudBackup(context, showToast = true) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowUpward, null)
                                        Text(stringResource(R.string.cloud_backup_short), fontSize = 10.sp) // "Backup"
                                    }
                                }
                                // 恢复按钮
                                Button(
                                    onClick = { viewModel.performCloudRestore(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ArrowDownward, null)
                                        Text(stringResource(R.string.cloud_restore_short), fontSize = 10.sp) // "Restore"
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Features", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_subscriptions)) },
                    leadingContent = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier.clickable {
                        currentScreen = AppScreen.Subscriptions
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_memory)) },
                    leadingContent = { Icon(Icons.Default.Psychology, null) },
                    modifier = Modifier.clickable {
                        currentScreen = AppScreen.Learning
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_ignored)) },
                    leadingContent = { Icon(Icons.Default.VisibilityOff, null) },
                    modifier = Modifier.clickable {
                        currentScreen = AppScreen.Logs
                        showSettingsSheet = false
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Data & About", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_export)) },
                    leadingContent = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier.clickable {
                        exportCsvLauncher.launch("Expenses.csv")
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup)) },
                    leadingContent = { Icon(Icons.Default.Save, null) },
                    modifier = Modifier.clickable {
                        backupLauncher.launch("Backup.json")
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_restore) + " (Local)") },
                    leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                    modifier = Modifier.clickable {
                        restoreLauncher.launch(arrayOf("application/json"))
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_fix)) },
                    leadingContent = { Icon(Icons.Default.Notifications, null) },
                    modifier = Modifier.clickable {
                        try { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); Toast.makeText(context, "Setting...", Toast.LENGTH_LONG).show() } catch (e: Exception) { }
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.clickable {
                        currentScreen = AppScreen.About
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_feedback)) },
                    leadingContent = { Icon(Icons.Default.BugReport, null) },
                    modifier = Modifier.clickable {
                        currentScreen = AppScreen.Feedback
                        showSettingsSheet = false
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.update)) },
                    leadingContent = { Icon(Icons.Default.SystemUpdate, null) },
                    modifier = Modifier.clickable {
                        viewModel.checkForUpdates(context, showToastIfLatest = true)
                        showSettingsSheet = false
                    }
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.settings_about)) },
            text = {
                Column {
                    Text("AI Expense Tracker", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Developed by 白开水")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("A smart expense tracker powered by Gemini AI.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Privacy Policy:", fontWeight = FontWeight.Bold)
                    Text("Data is stored locally on your device. Cloud backup is optional via Google Drive.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (suggestionToEdit != null) {
        var editAmount by remember { mutableStateOf(suggestionToEdit!!.expense.amount.toString()) }
        var editDate by remember { mutableStateOf(suggestionToEdit!!.predictedBillingDate.toString()) }

        AlertDialog(
            onDismissRequest = { suggestionToEdit = null },
            title = { Text("Confirm Subscription / 确认订阅信息") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Merchant: ${suggestionToEdit!!.expense.merchant}", fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Monthly Amount (RM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { if (it.toIntOrNull() in 1..31 || it.isEmpty()) editDate = it },
                        label = { Text("Billing Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalAmt = editAmount.toDoubleOrNull() ?: suggestionToEdit!!.expense.amount
                    val finalDay = editDate.toIntOrNull() ?: suggestionToEdit!!.predictedBillingDate
                    viewModel.confirmSubscription(suggestionToEdit!!, finalDay, finalAmt)
                    suggestionToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { suggestionToEdit = null }) { Text("Cancel") }
            }
        )
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPaywall() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.paywall_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.paywall_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissPaywall()
                    // 🟢 修改：引导他们去尊贵的 VIP 升级页面！
                    currentScreen = AppScreen.Premium
                }) {
                    Text(stringResource(R.string.premium_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPaywall() }) {
                    Text(stringResource(R.string.paywall_btn_cancel))
                }
            }
        )
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            modifier = Modifier.padding(16.dp),
            title = {
                Column {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.update_available),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.update_version, updateInfo!!.versionName),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        stringResource(R.string.update_whats_new),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )

                    // 🟢 增强型 Changelog 显示区域
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .heightIn(max = 200.dp) // 防止内容太长撑爆屏幕
                            .verticalScroll(rememberScrollState())
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = updateInfo!!.changelog, // 这里你在 Firebase 填：[EN] Fix bugs \n [ZH] 修复错误
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 🚀 核心改变：不再跳浏览器，而是后台静默下载
                        viewModel.downloadUpdate(context, updateInfo!!.downloadUrl, updateInfo!!.versionName)
                        viewModel.dismissUpdateDialog()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text(stringResource(R.string.update_later), color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// 🟢 重点：新增了 onNavigateToTransactions 回调，用于通知父组件切换底部 Tab
fun StatsScreen(viewModel: HomeViewModel, onNavigateToTransactions: () -> Unit) {
    val context = LocalContext.current

    val stats by viewModel.expenseStats.collectAsState(initial = emptyList())
    val currentType by viewModel.statsType.collectAsState()
    val totalAmount = remember(stats) { stats.sumOf { it.totalAmount } }
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val report by viewModel.monthlyReport.collectAsState()
    val isGenerating by viewModel.isGeneratingReport.collectAsState()

    val expenseColors = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726), Color(0xFFAB47BC), Color(0xFF26C6DA), Color(0xFFFF7043), Color(0xFF8D6E63))
    val incomeColors = listOf(Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFF26A69A), Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFF4DD0E1), Color(0xFF009688))
    val currentColors = if (currentType == "EXPENSE") expenseColors else incomeColors

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 🟢 1. AI 财务顾问 (现在置顶了！)
        if (currentType == "EXPENSE") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.stats_ai_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.generateMonthlyReport(context) }, enabled = !isGenerating) {
                                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Refresh, contentDescription = "Analyze")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (report != null) {
                            Text(text = report!!.summaryText, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                            Spacer(modifier = Modifier.height(8.dp))
                            val personaLabel = when(report!!.persona) {
                                "STRICT" -> stringResource(R.string.persona_toxic)
                                "GENTLE" -> stringResource(R.string.persona_gentle)
                                else -> stringResource(R.string.persona_neutral)
                            }
                            Text(text = "${stringResource(R.string.settings_persona)}: $personaLabel", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        } else {
                            Text(text = stringResource(R.string.stats_hint), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 2. 支出/收入 切换按钮 (排在第二位)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = currentType == "EXPENSE", onClick = { viewModel.setStatsType("EXPENSE") }, label = { Text(stringResource(R.string.type_expense)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer))
                Spacer(modifier = Modifier.width(16.dp))
                FilterChip(selected = currentType == "INCOME", onClick = { viewModel.setStatsType("INCOME") }, label = { Text(stringResource(R.string.type_income)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
            }
        }

        // 3. 趋势图卡片
//        if (currentType == "EXPENSE" && monthlyTrend.isNotEmpty()) {
//            item {
//                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(16.dp)) {
//                    TrendChart(data = monthlyTrend)
//                }
//            }
//        }

        // 4. 饼图区域 (Donut Chart)
        if (stats.isEmpty()) {
            item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data_month), color = Color.Gray) } }
        } else {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                    DonutChart(stats = stats.map { it.percentage }, colors = currentColors)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (currentType == "EXPENSE") stringResource(R.string.total_expense) else stringResource(R.string.total_income), fontSize = 14.sp, color = Color.Gray)
                        Text(text = "RM ${String.format("%.2f", totalAmount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 🟢 5. 分类列表 (加上了点击过滤并跳转的逻辑！)
            items(stats.size) { index ->
                val stat = stats[index]
                val color = currentColors[index % currentColors.size]

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    // 核心交互逻辑在这里：
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setCategoryFilter(stat.category) // 告诉 ViewModel 记住这个分类
                        onNavigateToTransactions() // 告诉 HomeScreen 切换回流水页
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stat.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${String.format("%.1f", stat.percentage * 100)}%", fontSize = 14.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { stat.percentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = Color.LightGray.copy(alpha = 0.2f))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "RM ${String.format("%.0f", stat.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChart(
    data: List<com.example.aiexpensetracker.viewmodel.MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "6-Month Spending Trend",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val width = size.width
            val height = size.height
            // 防止除以 0
            val spacing = if (data.size > 1) width / (data.size - 1) else width

            val maxAmount = data.maxOfOrNull { it.totalAmount } ?: 1f
            // 留出底部 30px 写字，顶部留白 20%
            val availableHeight = height - 40f
            val yScale = if (maxAmount == 0f) 1f else availableHeight / maxAmount

            val strokePath = Path()
            val fillPath = Path()

            data.forEachIndexed { index, trend ->
                val x = index * spacing
                // Y轴坐标：数值越大越靠上
                val y = height - 30f - (trend.totalAmount * yScale)

                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    // 使用贝塞尔曲线平滑连接
                    val prevX = (index - 1) * spacing
                    val prevTrend = data[index - 1]
                    val prevY = height - 30f - (prevTrend.totalAmount * yScale)

                    val controlX1 = prevX + (x - prevX) / 2
                    val controlX2 = prevX + (x - prevX) / 2

                    strokePath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                }

                // 画月份文字
                drawText(
                    textMeasurer = textMeasurer,
                    text = trend.monthLabel,
                    topLeft = Offset(x - 20f, height - 20f),
                    style = TextStyle(color = labelColor, fontSize = 10.sp)
                )

                // 画数据点
                drawCircle(
                    color = pointColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // 闭合填充区域
            fillPath.lineTo(width, height)
            fillPath.close()

            // 绘制渐变
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )

            // 绘制折线
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

// 🟢 1. 数据类增加 isChecked 属性
data class BreakdownItem(
    val name: String,
    val price: String,
    val isChecked: Boolean = true,
    val qty: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddExpenseDialog(
    expenseToEdit: ExpenseEntity? = null,
    scannedReceipt: ReceiptAnalysis? = null,
    scannedImageUri: String? = null,
    accountList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, Long, String, String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE) }

    data class TaxOption(val name: String, val rate: Double, val isCustom: Boolean = false)
    val defaultTaxes = listOf(TaxOption("SST 6%", 0.06), TaxOption("SST 8%", 0.08), TaxOption("Svc 10%", 0.10))
    val savedCustomTaxes = remember { prefs.getStringSet("custom_taxes", emptySet())?.map { val parts = it.split("|"); if (parts.size == 2) TaxOption(parts[0], parts[1].toDouble(), true) else null }?.filterNotNull() ?: emptyList() }
    val allAvailableTaxes = remember(savedCustomTaxes) { (defaultTaxes + savedCustomTaxes).toMutableStateList() }
    val selectedTaxes = remember { mutableStateListOf<TaxOption>() }
    var showAddTaxDialog by remember { mutableStateOf(false) }
    var taxToDelete by remember { mutableStateOf<TaxOption?>(null) }
    var isRoundingEnabled by remember { mutableStateOf(false) }

    var amountStr by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: scannedReceipt?.totalAmount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(expenseToEdit?.merchant ?: scannedReceipt?.merchant ?: "") }
    var selectedType by remember { mutableStateOf(expenseToEdit?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: "") }
    var currentImagePath by remember { mutableStateOf(expenseToEdit?.imagePath ?: scannedImageUri) }
    var showFullImage by remember { mutableStateOf(false) }

    val safeAccountList = if (accountList.isEmpty()) listOf("Cash") else accountList
    var selectedAccount by remember { mutableStateOf(expenseToEdit?.accountName ?: safeAccountList[0]) }
    var selectedTargetAccount by remember { mutableStateOf(expenseToEdit?.targetAccountName ?: safeAccountList.getOrElse(1) { safeAccountList[0] }) }

    val initialTime = expenseToEdit?.timestamp ?: scannedReceipt?.date ?: System.currentTimeMillis()
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = initialTime } }
    var displayDateMillis by remember { mutableStateOf(initialTime) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) currentImagePath = uri.toString()
    }

    // --- 明细逻辑 ---
    val breakdownItems = remember { mutableStateListOf<BreakdownItem>() }
    var showDetails by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") } // 这里的 note 仅显示给用户

    LaunchedEffect(expenseToEdit) {
        expenseToEdit?.let {
            amountStr = it.amount.toString()
            merchant = it.merchant
            selectedType = it.type
            selectedCategory = it.category
            selectedAccount = it.accountName
            displayDateMillis = it.timestamp
            currentImagePath = it.imagePath
            if (it.type == "TRANSFER") {
                selectedTargetAccount = it.targetAccountName ?: (accountList.getOrNull(1) ?: accountList.getOrNull(0) ?: "Cash")
            }

            // 解析 Note 中的 Meta 信息 (Tax & Rounding)
            val fullNote = it.note
            val metaRegex = Regex("""\[Meta\](.*)""")
            val metaMatch = metaRegex.find(fullNote)

            if (metaMatch != null) {
                val metaContent = metaMatch.groupValues[1]
                val parts = metaContent.split(";")
                parts.forEach { part ->
                    if (part.startsWith("Taxes:")) {
                        val taxesStr = part.removePrefix("Taxes:")
                        if (taxesStr.isNotBlank()) {
                            val taxList = taxesStr.split(",")
                            taxList.forEach { t ->
                                val tParts = t.split("|")
                                if (tParts.size == 2) {
                                    val tName = tParts[0]
                                    val tRate = tParts[1].toDoubleOrNull() ?: 0.0
                                    val existing = allAvailableTaxes.find { at -> at.name == tName && abs(at.rate - tRate) < 0.001 }
                                    if (existing != null) {
                                        if (!selectedTaxes.contains(existing)) selectedTaxes.add(existing)
                                    } else {
                                        val tempTax = TaxOption(tName, tRate, true)
                                        if (!allAvailableTaxes.contains(tempTax)) allAvailableTaxes.add(tempTax)
                                        selectedTaxes.add(tempTax)
                                    }
                                }
                            }
                        }
                    } else if (part.startsWith("Rounding:")) {
                        isRoundingEnabled = part.removePrefix("Rounding:").toBoolean()
                    }
                }
            }

            // 解析明细 (支持带数量和不带数量的旧格式)
            var cleanNote = fullNote.replace(metaRegex, "").trim()
            if (cleanNote.contains("[Details]")) {
                showDetails = true
                note = cleanNote.substringBefore("[Details]").trim()
                val detailsPart = cleanNote.substringAfter("[Details]")
                val lines = detailsPart.split("\n")

                // 🟢 正则 1: 匹配新格式 "3x ItemName: 15.00"
                val qtyRegex = Regex("""-\s*(\d+)x\s+(.+?):\s*(\d+(\.\d{1,2})?)""")
                // 🟢 正则 2: 匹配旧格式 "ItemName: 15.00"
                val oldRegex = Regex("""-\s*(.+?):\s*(\d+(\.\d{1,2})?)""")

                lines.forEach { line ->
                    val qtyMatch = qtyRegex.find(line.trim())
                    if (qtyMatch != null) {
                        val (qtyStr, name, price) = qtyMatch.destructured
                        breakdownItems.add(BreakdownItem(name, price, true, qtyStr.toIntOrNull() ?: 1))
                    } else {
                        val oldMatch = oldRegex.find(line.trim())
                        if (oldMatch != null) {
                            val (name, price) = oldMatch.destructured
                            breakdownItems.add(BreakdownItem(name, price, true, 1))
                        }
                    }
                }
            } else {
                note = cleanNote
            }
        }
    }

    // --- 🟢 2. 初始化逻辑 (防止 breakdownItems 为空时状态错误) ---
    LaunchedEffect(Unit) {
        // 如果是新建记账且有 Details 标记（这通常不会发生，除非是从外部文本粘贴，这里保留作为防守性代码）
        if (breakdownItems.isEmpty() && note.contains("[Details]")) {
            // 逻辑同上，解析 text 填充 items (略)
        }
    }


    LaunchedEffect(scannedReceipt) {
        if (scannedReceipt != null && expenseToEdit == null) {
            amountStr = String.format("%.2f", scannedReceipt.totalAmount)

            if (scannedReceipt.items.isNotEmpty()) {
                showDetails = true
                breakdownItems.clear()
                scannedReceipt.items.forEach {
                    // 🟢 读取 AI 识别到的 qty
                    breakdownItems.add(BreakdownItem(it.name, it.price.toString(), true, it.qty))
                }
            }

            selectedTaxes.clear()
            scannedReceipt.detectedRates.forEach { detectedRate ->
                val match = allAvailableTaxes.find { abs(it.rate - detectedRate) < 0.001 }
                if (match != null && !selectedTaxes.contains(match)) {
                    selectedTaxes.add(match)
                }
            }

            isRoundingEnabled = scannedReceipt.hasRounding

            // 🟢 智能账户匹配
            val detectedRaw = scannedReceipt.paymentMethod
            val detected = detectedRaw.lowercase().replace(" ", "")

            if (detectedRaw != "Unknown") {
                Toast.makeText(context, context.getString(R.string.toast_ai_pay_detected, detectedRaw), Toast.LENGTH_SHORT).show()
            }

            if (detected.isNotBlank() && detected != "unknown") {
                val keywords = mapOf(
                    // TNG / eWallet
                    "touch" to listOf("tng", "touch", "一触即通"),
                    "tng" to listOf("tng", "touch"),
                    "ewallet" to listOf("tng", "touch", "grab", "pay", "电子钱包", "ewallet"),
                    "电子钱包" to listOf("tng", "touch", "grab", "pay", "ewallet"),
                    // Grab
                    "grab" to listOf("grab"),
                    // Banks (银行)
                    "maybank" to listOf("maybank", "mae", "m2u"),
                    "mae" to listOf("maybank", "mae"),
                    "cimb" to listOf("cimb"),
                    "public" to listOf("public", "pbe", "大众"),
                    "hong" to listOf("hong", "hlb", "丰隆"),
                    "rhb" to listOf("rhb"),
                    "bank" to listOf("bank", "银行"),
                    // QR / DuitNow
                    "qr" to listOf("qr", "duitnow", "tng", "mae", "scan"),
                    "duitnow" to listOf("duitnow", "qr", "tng", "mae", "maybank"),
                    // Cards (卡)
                    "visa" to listOf("card", "credit", "debit", "visa", "bank", "卡"),
                    "master" to listOf("card", "credit", "debit", "master", "bank"),
                    "credit" to listOf("credit", "card", "信用"),
                    "卡" to listOf("card", "credit", "debit"),
                    // Cash (现金)
                    "cash" to listOf("cash", "tunai", "money", "现金", "钱"),
                    "tunai" to listOf("cash", "tunai", "money"),
                    "现金" to listOf("cash", "tunai", "money", "现金"),
                    // Chinese Apps
                    "微信" to listOf("wechat", "微信", "ewallet"),
                    "alipay" to listOf("alipay", "支付宝", "ewallet")
                )

                val bestMatch = safeAccountList.find { userAcc ->
                    val acc = userAcc.lowercase().replace(" ", "")
                    val directMatch = acc.contains(detected) || detected.contains(acc)
                    var dictMatch = false
                    for ((aiKey, targetKeys) in keywords) {
                        if (detected.contains(aiKey)) {
                            if (targetKeys.any { acc.contains(it) }) { dictMatch = true; break }
                        }
                    }
                    directMatch || dictMatch
                }

                if (bestMatch != null) selectedAccount = bestMatch
                else if (detected.contains("cash")) safeAccountList.find { it.equals("Cash", ignoreCase = true) }?.let { selectedAccount = it }
            }
        }
    }

    fun calculateTotalFromItems(): Double {
        return breakdownItems.filter { it.isChecked }.sumOf { it.price.toDoubleOrNull() ?: 0.0 }
    }

    val totalTaxRate = if (selectedType == "EXPENSE") selectedTaxes.sumOf { it.rate } else 0.0
    fun getTaxForAmount(amt: Double): Double = amt * totalTaxRate
    fun roundTo5Sen(value: Double): Double = (value * 20.0).roundToInt() / 20.0

    LaunchedEffect(selectedTaxes.size, breakdownItems.toList(), isRoundingEnabled) {
        val subtotal = calculateTotalFromItems()
        if (subtotal > 0 && showDetails) {
            var finalTotal = subtotal * (1 + totalTaxRate)
            if (isRoundingEnabled) finalTotal = roundTo5Sen(finalTotal)
            amountStr = String.format("%.2f", finalTotal)
        }
    }

    fun parseNoteToItems() {
        if (note.isBlank()) return

        // 🟢 升级正则：支持 "3x Apple 10" 或 "Apple 10"
        // Group 1: 数量 (可选)
        // Group 2: 名字
        // Group 3: 价格
        val regex = Regex("""(?:(\d+)[x*]\s*)?(.+?)[\s-:=]+(\d+(\.\d{1,2})?)""")

        val lines = note.split("\n", ",", "，")
        var newNoteText = note
        var foundCount = 0

        lines.forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                // 解构正则结果
                val qtyStr = match.groupValues[1] // 数量部分
                val name = match.groupValues[2]   // 名字部分
                val price = match.groupValues[3]  // 价格部分

                val qty = qtyStr.toIntOrNull() ?: 1 // 如果没写数量，默认为 1

                breakdownItems.add(BreakdownItem(name.trim(), price, true, qty))

                // 从备注中移除已识别的行
                newNoteText = newNoteText.replace(match.value, "").replace(line.trim(), "")
                foundCount++
            }
        }

        if (foundCount > 0) {
            showDetails = true
            note = newNoteText.replace(Regex("[,，\n]+"), " ").trim()
            val grandTotal = calculateTotalFromItems() // 这里的计算会自动包含 quantity
            if (grandTotal > 0) {
                var finalTotal = grandTotal * (1 + totalTaxRate)
                if (isRoundingEnabled) finalTotal = roundTo5Sen(finalTotal)
                amountStr = String.format("%.2f", finalTotal)
            }
            Toast.makeText(context, context.getString(R.string.toast_parse_success, foundCount), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.toast_parse_fail), Toast.LENGTH_SHORT).show()
        }
    }

    fun updateItemQty(index: Int, newQty: Int) {
        if (newQty < 1) return
        val item = breakdownItems[index]
        // 计算单价 (当前总价 / 当前数量)
        val currentTotal = item.price.toDoubleOrNull() ?: 0.0
        val unitPrice = if (item.qty > 0) currentTotal / item.qty else 0.0
        // 计算新总价
        val newTotal = unitPrice * newQty
        breakdownItems[index] = item.copy(qty = newQty, price = String.format("%.2f", newTotal))
    }

    // 🟢 核心修改 1：提取默认分类并过滤黑名单
    val defaultExpenseCategories = listOf(stringResource(R.string.cat_food), stringResource(R.string.cat_transport), stringResource(R.string.cat_shopping), stringResource(R.string.cat_entertainment), stringResource(R.string.cat_home), stringResource(R.string.cat_medical), stringResource(R.string.cat_other))
    val defaultIncomeCategories = listOf(stringResource(R.string.cat_salary), stringResource(R.string.cat_bonus), stringResource(R.string.cat_allowance), stringResource(R.string.cat_investment), stringResource(R.string.cat_red_packet), stringResource(R.string.cat_other))

    val currentPrefKey = if (selectedType == "EXPENSE") "custom_cat_expense" else "custom_cat_income"
    val deletedDefaultKey = if (selectedType == "EXPENSE") "deleted_default_exp" else "deleted_default_inc"

    // 读取被删除的默认分类和新增的自定义分类
    val deletedDefaults = remember(selectedType, categoryToDelete) { prefs.getStringSet(deletedDefaultKey, emptySet()) ?: emptySet() }
    val savedCustomCategories = remember(selectedType, showNewCategoryDialog, categoryToDelete) { prefs.getStringSet(currentPrefKey, emptySet())?.toList() ?: emptyList() }

    // 组合成最终展示的列表
    val currentDefaultList = (if (selectedType == "EXPENSE") defaultExpenseCategories else defaultIncomeCategories).filter { it !in deletedDefaults }
    val categoryList = remember(selectedType, savedCustomCategories, currentDefaultList) { (currentDefaultList + savedCustomCategories).toMutableStateList() }

    LaunchedEffect(selectedType) { if (selectedType != "TRANSFER" && categoryList.isNotEmpty() && selectedCategory !in categoryList) selectedCategory = categoryList.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expenseToEdit != null) stringResource(R.string.dialog_edit_title) else stringResource(R.string.dialog_add_title)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 600.dp)) {

                // --- Type, Amount, Tax, Merchant, Account, Category, Image (保持不变) ---
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        FilterChip(selected = selectedType == "EXPENSE", onClick = { selectedType = "EXPENSE"; selectedTaxes.clear() }, label = { Text(stringResource(R.string.type_exp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)); Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = selectedType == "INCOME", onClick = { selectedType = "INCOME"; selectedTaxes.clear() }, label = { Text(stringResource(R.string.type_inc)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9))); Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = selectedType == "TRANSFER", onClick = { selectedType = "TRANSFER" }, label = { Text(stringResource(R.string.type_trans)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                        label = { Text(stringResource(R.string.hint_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium
                    )
                }

                if (selectedType == "EXPENSE") {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isRoundingEnabled = !isRoundingEnabled }) {
                            Checkbox(checked = isRoundingEnabled, onCheckedChange = { isRoundingEnabled = it })
                            Text(stringResource(R.string.label_rounding), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 税费选项
                if (selectedType == "EXPENSE") {
                    item {
                        Column {
                            Text(stringResource(R.string.label_add_tax_header), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                items(allAvailableTaxes) { tax ->
                                    val isSelected = selectedTaxes.contains(tax)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .combinedClickable(
                                                onClick = { if (isSelected) selectedTaxes.remove(tax) else selectedTaxes.add(tax) },
                                                onLongClick = {
                                                    if (tax.isCustom) taxToDelete = tax else Toast.makeText(context, context.getString(R.string.error_cannot_delete_default), Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(tax.name, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                                item {
                                    IconButton(onClick = { showAddTaxDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (selectedType == "TRANSFER") {
                        Text(stringResource(R.string.label_from), style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc -> FilterChip(selected = selectedAccount == acc, onClick = { selectedAccount = acc }, label = { Text(if (acc == "Cash") stringResource(R.string.text_cash) else acc) }) } }
                        Text(stringResource(R.string.label_to), style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc -> FilterChip(selected = selectedTargetAccount == acc, onClick = { selectedTargetAccount = acc }, label = { Text(if (acc == "Cash") stringResource(R.string.text_cash) else acc) }) } }
                        merchant = "Transfer to $selectedTargetAccount"
                    } else {
                        OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text(stringResource(R.string.label_merchant)) }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.label_account), style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc -> FilterChip(selected = selectedAccount == acc, onClick = { selectedAccount = acc }, label = { Text(if (acc == "Cash") stringResource(R.string.text_cash) else acc) }) } }
                        Text(stringResource(R.string.label_category), style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            items(categoryList) { category ->
                                val isSelected = selectedCategory == category
                                Box(contentAlignment = Alignment.Center) {
                                    FilterChip(selected = isSelected, onClick = {}, label = { Text(category) }, shape = RoundedCornerShape(8.dp))
                                    // 🟢 核心修改 2：去掉了 if(isCustom) 的限制，所有分类都允许长按删除！
                                    Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(8.dp)).combinedClickable(onClick = { selectedCategory = category }, onLongClick = { categoryToDelete = category }))
                                }
                            }
                            item { IconButton(onClick = { showNewCategoryDialog = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) } }
                        }
                    }
                }

                item {
                    if (currentImagePath != null) {
                        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { showFullImage = true }, contentAlignment = Alignment.Center) {
                            AsyncImage(model = currentImagePath, contentDescription = "Receipt", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            IconButton(onClick = { currentImagePath = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.6f), CircleShape).size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp)) }
                        }
                    } else {
                        OutlinedButton(onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) { Icon(Icons.Default.AttachFile, null); Text(stringResource(R.string.btn_attach_receipt)) }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.hint_note)) }, modifier = Modifier.weight(1f), placeholder = { Text(stringResource(R.string.hint_note_example)) })
                        IconButton(onClick = { parseNoteToItems() }) { Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary) }
                    }
                }

                // 明细开关
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showDetails = !showDetails }) {
                        Checkbox(checked = showDetails, onCheckedChange = { showDetails = it })
                        Text(stringResource(R.string.label_add_breakdown), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        val sub = calculateTotalFromItems()
                        if (sub > 0) Text(
                            text = stringResource(R.string.text_subtotal, String.format("%.2f", sub)),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 🟢 4. 明细列表 (重构版 UI)
                if (showDetails) {
                    items(breakdownItems.size) { index ->
                        val item = breakdownItems[index]
                        val itemPriceVal = item.price.toDoubleOrNull() ?: 0.0
                        val taxAmt = getTaxForAmount(itemPriceVal)
                        val itemTotal = itemPriceVal + taxAmt

                        // 卡片布局
                        Card(
                            colors = CardDefaults.cardColors(
                                // 使用 surfaceVariant 作为背景，自动适配深浅色
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                // 第一行：Check | Qty(- 1 +) | Tax | Price
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { breakdownItems[index] = item.copy(isChecked = it) },
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    // 数量控制器
                                    // 🟢 修复背景：使用 surface 颜色，确保深色模式下是深灰色底
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .height(32.dp)
                                            .padding(horizontal = 4.dp) // 微调内边距
                                    ) {
                                        IconButton(onClick = { updateItemQty(index, item.qty - 1) }, modifier = Modifier.width(24.dp)) {
                                            Icon(Icons.Default.Remove, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        }

                                        // 🟢 修复1：数量输入框颜色 (onSurface) + 光标颜色
                                        BasicTextField(
                                            value = item.qty.toString(),
                                            onValueChange = { str: String -> str.toIntOrNull()?.let { updateItemQty(index, it) } },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface, // 关键：适配深色模式文字
                                                textAlign = TextAlign.Center,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary), // 光标颜色
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(30.dp)
                                        )

                                        IconButton(onClick = { updateItemQty(index, item.qty + 1) }, modifier = Modifier.width(24.dp)) {
                                            Icon(Icons.Default.Add, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))

                                    // 税 & 价格
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (taxAmt > 0) {
                                            Text(stringResource(R.string.text_tax_plus, String.format("%.2f", taxAmt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // 🟢 修复2：RM 标签颜色 (onSurfaceVariant)
                                            Text("RM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))

                                            // 🟢 修复3：价格输入框颜色
                                            BasicTextField(
                                                value = item.price,
                                                onValueChange = { newPrice -> breakdownItems[index] = item.copy(price = newPrice) },
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    color = MaterialTheme.colorScheme.onSurface, // 关键：适配深色模式文字
                                                    textAlign = TextAlign.End,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.width(70.dp)
                                            )
                                        }
                                        if (taxAmt > 0) {
                                            // 🟢 修复4：总价文字颜色 (onSurface)
                                            Text("= ${String.format("%.2f", itemTotal)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))

                                // 第二行：名字 + 删除
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { breakdownItems[index] = item.copy(name = it) },
                                        placeholder = { Text(stringResource(R.string.hint_item_name))},
                                        modifier = Modifier.weight(1f),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                        maxLines = 3
                                    )
                                    IconButton(onClick = { breakdownItems.removeAt(index) }) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        TextButton(onClick = { breakdownItems.add(BreakdownItem("", "", true, 1)) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.btn_add_item))
                        }
                    }
                }

                // 日期
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) { Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary); Text(SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(displayDateMillis)), style = MaterialTheme.typography.bodySmall) } }
                        OutlinedCard(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) { Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary); Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(displayDateMillis)), style = MaterialTheme.typography.bodySmall) } }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull()
                if (amt != null) {
                    val finalCategory = if (selectedType == "TRANSFER") "Transfer" else selectedCategory
                    val finalTarget = if (selectedType == "TRANSFER") selectedTargetAccount else null

                    // 🟢 5. 保存逻辑 (带数量)
                    var detailsString = ""
                    if (showDetails && breakdownItems.isNotEmpty()) {
                        val checkedItems = breakdownItems.filter { it.isChecked }
                        if (checkedItems.isNotEmpty()) {
                            detailsString = checkedItems.joinToString("\n") {
                                val p = it.price.toDoubleOrNull() ?: 0.0
                                val t = getTaxForAmount(p)
                                "- ${it.qty}x ${it.name}: ${it.price} (+${String.format("%.2f", t)} Tax)"
                            }
                        }
                    }

                    var metaString = ""
                    if (selectedTaxes.isNotEmpty() || isRoundingEnabled) {
                        val taxData = selectedTaxes.joinToString(",") { "${it.name}|${it.rate}" }
                        metaString = "[Meta]Taxes:$taxData;Rounding:$isRoundingEnabled"
                    }

                    var finalNote = note.trim()
                    if (detailsString.isNotBlank()) {
                        finalNote = if (finalNote.isBlank()) "[Details]\n$detailsString" else "$finalNote\n\n[Details]\n$detailsString"
                    }
                    if (metaString.isNotBlank()) {
                        finalNote = "$finalNote\n$metaString"
                    }

                    onConfirm(amt, merchant, finalCategory, selectedType, displayDateMillis, finalNote, selectedAccount, finalTarget, currentImagePath)
                }
            }) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )

    if (showAddTaxDialog) { var newTaxName by remember { mutableStateOf("") }; var newTaxRateStr by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddTaxDialog = false }, title = { Text("Add Custom Tax") }, text = { Column { OutlinedTextField(value = newTaxName, onValueChange = { newTaxName = it }, label = { Text("Name (e.g. Svc)") }); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = newTaxRateStr, onValueChange = { newTaxRateStr = it }, label = { Text("Rate % (e.g. 10)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } }, confirmButton = { Button(onClick = { val r = newTaxRateStr.toDoubleOrNull(); if (newTaxName.isNotBlank() && r != null) { val rateDecimal = r / 100.0; val currentSet = prefs.getStringSet("custom_taxes", emptySet())!!.toMutableSet(); currentSet.add("${newTaxName}|${rateDecimal}"); prefs.edit().putStringSet("custom_taxes", currentSet).apply(); val newTax = TaxOption(newTaxName, rateDecimal, true); allAvailableTaxes.add(newTax); showAddTaxDialog = false } }) { Text("Add") } }, dismissButton = { TextButton(onClick = { showAddTaxDialog = false }) { Text("Cancel") } }) }

    // 🟢 核心修改 3：删除确认弹窗，智能判断是删除了自定义还是默认分类
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { target ->
                            categoryList.remove(target)

                            val isDefault = defaultExpenseCategories.contains(target) || defaultIncomeCategories.contains(target)
                            if (isDefault) {
                                // 把默认分类拉进黑名单
                                val set = prefs.getStringSet(deletedDefaultKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                                set.add(target)
                                prefs.edit().putStringSet(deletedDefaultKey, set).apply()
                            } else {
                                // 删掉自定义分类
                                val set = prefs.getStringSet(currentPrefKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                                set.remove(target)
                                prefs.edit().putStringSet(currentPrefKey, set).apply()
                            }

                            if (selectedCategory == target) selectedCategory = categoryList.firstOrNull() ?: "Other"
                        };
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    if (showDatePicker) { val ds = rememberDatePickerState(initialSelectedDateMillis = displayDateMillis); DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { ds.selectedDateMillis?.let { displayDateMillis = it; calendar.timeInMillis = it }; showDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }) { DatePicker(state = ds) } }
    if (showTimePicker) { val ts = rememberTimePickerState(initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE), is24Hour = true); AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { calendar.set(Calendar.HOUR_OF_DAY, ts.hour); calendar.set(Calendar.MINUTE, ts.minute); displayDateMillis = calendar.timeInMillis; showTimePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }, text = { TimePicker(state = ts) } ) }
    if (showNewCategoryDialog) {
        var newCat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text(stringResource(R.string.dialog_new_cat_title)) },
            text = {
                OutlinedTextField(
                    value = newCat,
                    onValueChange = { newCat = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCat.isNotBlank() && !categoryList.contains(newCat)) {
                        categoryList.add(newCat)
                        val set = prefs.getStringSet(currentPrefKey, emptySet())?.toMutableSet() ?: mutableSetOf()
                        set.add(newCat)
                        prefs.edit().putStringSet(currentPrefKey, set).apply()
                        selectedCategory = newCat
                        showNewCategoryDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (taxToDelete != null) { AlertDialog(onDismissRequest = { taxToDelete = null }, title = { Text("Delete Tax?") }, confirmButton = { TextButton(onClick = { taxToDelete?.let { t -> allAvailableTaxes.remove(t); selectedTaxes.remove(t); val s = prefs.getStringSet("custom_taxes", emptySet())!!.toMutableSet(); s.remove("${t.name}|${t.rate}"); prefs.edit().putStringSet("custom_taxes", s).apply() }; taxToDelete = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { taxToDelete = null }) { Text("Cancel") } }) }
    if (showFullImage && currentImagePath != null) { Dialog(onDismissRequest = { showFullImage = false }) { Box(modifier = Modifier.fillMaxSize().clickable { showFullImage = false }, contentAlignment = Alignment.Center) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.9f))); AsyncImage(model = currentImagePath, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth()); IconButton(onClick = { showFullImage = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White) } } } }
}
@Composable
fun SubscriptionScreen(viewModel: HomeViewModel, onBack: () -> Unit = {}) {
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val totalMonthly = subscriptions.sumOf { it.amount }
    val context = LocalContext.current
    val isZh = Locale.getDefault().language == "zh" // 🟢 提取出来方便复用

    // 控制黑名单弹窗的状态
    var showBlacklistDialog by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

            // 🟢 新增：顶部标题栏 + 返回按钮 + 黑名单入口
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 黑名单入口按钮
                TextButton(onClick = { showBlacklistDialog = true }) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isZh) "已屏蔽" else "Ignored", color = Color.Gray)
                }
            }

            // 统计卡片
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.text_subscription_fixed), style = MaterialTheme.typography.labelMedium) // "订阅 / 固定支出"
                    Text("RM ${String.format("%.2f", totalMonthly)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    val countText = if (isZh) "${subscriptions.size} 个活跃订阅" else "${subscriptions.size} active subscriptions"
                    Text(countText, style = MaterialTheme.typography.bodySmall)
                }
            }

            // 订阅列表
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subscriptions) { sub ->
                    val calendar = Calendar.getInstance()
                    val today = calendar.get(Calendar.DAY_OF_MONTH)
                    var daysLeft = sub.billingDate - today
                    if (daysLeft < 0) daysLeft += 30

                    val daysText = if (daysLeft == 0) {
                        if (isZh) "今天扣款" else "Due Today"
                    } else {
                        if (isZh) "${daysLeft}天后扣款" else "Due in $daysLeft days"
                    }
                    val daysColor = if (daysLeft <= 3) MaterialTheme.colorScheme.error else Color.Gray

                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.merchant, fontWeight = FontWeight.Bold)
                                val dateText = if (isZh) "${sub.accountName} • 每月 ${sub.billingDate} 日" else "${sub.accountName} • Every ${sub.billingDate}th"
                                Text(dateText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("RM ${String.format("%.2f", sub.amount)}", fontWeight = FontWeight.Bold)
                                Text(daysText, style = MaterialTheme.typography.labelSmall, color = daysColor)
                            }
                            IconButton(onClick = { viewModel.deleteSubscription(sub) }) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                        }
                    }
                }
            }
        }
    }

    // 🟢 新增：黑名单管理弹窗
    if (showBlacklistDialog) {
        // 每次打开弹窗时，实时获取最新的黑名单数据
        var blacklisted by remember { mutableStateOf(viewModel.getIgnoredMerchants(context)) }

        AlertDialog(
            onDismissRequest = { showBlacklistDialog = false },
            title = { Text(if (isZh) "已屏蔽的订阅检测" else "Ignored Subscriptions") },
            text = {
                if (blacklisted.isEmpty()) {
                    Text(if (isZh) "暂无被屏蔽的商家。" else "No merchants ignored.", color = Color.Gray)
                } else {
                    LazyColumn {
                        items(blacklisted) { merchant ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(merchant, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    // 1. 从 SharedPreferences 移除
                                    viewModel.removeIgnoredMerchant(context, merchant)
                                    // 2. 刷新界面列表
                                    blacklisted = viewModel.getIgnoredMerchants(context)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlacklistDialog = false }) { Text(if (isZh) "关闭" else "Close") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: HomeViewModel,
    isSearchActive: Boolean,
    isCalendarView: Boolean,
    accountNames: List<String>,
    accountBalances: List<AccountBalance>,
    isBalanceVisible: Boolean,
    subscriptions: List<SubscriptionEntity>
) {
    val context = LocalContext.current
    val expenses by viewModel.filteredExpenses.collectAsState(initial = emptyList())
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    val currentMonthMillis by viewModel.currentMonth.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    val editRequest by viewModel.editRequest.collectAsState()
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()

    // 自动检测订阅
    val latestExpense = expenses.firstOrNull()
    LaunchedEffect(latestExpense) {
        if (latestExpense != null && latestExpense.type == "EXPENSE") {
            viewModel.checkLatestExpenseForSubscription(context,latestExpense)
        }
    }

    // 处理通知点击编辑
    LaunchedEffect(editRequest) {
        editRequest?.let { id ->
            val target = viewModel.getExpenseById(id)
            if (target != null) { expenseToEdit = target; showAddDialog = true }
            viewModel.clearEditRequest()
        }
    }

    // 切换月份/视图时重置选中日期
    LaunchedEffect(currentMonthMillis, isCalendarView) { selectedDay = null }

    // 过滤显示的账单
    val finalExpenses = remember(expenses, selectedDay, isCalendarView, categoryFilter) {
        var filteredList = expenses
        if (isCalendarView && selectedDay != null) {
            filteredList = filteredList.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.DAY_OF_MONTH) == selectedDay
            }
        }
        if (categoryFilter != null) {
            filteredList = filteredList.filter { it.category == categoryFilter }
        }
        filteredList
    }

    // 计算总额
    val displayExpense = remember(finalExpenses) { finalExpenses.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
    val displayIncome = remember(finalExpenses) { finalExpenses.filter { it.type == "INCOME" }.sumOf { it.amount } }

    // 日期格式化
    val isChinese = Locale.getDefault().language == "zh"
    val dateHeaderPattern = if (isChinese) "MM月dd日 (EEEE)" else "MMM dd (EEEE)"
    val groupedExpenses = remember(finalExpenses) { finalExpenses.groupBy { expense -> SimpleDateFormat(dateHeaderPattern, Locale.getDefault()).format(Date(expense.timestamp)) } }

    // 预算数据
    val categorySpendings = remember(expenses) { expenses.filter { it.type == "EXPENSE" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } } }
    val totalSubsCost = remember(subscriptions) { subscriptions.sumOf { it.amount } }
    val allExpenseCategories = listOf(stringResource(R.string.cat_food), stringResource(R.string.cat_transport), stringResource(R.string.cat_shopping), stringResource(R.string.cat_entertainment), stringResource(R.string.cat_home), stringResource(R.string.cat_medical), stringResource(R.string.cat_other))

    // 筛选出“选中日期”的订阅
    val todaySubs = remember(selectedDay, subscriptions) {
        if (selectedDay != null) {
            subscriptions.filter { it.billingDate == selectedDay }
        } else emptyList()
    }

    // Dialog 状态
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }; var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var targetCategoryForBudget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedVisibility(visible = showScrollToTop, enter = fadeIn(), exit = fadeOut()) {
                    SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top") }
                }
                FloatingActionButton(onClick = { expenseToEdit = null; showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) { Icon(Icons.Default.Add, contentDescription = null) }
            }
        }
    ) { innerPadding ->
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {

            if (categoryFilter != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
                        InputChip(
                            selected = true,
                            onClick = { viewModel.setCategoryFilter(null) }, // 点击清除过滤
                            label = { Text("Filtering: $categoryFilter") },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp)) },
                            colors = InputChipDefaults.inputChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                        )
                    }
                }
            }

            // 1. 账户列表
            if (!isSearchActive) {
                item { AccountBalanceList(accountBalances, { viewModel.addNewAccount(it) }, { viewModel.deleteAccount(it) }, { name, bal -> viewModel.adjustAccountBalance(name, bal) }, isBalanceVisible) }
            }

            // 2. 月份选择器
            if (!isSearchActive) {
                item { MonthSelector(currentMonthMillis, { viewModel.prevMonth() }, { viewModel.nextMonth() }) }
            }

            // 3. 收支总结卡片 (汉化 Daily Summary)
            item {
                val summaryTitle = if (isCalendarView && selectedDay != null) stringResource(R.string.text_daily_summary) else ""
                MonthSummaryCard(displayIncome, displayExpense, displayIncome - displayExpense, summaryTitle, isBalanceVisible)
            }

            // 4. 视图切换
            if (isCalendarView) {
                item { CalendarView(currentMonthMillis, expenses, selectedDay, { selectedDay = if (selectedDay == it) null else it }, subscriptions) }
            }

            // 🟢 5. 关键修复：订阅预留 (汉化)
            if (isCalendarView && selectedDay != null && todaySubs.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.text_upcoming), // "📅 预留 / 订阅"
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(todaySubs) { sub ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sub.merchant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                // 🟢 汉化: Subscription / Fixed Cost
                                Text(stringResource(R.string.text_subscription_fixed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                            }
                            Text("Est. RM ${String.format("%.2f", sub.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 6. 账单列表
            if (finalExpenses.isEmpty() && todaySubs.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_data_month), color = Color.Gray) } }
            } else {
                groupedExpenses.forEach { (header, list) ->
                    stickyHeader { DateHeader(header) }
                    items(list) { expense -> ExpenseCard(expense, { expenseToDelete = expense }, { expenseToEdit = expense; showAddDialog = true }, isBalanceVisible) }
                }
            }
        }
    }

    // Dialogs
    if (showTotalBudgetDialog) SetBudgetDialog(stringResource(R.string.dialog_set_total_budget), monthlyBudget, { showTotalBudgetDialog = false }, { viewModel.updateMonthlyBudget(it); showTotalBudgetDialog = false })
    if (targetCategoryForBudget != null) SetBudgetDialog("${stringResource(R.string.dialog_set_cat_budget)} $targetCategoryForBudget", categoryBudgets[targetCategoryForBudget] ?: 0.0, { targetCategoryForBudget = null }, { viewModel.updateCategoryBudget(targetCategoryForBudget!!, it); targetCategoryForBudget = null })
    if (showAddDialog) {
        AddExpenseDialog(
            expenseToEdit = expenseToEdit,
            accountList = accountNames,
            onDismiss = { showAddDialog = false },
            // 🟢 修复：这里增加第 9 个参数 imgPath
            onConfirm = { amt, merch, cat, type, time, note, acc, tgt, imgPath ->
                val id = expenseToEdit?.id ?: 0

                // 构造对象
                val newExpense = ExpenseEntity(
                    id = id,
                    amount = amt,
                    type = type,
                    merchant = merch,
                    category = cat,
                    timestamp = time,
                    originalText = expenseToEdit?.originalText ?: context.getString(R.string.manual_entry),
                    note = note,
                    accountName = acc,
                    targetAccountName = tgt,
                    // 🟢 修复：将图片路径传给数据库实体
                    imagePath = imgPath
                )

                viewModel.addExpense(newExpense)
                showAddDialog = false
            }
        )
    }
    if (expenseToDelete != null) AlertDialog(onDismissRequest = { expenseToDelete = null }, title = { Text(stringResource(R.string.dialog_delete_title)) }, confirmButton = { TextButton(onClick = { viewModel.deleteExpense(expenseToDelete!!); expenseToDelete = null }) { Text(stringResource(R.string.btn_delete)) } }, dismissButton = { TextButton(onClick = { expenseToDelete = null }) { Text(stringResource(R.string.btn_cancel)) } })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountBalanceList(
    balances: List<AccountBalance>,
    onAddAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onAdjustBalance: (String, Double) -> Unit,
    isVisible: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var accountActionSheet by remember { mutableStateOf<AccountBalance?>(null) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(balances) { acc ->
            // 🟢 汉化逻辑：如果是 "Cash"，显示 "现金"
            val displayName = if (acc.name == "Cash") stringResource(R.string.text_cash) else acc.name

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.combinedClickable(
                    onClick = { accountActionSheet = acc },
                    onLongClick = {
                        // 🟢 Cash 账户不可删除
                        if (acc.name != "Cash") accountToDelete = acc.name
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(text = displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val displayBalance = if (isVisible) "RM ${String.format("%.2f", acc.balance)}" else "RM ****"
                    Text(text = displayBalance, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { showAddDialog = true }
            ) {
                Box(modifier = Modifier.padding(12.dp).size(width = 30.dp, height = 36.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // 🟢 1. 账户操作菜单
    if (accountActionSheet != null) {
        val displayName = if (accountActionSheet!!.name == "Cash") stringResource(R.string.text_cash) else accountActionSheet!!.name
        AlertDialog(
            onDismissRequest = { accountActionSheet = null },
            title = { Text(displayName) },
            text = { Text(stringResource(R.string.dialog_account_action)) }, // "账户操作"
            confirmButton = {
                TextButton(onClick = { showAdjustDialog = true }) {
                    Text(stringResource(R.string.dialog_adjust_balance)) // "修正余额"
                }
            },
            dismissButton = {
                if (accountActionSheet?.name != "Cash") {
                    TextButton(
                        onClick = { accountToDelete = accountActionSheet!!.name; accountActionSheet = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.btn_delete))
                    }
                }
            }
        )
    }

    // 🟢 2. 修正余额弹窗
    if (showAdjustDialog && accountActionSheet != null) {
        var newBalanceStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false; accountActionSheet = null },
            title = { Text(stringResource(R.string.dialog_adjust_balance)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.dialog_current_balance, String.format("%.2f", accountActionSheet!!.balance)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBalanceStr,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) newBalanceStr = it },
                        label = { Text(stringResource(R.string.hint_actual_balance)) }, // "实际余额"
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val bal = newBalanceStr.toDoubleOrNull()
                    if (bal != null) {
                        onAdjustBalance(accountActionSheet!!.name, bal)
                        showAdjustDialog = false
                        accountActionSheet = null
                    }
                }) { Text(stringResource(R.string.btn_confirm)) } // "确定"
            }
        )
    }

    // 🟢 3. 添加账户弹窗
    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("${stringResource(R.string.btn_add)} ${stringResource(R.string.label_account)}") }, // "添加 账户"
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(stringResource(R.string.label_account)) }, singleLine = true) },
            confirmButton = {
                Button(onClick = { if(newName.isNotBlank()) { onAddAccount(newName); showAddDialog = false } }) {
                    Text(stringResource(R.string.btn_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // 🟢 4. 删除账户确认
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_account)) }, // "删除账户"
            text = { Text(stringResource(R.string.dialog_delete_account_msg, accountToDelete!!)) }, // "确定删除...?"
            confirmButton = {
                TextButton(
                    onClick = { onDeleteAccount(accountToDelete!!); accountToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseEntity,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
    isVisible: Boolean
) {
    val isIncome = expense.type == "INCOME"
    val isTransfer = expense.type == "TRANSFER"

    // 配色逻辑
    val amountColor = when {
        isIncome -> Color(0xFF4CAF50) // 绿色
        isTransfer -> Color(0xFF2196F3) // 蓝色
        else -> Color(0xFFE53935) // 红色
    }
    val amountPrefix = if (isIncome) "+" else if (isTransfer) "" else "-"

    // 智能处理 Note：只显示 "[Details]" 之前的内容
    val cleanNote = remember(expense.note) {
        if (expense.note.contains("[Details]")) {
            expense.note.substringBefore("[Details]").trim()
        } else {
            expense.note
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick) // 点击卡片进入详情/编辑页
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 左侧信息区 ---
            Column(modifier = Modifier.weight(1f)) {

                // 🟢 1. 商家名称 + 附件图标行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // 🟢 关键技巧：fill = false 让 Text 只占用实际需要的宽度
                        // 如果文字太长，它会收缩并显示省略号，给后面的 Icon 留位置
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 🟢 如果有 imagePath，只显示一个小回形针图标
                    if (expense.imagePath != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Has Receipt",
                            modifier = Modifier.size(16.dp), // 图标做小一点，精致
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }

                // 2. Note (显示简化后的备注)
                if (cleanNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cleanNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Category • Account • Time
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(expense.timestamp))
                val accountInfo = if (isTransfer) "${expense.accountName} ➜ ${expense.targetAccountName}" else expense.accountName

                Text(
                    text = "${expense.category} • $accountInfo • $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }

            // --- 右侧金额区 ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                val displayAmount = if (isVisible) "$amountPrefix RM ${String.format("%.2f", expense.amount)}" else "$amountPrefix ****"
                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                // 删除按钮
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: HomeViewModel) {
    val expenses by viewModel.filteredExpenses.collectAsState(initial = emptyList())
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()

    val totalSpent = remember(expenses) { expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
    val totalSubscriptions = remember(subscriptions) { subscriptions.sumOf { it.amount } }
    val categorySpendings = remember(expenses) {
        expenses.filter { it.type == "EXPENSE" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)

    val defaultExpenseCategories = listOf(
        stringResource(R.string.cat_food), stringResource(R.string.cat_transport),
        stringResource(R.string.cat_shopping), stringResource(R.string.cat_entertainment),
        stringResource(R.string.cat_home), stringResource(R.string.cat_medical), stringResource(R.string.cat_other)
    )
    val deletedDefaults = prefs.getStringSet("deleted_default_exp", emptySet()) ?: emptySet()
    val activeDefaults = defaultExpenseCategories.filter { it !in deletedDefaults }
    val savedCustomCategories = prefs.getStringSet("custom_cat_expense", emptySet())?.toList() ?: emptyList()

    val allCategories = (activeDefaults + savedCustomCategories).distinct()

    var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var targetCategoryForBudget by remember { mutableStateOf<String?>(null) }

    val safeBudget = if (monthlyBudget > 0) monthlyBudget else 1.0
    val spentProgress = (totalSpent / safeBudget).toFloat().coerceAtMost(1f)
    val reservedProgress = ((totalSpent + totalSubscriptions) / safeBudget).toFloat().coerceAtMost(1f)
    val isTotalOver = (totalSpent + totalSubscriptions) > monthlyBudget

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.budget_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showTotalBudgetDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Text(
                            text = "RM ${String.format("%.0f", monthlyBudget)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                            LinearProgressIndicator(
                                progress = { reservedProgress },
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                color = Color.Gray.copy(alpha = 0.4f),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            LinearProgressIndicator(
                                progress = { spentProgress },
                                modifier = Modifier.fillMaxWidth(spentProgress).height(12.dp).clip(RoundedCornerShape(6.dp)),
                                color = if (isTotalOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val reservedText = if (totalSubscriptions > 0) "(+RM ${String.format("%.0f", totalSubscriptions)} reserved)" else ""
                        Text(
                            text = "${stringResource(R.string.budget_used)} RM ${String.format("%.0f", totalSpent)} $reservedText",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isTotalOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.budget_detail_hint),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(allCategories) { category ->
                val spent = categorySpendings[category] ?: 0.0
                val budget = categoryBudgets[category] ?: 0.0
                val hasBudget = budget > 0
                val catProgress = if (hasBudget) (spent / budget).toFloat() else 0f
                val isCatOver = hasBudget && catProgress > 1f

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { targetCategoryForBudget = category },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "RM ${String.format("%.0f", spent)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if(isCatOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (hasBudget) " / ${String.format("%.0f", budget)}" else " / Unlimited",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
                                )
                            }
                        }

                        if (hasBudget) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { catProgress.coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (isCatOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTotalBudgetDialog) {
        SetBudgetDialog(
            title = stringResource(R.string.dialog_set_total_budget),
            init = monthlyBudget,
            dismiss = { showTotalBudgetDialog = false },
            confirm = { viewModel.updateMonthlyBudget(it); showTotalBudgetDialog = false }
        )
    }

    if (targetCategoryForBudget != null) {
        SetBudgetDialog(
            title = "${stringResource(R.string.dialog_set_cat_budget)}: $targetCategoryForBudget",
            init = categoryBudgets[targetCategoryForBudget] ?: 0.0,
            dismiss = { targetCategoryForBudget = null },
            confirm = { viewModel.updateCategoryBudget(targetCategoryForBudget!!, it); targetCategoryForBudget = null },
            // 🟢 用户点击 Remove 时，把该分类预算设为 0 (即 Unlimited)
            onDelete = { viewModel.updateCategoryBudget(targetCategoryForBudget!!, 0.0); targetCategoryForBudget = null }
        )
    }
}

// 🟢 带有 Remove 按钮的全新 Dialog
@Composable
fun SetBudgetDialog(
    title: String,
    init: Double,
    dismiss: () -> Unit,
    confirm: (Double) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var v by remember { mutableStateOf(if (init > 0) String.format("%.0f", init) else "") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = v,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) v = it },
                label = { Text(stringResource(R.string.hint_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { confirm(v.toDoubleOrNull() ?: 0.0) }) { Text(stringResource(R.string.btn_save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Remove")
                    }
                }
                TextButton(onClick = dismiss) { Text(stringResource(R.string.btn_cancel)) }
            }
        }
    )
}

@Composable
fun CalendarView(
    currentMonthMillis: Long,
    expenses: List<ExpenseEntity>,
    selectedDate: Int?,
    onDateSelected: (Int) -> Unit,
    subscriptions: List<SubscriptionEntity>
) {
    val calendar = remember(currentMonthMillis) { Calendar.getInstance().apply { timeInMillis = currentMonthMillis; set(Calendar.DAY_OF_MONTH, 1) } }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val offset = firstDayOfWeek - 1

    val todayCal = Calendar.getInstance()
    val currentRealDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val currentRealMonth = todayCal.get(Calendar.MONTH)
    val currentRealYear = todayCal.get(Calendar.YEAR)
    val isSameMonthAsReal = calendar.get(Calendar.MONTH) == currentRealMonth && calendar.get(Calendar.YEAR) == currentRealYear

    val dayStatus = remember(expenses, currentMonthMillis, subscriptions) {
        val map = mutableMapOf<Int, Triple<Boolean, Boolean, Boolean>>()
        val cal = Calendar.getInstance()

        expenses.forEach {
            cal.timeInMillis = it.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val current = map.getOrDefault(day, Triple(false, false, false))
            if (it.type == "EXPENSE") map[day] = current.copy(first = true) else map[day] = current.copy(second = true)
        }

        if (isSameMonthAsReal) {
            subscriptions.forEach { sub ->
                if (sub.billingDate > currentRealDay) {
                    val current = map.getOrDefault(sub.billingDate, Triple(false, false, false))
                    map[sub.billingDate] = current.copy(third = true)
                }
            }
        }
        map
    }

    val weeks = listOf("S", "M", "T", "W", "T", "F", "S")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { weeks.forEach { day -> Text(text = day, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.Bold) } }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(260.dp), userScrollEnabled = false) {
            items(offset) { Box(modifier = Modifier.size(40.dp)) }
            items(daysInMonth) { index ->
                val day = index + 1
                val isSelected = selectedDate == day
                val status = dayStatus[day]
                val hasExpense = status?.first == true
                val hasIncome = status?.second == true
                val hasSub = status?.third == true

                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onDateSelected(day) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = day.toString(), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (!isSelected) {
                            Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasExpense) Box(modifier = Modifier.size(4.dp).background(Color(0xFFE53935), CircleShape))
                                if (hasIncome) Box(modifier = Modifier.size(4.dp).background(Color(0xFF4CAF50), CircleShape))
                                if (hasSub) Box(modifier = Modifier.size(4.dp).background(Color(0xFF2196F3), CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSummaryCard(inc: Double, exp: Double, bal: Double, title: String, visible: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.total_income), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(if(visible) String.format("%.0f", inc) else "****", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.total_expense), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(if(visible) String.format("%.0f", exp) else "****", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.balance), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(if(visible) String.format("%.0f", bal) else "****", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun DateHeader(text: String) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun MonthSelector(curr: Long, prev: () -> Unit, next: () -> Unit) {
    val isZh = Locale.getDefault().language == "zh"
    val fmt = SimpleDateFormat(if (isZh) "yyyy年 MM月" else "MMM yyyy", Locale.getDefault())
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        IconButton(onClick = prev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(fmt.format(Date(curr)), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(onClick = next) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
}

@Composable
fun SetBudgetDialog(title: String, init: Double, dismiss: () -> Unit, confirm: (Double) -> Unit) {
    var v by remember { mutableStateOf(if (init > 0) String.format("%.0f", init) else "") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(v, { if (it.all { c -> c.isDigit() || c == '.' }) v = it }, label = { Text(stringResource(R.string.hint_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }, confirmButton = { Button(onClick = { confirm(v.toDoubleOrNull() ?: 0.0) }) { Text(stringResource(R.string.btn_save)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.btn_cancel)) } })
}

@Composable
fun DonutChart(stats: List<Float>, colors: List<Color>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val d = size.minDimension
        var start = -90f
        stats.forEachIndexed { i, p ->
            val sweep = p * 360f
            drawArc(colors[i % colors.size], start, sweep, false, style = Stroke(35.dp.toPx()), topLeft = Offset((size.width - d) / 2 + 35.dp.toPx() / 2, (size.height - d) / 2 + 35.dp.toPx() / 2), size = Size(d - 35.dp.toPx(), d - 35.dp.toPx()))
            start += sweep
        }
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)

    // 🟢 1. 新增：秘密后门状态变量
    var clickCount by remember { mutableStateOf(0) }
    var showDevBackdoor by remember { mutableStateOf(false) }
    var redeemCode by remember { mutableStateOf("") }

    // 主体滚动内容
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // 1. App Header
        item {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // 🟢 2. 修改：加上了连击触发器！
            Text(
                text = "v3.4.2",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.clickable {
                    clickCount++
                    if (clickCount >= 5) {
                        showDevBackdoor = true
                        clickCount = 0 // 重置点击次数
                    }
                }
            )
        }

        // 2. Credit Card (Design & AI)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Design 部分
                    Text(
                        text = stringResource(R.string.about_design_label), // "Design & Integration by"
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "白开水 (Bai Kai Shui)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI 部分
                    Text(
                        text = stringResource(R.string.about_ai_label), // "AI-Assisted Code Generation"
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = stringResource(R.string.about_ai_value), // "Powered by Google Gemini"
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // 3. Privacy Section (Privacy Title & Body)
        item {
            Text(
                text = stringResource(R.string.about_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Text(
                text = stringResource(R.string.about_privacy_body), // 这里会在中文模式下显示双语
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        // Footer
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "© 2026 白开水. All Rights Reserved.\n(AI-assisted code integration)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }

    // 🟢 3. 新增：开发者兑换码弹窗 (放在 LazyColumn 外部，保证弹窗能覆盖全屏)
    if (showDevBackdoor) {
        AlertDialog(
            onDismissRequest = {
                showDevBackdoor = false
                redeemCode = "" // 关闭时清空输入框
            },
            title = { Text("Developer Backdoor") },
            text = {
                OutlinedTextField(
                    value = redeemCode,
                    onValueChange = { redeemCode = it },
                    label = { Text("Enter Redeem Code") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val code = redeemCode.trim().uppercase() // 统一转大写，防止输错大小写

                    if (code == "白开水06260715") {
                        // 🟢 暗号 1：开启永久 VIP (老板专属)
                        prefs.edit()
                            .putBoolean("is_lifetime_vip", true)
                            .apply()
                        Toast.makeText(context, "VIP Activated! Welcome Boss!", Toast.LENGTH_LONG).show()

                    } else if (code == "0000") {
                        // 🔴 暗号 2：彻底解除 VIP (斩草除根测试用)
                        prefs.edit()
                            .remove("is_vip")              // 删掉旧版的残留
                            .remove("is_lifetime_vip")     // 撤销永久 VIP
                            .remove("vip_expiry_time")     // 💥 关键：清空到期时间戳！
                            .apply()
                        Toast.makeText(context, "VIP Deactivated! Back to normal.", Toast.LENGTH_SHORT).show()

                    } else {
                        // 错误暗号
                        Toast.makeText(context, "Invalid Code", Toast.LENGTH_SHORT).show()
                    }

                    showDevBackdoor = false
                    redeemCode = ""
                }) { Text("Unlock / Execute") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDevBackdoor = false
                    redeemCode = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun IgnoredLogsScreen(vm: HomeViewModel) {
    val logs by vm.ignoredLogs.collectAsState(initial = emptyList())
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // --- 顶部标题栏 ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_ignored), // "Ignored Logs"
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // "清空所有" 按钮
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = { vm.clearIgnoredLogs() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_delete)) // "Clear All"
                }
            }
        }

        // --- 内容区域 ---
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.no_data_search), color = Color.Gray) // "No logs found"
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // 防止被底部导航遮挡
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // 1. 头部：包名 + 原因
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 简化包名显示 (例如 com.bank.rhb -> rhb)
                                Text(
                                    text = log.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = log.reason, // e.g., "Keyword Filter"
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. 核心内容：标题 + 正文
                            Text(
                                text = log.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = log.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))

                            // 3. 操作按钮：删除 vs 恢复
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                // 单条删除
                                TextButton(onClick = { vm.deleteIgnoredLog(log) }) {
                                    Text("Delete", color = Color.Gray, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 🟢 恢复按钮 (Recover)
                                Button(
                                    onClick = { vm.recoverIgnoredLog(log, context) }, // 调用 ViewModel 的恢复逻辑
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Recover", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningListScreen(vm: HomeViewModel) {
    val list by vm.allLearnedHabits.collectAsState(initial = emptyList())
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            // "AI 记忆管理"
            Text(stringResource(R.string.settings_memory), style = MaterialTheme.typography.titleSmall)
        }
        if (list.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_data_search), color = Color.Gray)
                }
            }
        }
        items(list) { i ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${i.merchant} -> ${i.category}")
                IconButton({ vm.deleteLearning(i.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(viewModel: HomeViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    // 🟢 1. 状态变成了一个 List
    var attachedImageUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    val maxImages = 5 // 限制最多 5 张

    // 🟢 2. 改用 PickMultipleVisualMedia
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxImages)
    ) { uris ->
        if (uris.isNotEmpty()) {
            // 合并原有图片和新选的图片，并截断多余的
            attachedImageUris = (attachedImageUris + uris).take(maxImages)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.feedback_hint), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text(stringResource(R.string.feedback_placeholder)) },
            shape = RoundedCornerShape(12.dp)
        )

        // 🟢 3. 多图展示区
        if (attachedImageUris.size < maxImages) {
            OutlinedButton(
                onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Attach Screenshots (${attachedImageUris.size}/$maxImages)")
            }
        }

        if (attachedImageUris.isNotEmpty()) {
            // 横向滑动列表展示已选的图片名称/占位符
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attachedImageUris.size) { index ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Image ${index + 1}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    // 移除指定的图片
                                    attachedImageUris = attachedImageUris.toMutableList().apply { removeAt(index) }
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (feedbackText.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.feedback_empty_toast), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isSending = true
                viewModel.sendFeedbackToDiscord(
                    context = context,
                    feedbackText = feedbackText,
                    imageUris = attachedImageUris, // 🟢 传 List 进去
                    onSuccess = {
                        isSending = false
                        Toast.makeText(context, context.getString(R.string.feedback_success_toast), Toast.LENGTH_LONG).show()
                        onBack()
                    },
                    onError = { errorMsg ->
                        isSending = false
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isSending,
            shape = RoundedCornerShape(25.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feedback_send_btn), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(viewModel: HomeViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)

    // 数据迁移：兼容之前用兑换码变成 VIP 的老用户
    if (prefs.getBoolean("is_vip", false)) {
        prefs.edit().putBoolean("is_lifetime_vip", true).remove("is_vip").apply()
    }

    var isLifetimeVip by remember { mutableStateOf(VipUtils.isLifetimeVip(context)) }
    var vipExpiryTime by remember { mutableStateOf(VipUtils.getVipExpiryTime(context)) }
    val currentTime = System.currentTimeMillis()
    val isVip = isLifetimeVip || currentTime < vipExpiryTime

    var selectedPlan by remember { mutableStateOf(1) } // 0: 月, 1: 年, 2: 永久
    var showRedeemDialog by remember { mutableStateOf(false) }
    var redeemCode by remember { mutableStateOf("") }

    var showPaymentSheet by remember { mutableStateOf(false) }
    var senderName by remember { mutableStateOf("") }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ✅ 修复一 & 二：
    // 原来监听 latest_receipt（已被删除的节点），现在改为监听 history 的新增子节点。
    // 同时用 Firebase Transaction 在服务器端原子性标记 claimed:true，
    // 确保同一笔收款只能被一个用户认领，彻底杜绝同名用户双方都获得 VIP 的漏洞。
    //
    // ✅ 修复三：
    // 用独立的 rememberUpdatedState 持有 senderName 和 selectedPlan，
    // 让监听器内部永远能读到最新的值，不需要因为这两个值变化而重建监听器。
    val currentSenderName by rememberUpdatedState(senderName)
    val currentSelectedPlan by rememberUpdatedState(selectedPlan)

    // ✅ 修复三（续）：DisposableEffect 只依赖 showPaymentSheet，
    // 弹窗打开时注册一次，关闭时完整清理，不会因为用户输入名字就反复重建。
    DisposableEffect(showPaymentSheet) {
        if (showPaymentSheet) {
            val database = com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://ai-expense-tracker-0-default-rtdb.asia-southeast1.firebasedatabase.app/"
            )
            val historyRef = database.getReference("global_payments/history")

            // ✅ 修复一：改用 addChildEventListener 监听新增子节点
            // 只有在弹窗打开之后才会到达的新收款才触发，历史旧记录不会误触发
            val listener = object : com.google.firebase.database.ChildEventListener {
                override fun onChildAdded(
                    snapshot: com.google.firebase.database.DataSnapshot,
                    previousChildName: String?
                ) {
                    val receiptKey = snapshot.key ?: return
                    val text = snapshot.child("text").getValue(String::class.java) ?: return
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: return
                    val now = System.currentTimeMillis()

                    // 基本校验：必须是 10 分钟内的广播，且用户已输入名字
                    if (now - timestamp > 10 * 60 * 1000L) return
                    if (currentSenderName.isBlank()) return

                    val cleanInput = currentSenderName.trim().uppercase().replace("\\s+".toRegex(), "")
                    val targetPrice = when (currentSelectedPlan) { 0 -> "9.90"; 1 -> "89.90"; else -> "199.00" }

                    if (!text.contains(cleanInput) || !text.contains(targetPrice)) return

                    // ✅ 修复二：用 Firebase Transaction 原子性认领收据
                    // runTransaction 在服务器端保证只有第一个到达的用户能把 claimed 从 null 改为 true
                    // 后来的用户拿到的 currentData 已经是 true，直接 abort，不会重复发货
                    val claimRef = historyRef.child(receiptKey).child("claimed")
                    claimRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                        override fun doTransaction(
                            currentData: com.google.firebase.database.MutableData
                        ): com.google.firebase.database.Transaction.Result {
                            // 如果已经被别人认领了，放弃
                            if (currentData.getValue(Boolean::class.java) == true) {
                                return com.google.firebase.database.Transaction.abort()
                            }
                            // 还没人认领，标记为我的
                            currentData.value = true
                            return com.google.firebase.database.Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: com.google.firebase.database.DatabaseError?,
                            committed: Boolean,
                            currentData: com.google.firebase.database.DataSnapshot?
                        ) {
                            if (!committed || error != null) {
                                // 认领失败（被抢先了）——静默忽略，不发货
                                android.util.Log.w("PremiumScreen", "Receipt already claimed by another user.")
                                return
                            }

                            // 认领成功！正式发货
                            val editor = prefs.edit()
                            val base = if (vipExpiryTime > now) vipExpiryTime else now

                            when (currentSelectedPlan) {
                                0 -> {
                                    vipExpiryTime = base + 30L * 24 * 60 * 60 * 1000L
                                    editor.putLong("vip_expiry_time", vipExpiryTime)
                                }
                                1 -> {
                                    vipExpiryTime = base + 365L * 24 * 60 * 60 * 1000L
                                    editor.putLong("vip_expiry_time", vipExpiryTime)
                                }
                                2 -> {
                                    isLifetimeVip = true
                                    editor.putBoolean("is_lifetime_vip", true)
                                }
                            }
                            editor.apply()

                            // 回到主线程更新 UI
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                showPaymentSheet = false
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_payment_success),
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    })
                }

                override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
                override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("PremiumScreen", "Firebase listener cancelled: ${error.message}")
                }
            }

            // 只监听从现在起新增的子节点，过去的历史全部忽略
            historyRef.orderByChild("timestamp")
                .startAt(System.currentTimeMillis().toDouble())
                .addChildEventListener(listener)

            onDispose {
                historyRef.removeEventListener(listener)
            }
        } else {
            onDispose { /* 弹窗关闭，没有监听器需要清理 */ }
        }
    }

    Scaffold(
        bottomBar = {
            val priceStr = when (selectedPlan) { 0 -> "RM 9.90"; 1 -> "RM 89.90"; else -> "RM 199.00" }
            val btnText = if (isVip && selectedPlan != 2) {
                stringResource(R.string.premium_btn_renew, priceStr)
            } else {
                stringResource(R.string.premium_btn_pay, priceStr)
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = { showPaymentSheet = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLifetimeVip || selectedPlan != 2
                ) {
                    Text(
                        text = if (isLifetimeVip && selectedPlan == 2) stringResource(R.string.premium_btn_owned) else btnText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showRedeemDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.premium_btn_redeem), color = Color.Gray)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isVip) {
                item {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val formattedDate = dateFormat.format(java.util.Date(vipExpiryTime))
                    val expiryDateStr = if (isLifetimeVip) {
                        stringResource(R.string.premium_lifetime_access)
                    } else {
                        stringResource(R.string.premium_expires_on, formattedDate)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, Color(0xFFFFB300)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.premium_vip), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFFFB300))
                            Spacer(Modifier.height(4.dp))
                            Text(expiryDateStr, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                item {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "VIP", modifier = Modifier.size(80.dp), tint = Color(0xFFFFB300))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.premium_subtitle), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            val features = listOf(R.string.premium_feat_1, R.string.premium_feat_2, R.string.premium_feat_3, R.string.premium_feat_4)
            items(features.size) { index ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(features[index]), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                PricingCard(
                    title = stringResource(R.string.premium_plan_monthly),
                    price = "RM 9.90",
                    subtitle = stringResource(R.string.premium_plan_monthly_sub),
                    isSelected = selectedPlan == 0,
                    onClick = { selectedPlan = 0 }
                )
                Spacer(modifier = Modifier.height(12.dp))
                PricingCard(
                    title = stringResource(R.string.premium_plan_yearly),
                    price = "RM 89.90",
                    subtitle = stringResource(R.string.premium_plan_yearly_sub),
                    isSelected = selectedPlan == 1,
                    isBestValue = true,
                    onClick = { selectedPlan = 1 }
                )
                Spacer(modifier = Modifier.height(12.dp))
                PricingCard(
                    title = stringResource(R.string.premium_plan_lifetime),
                    price = "RM 199.00",
                    subtitle = stringResource(R.string.premium_plan_lifetime_sub),
                    isSelected = selectedPlan == 2,
                    onClick = { selectedPlan = 2 }
                )
            }
        }
    }

    if (showPaymentSheet) {
        val priceStr = when (selectedPlan) { 0 -> "RM 9.90"; 1 -> "RM 89.90"; else -> "RM 199.00" }
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.premium_duitnow_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ UX 修复：Step 1 — 强制先输入名字
                androidx.compose.material3.OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it.uppercase() },
                    label = { Text(stringResource(R.string.premium_sender_name_label)) },
                    placeholder = { Text(stringResource(R.string.premium_sender_name_hint)) },
                    supportingText = {
                        Text(
                            text = stringResource(R.string.premium_sender_name_warning),
                            color = Color(0xFFD32F2F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🟢 安全升级：名字必须大于等于 3 个字符才算 Ready，防短名撞车
                val nameReady = senderName.trim().length >= 3

                // ✅ UX 修复：Step 2 — 名字够长才解锁二维码
                androidx.compose.animation.AnimatedVisibility(
                    visible = nameReady,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "2. ${stringResource(R.string.premium_scan_to_pay, priceStr)}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.duitnow_qr),
                            contentDescription = "QR",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // 🟢 补回：金额必须绝对精准的红色免责声明
                        Text(
                            text = stringResource(R.string.premium_exact_amount_warning),
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 没输入名字时显示占位提示框
                if (!nameReady) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Color.LightGray.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.premium_name_too_short),
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 等待付款的转圈提示
                if (nameReady) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.premium_waiting_payment),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 🟢 补回：极客售后兜底 - 一键发送申诉邮件
                TextButton(onClick = {
                    val subject = context.getString(R.string.premium_support_email_subject, senderName.ifBlank { "Unknown" })
                    val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    val body = context.getString(R.string.premium_support_email_body, senderName.ifBlank { "N/A" }, priceStr, timeStr)

                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("rightstar.eshop@gmail.com")) // ⚠️ 切记替换成你的真实开发者邮箱
                        putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
                        putExtra(android.content.Intent.EXTRA_TEXT, body)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No email app found. Contact rightstar.eshop@gmail.com", android.widget.Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text(
                        text = stringResource(R.string.premium_support_btn),
                        color = Color.Gray,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showRedeemDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRedeemDialog = false },
            title = { Text(stringResource(R.string.premium_redeem_title), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = redeemCode,
                    onValueChange = { redeemCode = it },
                    label = { Text(stringResource(R.string.premium_redeem_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val code = redeemCode.trim().uppercase()
                    // 读取已经使用过的兑换码记录，防止重复白嫖
                    val usedCodes = prefs.getStringSet("used_redeem_codes", mutableSetOf()) ?: mutableSetOf()

                    if (usedCodes.contains(code)) {
                        // 拦截：已经用过的码
                        android.widget.Toast.makeText(context, context.getString(R.string.code_used), android.widget.Toast.LENGTH_SHORT).show()
                    }
                    else if (code == "白开水06260715") {
                        // 1. 老板专属：永久 VIP
                        prefs.edit().putBoolean("is_lifetime_vip", true).apply()
                        isLifetimeVip = true // 立即刷新 UI
                        android.widget.Toast.makeText(context, context.getString(R.string.toast_lifetime_activated), android.widget.Toast.LENGTH_LONG).show()
                        showRedeemDialog = false
                    }
                    else if (code == "FREE30DAYS" || code == "EARLYBIRD30") {
                        // 🟢 2. 新增功能：30天 VIP (你可以随意在 || 后面加新的兑换码)
                        val currentExpiry = prefs.getLong("vip_expiry_time", 0L)
                        val now = System.currentTimeMillis()

                        // 如果还没过期就在原基础上加，过期了就从今天开始算
                        val baseTime = if (currentExpiry > now) currentExpiry else now
                        val newExpiry = baseTime + 30L * 24 * 60 * 60 * 1000L // 加上 30 天的毫秒数

                        // 存入新时间，并且把这个码加入“已使用”黑名单
                        prefs.edit()
                            .putLong("vip_expiry_time", newExpiry)
                            .putStringSet("used_redeem_codes", usedCodes.toMutableSet().apply { add(code) })
                            .apply()

                        vipExpiryTime = newExpiry // 立即刷新 UI 状态
                        android.widget.Toast.makeText(context, "30 Days VIP Activated! 🎉", android.widget.Toast.LENGTH_LONG).show()
                        showRedeemDialog = false
                    }
                    else {
                        // 错误兑换码
                        android.widget.Toast.makeText(context, context.getString(R.string.toast_invalid_code), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.premium_redeem_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRedeemDialog = false }) {
                    Text(stringResource(R.string.premium_redeem_cancel))
                }
            }
        )
    }
}

// 辅助组件：价格卡片
@Composable
fun PricingCard(title: String, price: String, subtitle: String, isSelected: Boolean, isBestValue: Boolean = false, onClick: () -> Unit) {
    val titleColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isSelected) Color.DarkGray else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        border = if (isSelected) BorderStroke(2.dp, Color(0xFFFFB300)) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFFCF0) else MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = titleColor)
                    if (isBestValue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Badge(containerColor = Color(0xFFFF5252)) {
                            Text(stringResource(R.string.premium_badge_best_value), color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
            Text(price, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface)
        }
    }
}

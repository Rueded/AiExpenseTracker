package com.example.aiexpensetracker.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiexpensetracker.R
import com.example.aiexpensetracker.database.ExpenseEntity
import com.example.aiexpensetracker.database.SubscriptionEntity
import com.example.aiexpensetracker.viewmodel.AccountBalance
import com.example.aiexpensetracker.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen { Home, Stats, Learning, Logs, About, Subscriptions }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE) }

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
    val subscriptionSuggestion by viewModel.subscriptionSuggestion.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var showSettingsSheet by remember { mutableStateOf(false) }

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
                                AppScreen.Learning -> stringResource(R.string.settings_memory)
                                AppScreen.Logs -> stringResource(R.string.settings_ignored)
                                AppScreen.About -> stringResource(R.string.settings_about)
                                AppScreen.Subscriptions -> stringResource(R.string.settings_subscriptions)
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
                NavigationBarItem(icon = { Icon(Icons.Default.Info, contentDescription = null) }, label = { Text(stringResource(R.string.tab_stats)) }, selected = currentScreen == AppScreen.Stats, onClick = { currentScreen = AppScreen.Stats })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (currentScreen) {
                AppScreen.Home -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 🟢 汉化订阅检测弹窗
                        AnimatedVisibility(visible = subscriptionSuggestion != null, enter = fadeIn(), exit = fadeOut()) {
                            subscriptionSuggestion?.let { suggestion ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(stringResource(R.string.sub_detect_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) // "检测到潜在订阅"
                                        Text(stringResource(R.string.sub_detect_msg, suggestion.expense.merchant, String.format("%.2f", suggestion.expense.amount)), style = MaterialTheme.typography.bodyMedium) // "是每月自动扣款的订阅吗？"
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { viewModel.dismissSuggestion() }) { Text(stringResource(R.string.btn_no)) } // "忽略"
                                            Button(onClick = { viewModel.confirmSubscription(suggestion) }) { Text(stringResource(R.string.btn_yes_add)) } // "是的，添加"
                                        }
                                    }
                                }
                            }
                        }
                        TransactionListScreen(viewModel, isSearchActive, isCalendarView, accountBalances.map { it.name }, accountBalances, isBalanceVisible, subscriptions)
                    }
                }
                AppScreen.Stats -> StatsScreen(viewModel)
                AppScreen.Learning -> LearningListScreen(viewModel)
                AppScreen.Logs -> IgnoredLogsScreen(viewModel)
                AppScreen.About -> AboutScreen()
                AppScreen.Subscriptions -> SubscriptionScreen(viewModel) { currentScreen = AppScreen.Home }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                Text(stringResource(R.string.settings_ai_assistant), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                var currentPersona by remember { mutableStateOf(viewModel.getAiPersona(context)) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_persona)) },
                    supportingContent = {
                        val label = when(currentPersona) {
                            "STRICT" -> stringResource(R.string.persona_toxic)
                            "GENTLE" -> stringResource(R.string.persona_gentle)
                            else -> stringResource(R.string.persona_neutral)
                        }
                        Text(label)
                    },
                    trailingContent = { IconButton(onClick = { val next = when(currentPersona) { "NEUTRAL"->"STRICT"; "STRICT"->"GENTLE"; else->"NEUTRAL" }; currentPersona=next; viewModel.setAiPersona(context, next) }) { Icon(Icons.Default.Refresh, null) } }
                )

                ListItem(headlineContent = { Text(stringResource(R.string.settings_subscriptions)) }, leadingContent = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.clickable { currentScreen = AppScreen.Subscriptions; showSettingsSheet = false })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_memory)) }, leadingContent = { Icon(Icons.Default.Edit, null) }, modifier = Modifier.clickable { currentScreen = AppScreen.Learning; showSettingsSheet = false })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_ignored)) }, leadingContent = { Icon(Icons.Default.Warning, null) }, modifier = Modifier.clickable { currentScreen = AppScreen.Logs; showSettingsSheet = false })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_about)) }, leadingContent = { Icon(Icons.Default.Info, null) }, modifier = Modifier.clickable { currentScreen = AppScreen.About; showSettingsSheet = false })

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // 🟢 Compatibility

                val currentLocale = androidx.core.os.LocaleListCompat.getAdjustedDefault()[0]
                val isEnglish = currentLocale?.language == "en"
                ListItem(headlineContent = { Text(stringResource(R.string.settings_language)) }, trailingContent = { Switch(checked = isEnglish, onCheckedChange = { checked -> showSettingsSheet = false; onLanguageChange(if (checked) "en" else "zh") }) })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_dark_mode)) }, trailingContent = { Switch(checked = isDarkTheme, onCheckedChange = { checked -> onToggleTheme(checked) }) })

                ListItem(headlineContent = { Text(stringResource(R.string.settings_export)) }, leadingContent = { Icon(Icons.Default.Share, null) }, modifier = Modifier.clickable { exportCsvLauncher.launch("Expenses.csv"); showSettingsSheet = false })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_backup)) }, leadingContent = { Icon(painterResource(android.R.drawable.ic_menu_save), null) }, modifier = Modifier.clickable { backupLauncher.launch("Backup.json"); showSettingsSheet = false })
                ListItem(headlineContent = { Text(stringResource(R.string.settings_restore)) }, leadingContent = { Icon(painterResource(android.R.drawable.ic_menu_revert), null) }, modifier = Modifier.clickable { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream")); showSettingsSheet = false })

                ListItem(headlineContent = { Text(stringResource(R.string.settings_fix)) }, leadingContent = { Icon(Icons.Default.Settings, null) }, modifier = Modifier.clickable { try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); Toast.makeText(context, "Setting...", Toast.LENGTH_LONG).show() } catch (e: Exception) { }; showSettingsSheet = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val stats by viewModel.expenseStats.collectAsState(initial = emptyList())
    val currentType by viewModel.statsType.collectAsState()
    val totalAmount = remember(stats) { stats.sumOf { it.totalAmount } }

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
        // 支出/收入 切换
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = currentType == "EXPENSE", onClick = { viewModel.setStatsType("EXPENSE") }, label = { Text(stringResource(R.string.type_expense)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)); Spacer(modifier = Modifier.width(16.dp))
                FilterChip(selected = currentType == "INCOME", onClick = { viewModel.setStatsType("INCOME") }, label = { Text(stringResource(R.string.type_income)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
            }
        }

        // AI 分析卡片 (仅支出显示)
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
                                // "AI 财务顾问"
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
                            // "AI 人设: ..."
                            val personaLabel = when(report!!.persona) {
                                "STRICT" -> stringResource(R.string.persona_toxic)
                                "GENTLE" -> stringResource(R.string.persona_gentle)
                                else -> stringResource(R.string.persona_neutral)
                            }
                            Text(text = "${stringResource(R.string.settings_persona)}: $personaLabel", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        } else {
                            // "点击刷新..."
                            Text(text = stringResource(R.string.stats_hint), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 图表区域
        if (stats.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    // "本月暂无账单"
                    Text(stringResource(R.string.no_data_month), color = Color.Gray)
                }
            }
        } else {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                    DonutChart(stats = stats.map { it.percentage }, colors = currentColors)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // "总支出" / "总收入"
                        Text(if (currentType == "EXPENSE") stringResource(R.string.total_expense) else stringResource(R.string.total_income), fontSize = 14.sp, color = Color.Gray)
                        Text(text = "RM ${String.format("%.2f", totalAmount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(stats.size) { index ->
                val stat = stats[index]
                val color = currentColors[index % currentColors.size]
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddExpenseDialog(
    expenseToEdit: ExpenseEntity? = null,
    accountList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, String, Long, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE) }

    // --- 1. 状态定义 ---
    var amountStr by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var merchant by remember { mutableStateOf(expenseToEdit?.merchant ?: "") }
    var note by remember { mutableStateOf(expenseToEdit?.note ?: "") }
    var selectedType by remember { mutableStateOf(expenseToEdit?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(expenseToEdit?.category ?: "") }

    // 账户逻辑
    val safeAccountList = if (accountList.isEmpty()) listOf("Cash") else accountList
    var selectedAccount by remember { mutableStateOf(expenseToEdit?.accountName ?: safeAccountList[0]) }
    var selectedTargetAccount by remember { mutableStateOf(expenseToEdit?.targetAccountName ?: safeAccountList.getOrElse(1) { safeAccountList[0] }) }

    // 时间逻辑
    val calendar = remember { Calendar.getInstance().apply { timeInMillis = expenseToEdit?.timestamp ?: System.currentTimeMillis() } }
    var displayDateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 分类管理状态
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    // --- 2. 分类列表逻辑 ---
    val defaultExpenseCategories = listOf(stringResource(R.string.cat_food), stringResource(R.string.cat_transport), stringResource(R.string.cat_shopping), stringResource(R.string.cat_entertainment), stringResource(R.string.cat_home), stringResource(R.string.cat_medical), stringResource(R.string.cat_other))
    val defaultIncomeCategories = listOf(stringResource(R.string.cat_salary), stringResource(R.string.cat_bonus), stringResource(R.string.cat_allowance), stringResource(R.string.cat_investment), stringResource(R.string.cat_red_packet), stringResource(R.string.cat_other))

    val currentPrefKey = if (selectedType == "EXPENSE") "custom_cat_expense" else "custom_cat_income"
    val savedCustomCategories = remember(selectedType, showNewCategoryDialog, categoryToDelete) {
        prefs.getStringSet(currentPrefKey, emptySet())?.toList() ?: emptyList()
    }
    val currentDefaultList = if (selectedType == "EXPENSE") defaultExpenseCategories else defaultIncomeCategories

    // 合并列表
    val categoryList = remember(selectedType, savedCustomCategories) { (currentDefaultList + savedCustomCategories).toMutableStateList() }

    // 切换类型时，确保选中有效的分类
    LaunchedEffect(selectedType) {
        if (selectedType != "TRANSFER" && categoryList.isNotEmpty() && selectedCategory !in categoryList) {
            selectedCategory = categoryList.first()
        }
    }

    // --- 3. UI 内容 ---
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expenseToEdit != null) stringResource(R.string.dialog_edit_title) else stringResource(R.string.dialog_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 类型选择 (汉化)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(selected = selectedType == "EXPENSE", onClick = { selectedType = "EXPENSE" }, label = { Text(stringResource(R.string.type_exp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)); Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = selectedType == "INCOME", onClick = { selectedType = "INCOME" }, label = { Text(stringResource(R.string.type_inc)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9))); Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = selectedType == "TRANSFER", onClick = { selectedType = "TRANSFER" }, label = { Text(stringResource(R.string.type_trans)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer))
                }

                // 金额
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountStr = it },
                    label = { Text(stringResource(R.string.hint_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // 核心逻辑：转账 vs 普通 (汉化 Label)
                if (selectedType == "TRANSFER") {
                    Text(stringResource(R.string.label_from), style = MaterialTheme.typography.bodySmall) // "From:"
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc ->
                        val displayName = if (acc == "Cash") stringResource(R.string.text_cash) else acc
                        FilterChip(selected = selectedAccount == acc, onClick = { selectedAccount = acc }, label = { Text(displayName) })
                    } }

                    Text(stringResource(R.string.label_to), style = MaterialTheme.typography.bodySmall) // "To:"
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc ->
                        val displayName = if (acc == "Cash") stringResource(R.string.text_cash) else acc
                        FilterChip(selected = selectedTargetAccount == acc, onClick = { selectedTargetAccount = acc }, label = { Text(displayName) })
                    } }
                    merchant = "Transfer to $selectedTargetAccount"
                } else {
                    // 商户名 (汉化 Label)
                    OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text(stringResource(R.string.label_merchant)) }, modifier = Modifier.fillMaxWidth())

                    // 账户选择 (汉化 Label + Cash 汉化)
                    Text(stringResource(R.string.label_account), style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(safeAccountList) { acc ->
                        val displayName = if (acc == "Cash") stringResource(R.string.text_cash) else acc
                        FilterChip(selected = selectedAccount == acc, onClick = { selectedAccount = acc }, label = { Text(displayName) })
                    } }

                    // 分类选择
                    Text(stringResource(R.string.label_category), style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        items(categoryList) { category ->
                            val isSelected = selectedCategory == category
                            val isCustom = !currentDefaultList.contains(category)

                            // 长按删除逻辑
                            Box(contentAlignment = Alignment.Center) {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {},
                                    label = { Text(category) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { selectedCategory = category },
                                            onLongClick = {
                                                if (isCustom) categoryToDelete = category
                                                else Toast.makeText(context, "Cannot delete default category", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                )
                            }
                        }
                        // 添加分类按钮
                        item {
                            IconButton(onClick = { showNewCategoryDialog = true }, modifier = Modifier.size(32.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // 备注
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.hint_note)) }, modifier = Modifier.fillMaxWidth())

                // 日期选择
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(displayDateMillis)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedCard(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(displayDateMillis)), style = MaterialTheme.typography.bodySmall)
                        }
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
                    onConfirm(amt, merchant, finalCategory, selectedType, displayDateMillis, note, selectedAccount, finalTarget)
                }
            }) { Text(stringResource(R.string.btn_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )

    // 组件：DatePicker
    if (showDatePicker) {
        val ds = rememberDatePickerState(initialSelectedDateMillis = displayDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { ds.selectedDateMillis?.let { displayDateMillis = it; calendar.timeInMillis = it }; showDatePicker = false }) { Text(stringResource(R.string.btn_confirm)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.btn_cancel)) } }
        ) { DatePicker(state = ds) }
    }

    // 组件：TimePicker
    if (showTimePicker) {
        val ts = rememberTimePickerState(initialHour = calendar.get(Calendar.HOUR_OF_DAY), initialMinute = calendar.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { calendar.set(Calendar.HOUR_OF_DAY, ts.hour); calendar.set(Calendar.MINUTE, ts.minute); displayDateMillis = calendar.timeInMillis; showTimePicker = false }) { Text(stringResource(R.string.btn_confirm)) } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel)) } },
            text = { TimePicker(state = ts) }
        )
    }

    // 组件：新建分类
    if (showNewCategoryDialog) {
        var newCat by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text(stringResource(R.string.dialog_new_cat_title)) },
            text = { OutlinedTextField(value = newCat, onValueChange = { newCat = it }, label = { Text(stringResource(R.string.hint_cat_name)) }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    if (newCat.isNotBlank() && !categoryList.contains(newCat)) {
                        categoryList.add(newCat)
                        val set = prefs.getStringSet(currentPrefKey, emptySet())!!.toMutableSet()
                        set.add(newCat)
                        prefs.edit().putStringSet(currentPrefKey, set).apply()
                        selectedCategory = newCat
                    }
                    showNewCategoryDialog = false
                }) { Text(stringResource(R.string.btn_add)) }
            },
            dismissButton = { TextButton(onClick = { showNewCategoryDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }

    // 组件：删除分类确认 (汉化)
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.dialog_delete_category)) },
            text = { Text(stringResource(R.string.dialog_delete_category_msg, categoryToDelete!!)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { target ->
                            categoryList.remove(target)
                            val set = prefs.getStringSet(currentPrefKey, emptySet())?.toMutableSet()
                            set?.remove(target)
                            prefs.edit().putStringSet(currentPrefKey, set).apply()
                            if (selectedCategory == target) selectedCategory = categoryList.firstOrNull() ?: "Other"
                        }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
fun SubscriptionScreen(viewModel: HomeViewModel, onBack: () -> Unit = {}) {
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val totalMonthly = subscriptions.sumOf { it.amount }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.text_subscription_fixed), style = MaterialTheme.typography.labelMedium) // "订阅 / 固定支出"
                    Text("RM ${String.format("%.2f", totalMonthly)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    // 🟢 汉化: X 个活跃订阅
                    val countText = if (Locale.getDefault().language == "zh") "${subscriptions.size} 个活跃订阅" else "${subscriptions.size} active subscriptions"
                    Text(countText, style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subscriptions) { sub ->
                    val calendar = Calendar.getInstance()
                    val today = calendar.get(Calendar.DAY_OF_MONTH)
                    var daysLeft = sub.billingDate - today
                    if (daysLeft < 0) daysLeft += 30

                    // 🟢 汉化日期显示
                    val isZh = Locale.getDefault().language == "zh"
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
                                // 🟢 汉化: 每月 X 日
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
            viewModel.checkLatestExpenseForSubscription(latestExpense)
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
    val finalExpenses = remember(expenses, selectedDay, isCalendarView) {
        if (isCalendarView && selectedDay != null) {
            expenses.filter { val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }; cal.get(Calendar.DAY_OF_MONTH) == selectedDay }
        } else expenses
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
            } else if (!isSearchActive) {
                item { BudgetCard(displayExpense, monthlyBudget, categorySpendings, categoryBudgets, allExpenseCategories, { showTotalBudgetDialog = true }, { cat, _ -> targetCategoryForBudget = cat }, totalSubsCost) }
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
    if (showAddDialog) AddExpenseDialog(expenseToEdit, accountNames, { showAddDialog = false }) { amt, merch, cat, type, time, note, acc, tgt ->
        val id = expenseToEdit?.id ?: 0
        viewModel.addExpense(ExpenseEntity(id, amt, type, merch, cat, time, expenseToEdit?.originalText ?: context.getString(R.string.manual_entry), note, acc, tgt))
        showAddDialog = false
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
fun ExpenseCard(expense: ExpenseEntity, onDeleteClick: () -> Unit, onClick: () -> Unit, isVisible: Boolean) {
    val isIncome = expense.type == "INCOME"
    val isTransfer = expense.type == "TRANSFER"
    val amountColor = if (isIncome) Color(0xFF4CAF50) else if (isTransfer) Color(0xFF2196F3) else Color(0xFFE53935)
    val amountPrefix = if (isIncome) "+" else if (isTransfer) "" else "-"

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.merchant, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                if (expense.note.isNotBlank()) { Text(expense.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) }
                val accountInfo = if (isTransfer) "${expense.accountName} ➜ ${expense.targetAccountName}" else expense.accountName
                Text("${expense.category} • $accountInfo • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(expense.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTransfer) Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = amountColor, modifier = Modifier.size(20.dp).padding(end = 4.dp))
                val displayAmount = if (isVisible) "$amountPrefix RM ${String.format("%.2f", expense.amount)}" else "$amountPrefix ****"
                Text(displayAmount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = amountColor)
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.outline) }
            }
        }
    }
}

@Composable
fun BudgetCard(
    totalSpent: Double,
    monthlyBudget: Double,
    categorySpendings: Map<String, Double>,
    categoryBudgets: Map<String, Double>,
    allCategories: List<String>,
    onEditTotalBudget: () -> Unit,
    onEditCategoryBudget: (String, Double) -> Unit,
    totalSubscriptions: Double
) {
    var isExpanded by remember { mutableStateOf(false) }

    val safeBudget = if (monthlyBudget > 0) monthlyBudget else 1.0
    val spentProgress = (totalSpent / safeBudget).toFloat().coerceAtMost(1f)
    val reservedProgress = ((totalSpent + totalSubscriptions) / safeBudget).toFloat().coerceAtMost(1f)

    val isTotalOver = (totalSpent + totalSubscriptions) > monthlyBudget
    val totalColor = if (isTotalOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                    Text(stringResource(R.string.budget_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                IconButton(onClick = onEditTotalBudget, modifier = Modifier.size(24.dp)) { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)) }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                LinearProgressIndicator(
                    progress = { reservedProgress },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)),
                    color = Color.Gray.copy(alpha = 0.5f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { spentProgress },
                    modifier = Modifier.fillMaxWidth(spentProgress).height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = totalColor,
                    trackColor = Color.Transparent,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val reservedText = if (totalSubscriptions > 0) " (+RM ${String.format("%.0f", totalSubscriptions)} reserved)" else ""
                Text(text = "${stringResource(R.string.budget_used)} RM ${String.format("%.0f", totalSpent)}$reservedText", style = MaterialTheme.typography.labelSmall, color = totalColor)
                Text(text = "${stringResource(R.string.budget_total)} RM ${String.format("%.0f", monthlyBudget)}", style = MaterialTheme.typography.labelSmall)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.budget_detail_hint), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    allCategories.forEach { category ->
                        val spent = categorySpendings[category] ?: 0.0
                        val budget = categoryBudgets[category] ?: 0.0
                        val hasBudget = budget > 0
                        val catProgress = if (hasBudget) (spent / budget).toFloat() else 0f
                        val isCatOver = hasBudget && catProgress > 1f
                        val catColor = if (isCatOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onEditCategoryBudget(category, budget) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.25f))
                            Column(modifier = Modifier.weight(0.75f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "RM ${String.format("%.0f", spent)}", style = MaterialTheme.typography.labelSmall, color = if(isCatOver) MaterialTheme.colorScheme.error else Color.Unspecified)
                                    Text(text = if (hasBudget) "/ ${String.format("%.0f", budget)}" else stringResource(R.string.budget_unlimited), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                if (hasBudget) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(progress = { catProgress.coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = catColor, trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
            // 标题
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
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
}

@Composable
fun IgnoredLogsScreen(vm: HomeViewModel) {
    val logs by vm.ignoredLogs.collectAsState(initial = emptyList())
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                // "拦截日志"
                Text(stringResource(R.string.settings_ignored), style = MaterialTheme.typography.titleSmall)
                // "删除" (作为清空按钮)
                TextButton({ vm.clearIgnoredLogs() }) { Text(stringResource(R.string.btn_delete)) }
            }
        }
        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    // "暂无数据" (复用搜索无结果的提示)
                    Text(stringResource(R.string.no_data_search), color = Color.Gray)
                }
            }
        }
        items(logs) { log ->
            Text("${log.packageName}: ${log.reason}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
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
package com.example.aiexpensetracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.DownloadManager
import android.os.Environment
import com.example.aiexpensetracker.R
import com.example.aiexpensetracker.database.AccountEntity
import com.example.aiexpensetracker.database.AnalysisReportEntity
import com.example.aiexpensetracker.database.AppDatabase
import com.example.aiexpensetracker.database.ExpenseEntity
// 🟢 1. 必须导入这个，否则 'IgnoredEntity' 报红
import com.example.aiexpensetracker.database.IgnoredEntity
import com.example.aiexpensetracker.database.MerchantLearningEntity
import com.example.aiexpensetracker.database.SubscriptionEntity
import com.example.aiexpensetracker.network.AiProcessor
import com.example.aiexpensetracker.utils.EncryptionHelper
import com.example.aiexpensetracker.utils.ImageUtils
import com.example.aiexpensetracker.utils.ZipUtils
import com.example.aiexpensetracker.utils.VipUtils
import com.example.aiexpensetracker.network.ReceiptAnalysis
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.example.aiexpensetracker.network.DriveServiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Collections

data class CategoryStat(val category: String, val totalAmount: Double, val percentage: Float)
data class AccountBalance(val name: String, val balance: Double)
data class SubscriptionSuggestion(
    val expense: ExpenseEntity,
    val predictedBillingDate: Int
)
data class MonthlyTrend(
    val monthLabel: String, // 例如 "Jan", "Feb"
    val totalAmount: Float
)

data class BreakdownItem(
    val name: String,
    val price: String,
    val isChecked: Boolean = true,
    val qty: Int = 1
)

data class AppUpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val changelog: String = "",
    val downloadUrl: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()
    private val learningDao = database.merchantLearningDao()
    private val accountDao = database.accountDao()
    private val subscriptionDao = database.subscriptionDao()
    private val ignoredDao = database.ignoredDao()
    private val analysisDao = database.analysisReportDao()

    private val _rawExpenses = expenseDao.getAllExpenses()
    private val _accounts = accountDao.getAllAccounts()
    val accounts = _accounts
    val subscriptions = subscriptionDao.getAllSubscriptions()
    val ignoredLogs = ignoredDao.getRecentLogs()
    val allLearnedHabits = learningDao.getAllLearnings()

    private val _currentMonth = MutableStateFlow(System.currentTimeMillis())
    val currentMonth: StateFlow<Long> = _currentMonth

    private val _statsType = MutableStateFlow("EXPENSE")
    val statsType = _statsType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _editRequest = MutableStateFlow<Int?>(null)
    val editRequest = _editRequest.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget = _monthlyBudget.asStateFlow()

    private val _categoryBudgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categoryBudgets = _categoryBudgets.asStateFlow()

    // 🟢 1. 新增：用于在流水页面按分类筛选的变量
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    // 设置过滤的分类
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    private val _subscriptionSuggestions = MutableStateFlow<List<SubscriptionSuggestion>>(emptyList())

    val subscriptionSuggestions: StateFlow<List<SubscriptionSuggestion>> = _subscriptionSuggestions

    // 🟢 AI 报告状态
    private val _monthlyReport = MutableStateFlow<AnalysisReportEntity?>(null)
    val monthlyReport = _monthlyReport.asStateFlow()

    private var reportListeningJob: Job? = null

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport = _isGeneratingReport.asStateFlow()

    private val _monthlyTrend = MutableStateFlow<List<MonthlyTrend>>(emptyList())
    val monthlyTrend: StateFlow<List<MonthlyTrend>> = _monthlyTrend

    var driveServiceHelper: DriveServiceHelper? = null

    var scannedExpense by mutableStateOf<ExpenseEntity?>(null)
        private set

    fun clearScannedExpense() { scannedExpense = null }

    private val _scannedReceipt = MutableStateFlow<ReceiptAnalysis?>(null)
    val scannedReceipt: StateFlow<ReceiptAnalysis?> = _scannedReceipt

    // 🟢 新增 2: 临时保存当前扫描图片的 URI
    var currentScannedUri: Uri? = null

    // 🟢 控制付费墙弹窗的变量
    private val _showPaywall = MutableStateFlow(false)
    val showPaywall = _showPaywall.asStateFlow()

    // 🟢 存储更新信息的 State
    private val _updateInfo = kotlinx.coroutines.flow.MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: kotlinx.coroutines.flow.StateFlow<AppUpdateInfo?> = _updateInfo

    // 关闭更新弹窗
    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    // 🟢 检查更新的核心逻辑
    fun checkForUpdates(context: android.content.Context, showToastIfLatest: Boolean = false) {
        if (showToastIfLatest) {
            android.widget.Toast.makeText(context, "Checking for updates...", android.widget.Toast.LENGTH_SHORT).show()
        }

        val database = com.google.firebase.database.FirebaseDatabase.getInstance(
            "https://ai-expense-tracker-0-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        val updateRef = database.getReference("app_updates/latest")

        updateRef.get().addOnSuccessListener { snapshot ->
            val info = snapshot.getValue(AppUpdateInfo::class.java)
            if (info != null) {
                try {
                    // 获取当前手机上 App 的 Version Code
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        pInfo.versionCode
                    }

                    // 比较版本：如果云端版本 > 当前版本，弹出更新
                    if (info.versionCode > currentVersionCode) {
                        _updateInfo.value = info
                    } else {
                        if (showToastIfLatest) {
                            android.widget.Toast.makeText(context, "You are on the latest version! 🎉", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.addOnFailureListener {
            if (showToastIfLatest) {
                android.widget.Toast.makeText(context, "Failed to check update.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadUpdate(context: Context, url: String, versionName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.app_name) + " " + versionName)
                .setDescription(context.getString(R.string.update_downloading, 0))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AiExpenseTracker_$versionName.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, context.getString(R.string.update_download_start), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissPaywall() {
        _showPaywall.value = false
    }

    // 🟢 2. 修正后的扫描函数
    fun scanReceipt(context: Context, uri: Uri, onResult: () -> Unit) {
        // 先过收费站！
        checkAiQuotaAndProceed(
            context = context,
            onLimitReached = { _showPaywall.value = true }, // 拦截，弹窗！
            onProceed = { customKey, customModel ->
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = uriToBitmap(context, uri)
                        val scaledBitmap = scaleBitmapDown(bitmap, 1024)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "AI is reading details... 🧾", Toast.LENGTH_SHORT).show()
                        }

                        // 🟢 呼叫 AI，传入自定义 Key 和 Model
                        val analysisResult = AiProcessor.analyzeReceiptImage(scaledBitmap, customKey, customModel)

                        withContext(Dispatchers.Main) {
                            if (analysisResult != null) {
                                currentScannedUri = uri
                                _scannedReceipt.value = analysisResult
                                onResult()
                            } else {
                                Toast.makeText(context, "AI couldn't read the receipt. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Scan Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    fun clearScannedReceipt() {
        _scannedReceipt.value = null
        currentScannedUri = null
    }

    fun calculateMonthlyTrend() {
        viewModelScope.launch(Dispatchers.IO) {
            // 🔴 错误原因：Flow 没有 .value
            // 🟢 修正：使用 .first() 获取当前最新的列表数据
            val allExpenses = _rawExpenses.first()

            val calendar = Calendar.getInstance()
            val resultMap = linkedMapOf<String, Float>() // LinkedHashMap 保持顺序

            // 初始化最近 6 个月 (key = "MMM", value = 0f)
            for (i in 5 downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.MONTH, -i)
                val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(tempCal.time)
                resultMap[monthName] = 0f
            }

            // 遍历账单
            val sdf = SimpleDateFormat("MMM", Locale.getDefault())

            // 现在 allExpenses 被正确识别为 List<ExpenseEntity>，下面的报错也会消失
            allExpenses.forEach { expense ->
                if (expense.type == "EXPENSE") {
                    val expenseMonth = sdf.format(Date(expense.timestamp))

                    if (resultMap.containsKey(expenseMonth)) {
                        resultMap[expenseMonth] = resultMap[expenseMonth]!! + expense.amount.toFloat()
                    }
                }
            }

            // 转换成 List 并更新 StateFlow
            val trendList = resultMap.map { entry ->
                MonthlyTrend(entry.key, entry.value)
            }

            _monthlyTrend.value = trendList
        }
    }

    // 🟢 3. 修复 Bitmap 相关的辅助函数
    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width // 👈 现在这里不会报错了
        val originalHeight = bitmap.height

        if (originalWidth <= maxDimension && originalHeight <= maxDimension) return bitmap

        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val width: Int
        val height: Int

        if (originalWidth > originalHeight) {
            width = maxDimension
            height = (maxDimension / aspectRatio).toInt()
        } else {
            height = maxDimension
            width = (maxDimension * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    // 🟢 发送反馈到 Discord 的功能
    // 🟢 升级：支持多图发送到 Discord
    fun sendFeedbackToDiscord(
        context: Context,
        feedbackText: String,
        imageUris: List<android.net.Uri>, // 👈 变成了 List
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⚠️ 记得填你的 Discord Webhook URL！
                val webhookUrl = "https://discord.com/api/webhooks/1476117181086302335/UzzBY2CpAcg1NGhP8lT_iGcM2fVzrvFzXJPF3ZiAUNQeOV3hm4HO8TSjxihrOD2iF0fD"

                val client = OkHttpClient()
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

                builder.addFormDataPart("content", "**📢 收到新反馈 (AI Tracker)**\n> $feedbackText")

                // 🟢 循环遍历所有图片，Discord 接受 file0, file1, file2 这样的命名
                imageUris.forEachIndexed { index, uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        // 名字必须是 file0, file1 这种格式
                        builder.addFormDataPart("file$index", "screenshot_$index.jpg", requestBody)
                    }
                }

                val request = Request.Builder().url(webhookUrl).post(builder.build()).build()

                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) onSuccess() else onError("Failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("Error: ${e.localizedMessage}") }
            }
        }
    }

    // 🟢 终极防作弊版：三级分流权限检查器 (使用网络真实时间)
    fun checkAiQuotaAndProceed(context: Context, onLimitReached: () -> Unit, onProceed: (customKey: String?, customModel: String?) -> Unit) {
        // 开启 IO 协程进行网络请求
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            val isVip = VipUtils.isUserVip(context)
            val customKey = prefs.getString("custom_api_key", "") ?: ""
            val customModel = prefs.getString("custom_model_name", "") ?: ""

            // 级别 1 & 2：VIP 或 极客自带 Key，无需查时间，直接放行！
            if (isVip || (customKey.isNotBlank() && customModel.isNotBlank())) {
                withContext(Dispatchers.Main) {
                    onProceed(customKey.takeIf { it.isNotBlank() }, customModel.takeIf { it.isNotBlank() })
                }
                return@launch
            }

            // 级别 3：普通用户，去网络获取真实的吉隆坡时间 🕵️‍♂️
            val today = AiProcessor.getNetworkDate()
            val lastDate = prefs.getString("last_ai_usage_date", "")
            var usage = prefs.getInt("daily_ai_usage", 0)

            // 如果真实网络时间跨天了，才重置额度
            if (today != lastDate) {
                usage = 0
                prefs.edit().putString("last_ai_usage_date", today).apply()
            }

            // 切回主线程更新 UI 状态
            withContext(Dispatchers.Main) {
                if (usage >= 5) {
                    onLimitReached() // ❌ 没额度了，拦截！
                } else {
                    prefs.edit().putInt("daily_ai_usage", usage + 1).apply()
                    onProceed(null, null) // ✅ 扣除额度，放行！
                }
            }
        }
    }

    // 检查用户是否已经登录
    fun checkGoogleLogin(context: Context) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            initializeDriveHelper(context, account)
        }
    }

    // 初始化 Drive Helper
    fun initializeDriveHelper(context: Context, account: GoogleSignInAccount) {
        try {
            // 1. 创建 Google 凭证，指定权限为 "只能访问 App 自己的文件夹" (DRIVE_APPFOLDER)
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            )
            // 2. 绑定当前登录的账户
            credential.selectedAccount = account.account

            // 3. 构建 Google Drive API 客户端
            val googleDriveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("AI Expense Tracker").build()

            // 4. 初始化我们的 Helper
            driveServiceHelper = DriveServiceHelper(googleDriveService)

            Log.d("Drive", "Helper initialized successfully")

            // 5. 🟢 登录成功后，立刻尝试一次自动备份
            //performCloudBackup(context)
            Toast.makeText(context, "Drive Connected. Please choose Backup or Restore.", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Drive Init Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 🟢 1. 云端备份 (ZIP 版：包含图片 + 加密数据)
    fun performCloudBackup(context: Context, showToast: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 检查连接
            if (driveServiceHelper == null) {
                if (showToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Google Drive not connected!", Toast.LENGTH_LONG).show()
                    }
                }
                return@launch
            }

            try {
                // 2. 准备数据
                val plainJson = generateBackupJsonString(context)

                // 🔒 加密 JSON 内容 (保护隐私)
                val encryptedJson = EncryptionHelper.encrypt(plainJson)

                // 3. 收集所有图片路径 (只取非空的)
                val allExpenses = _rawExpenses.first()
                val imagePaths = allExpenses.mapNotNull { it.imagePath }

                // 4. 创建临时 ZIP 文件
                val zipFile = java.io.File(context.cacheDir, "temp_backup.zip")

                // 📦 打包！(使用 ZipUtils)
                // 注意：这里我们把加密后的 JSON 塞进去
                ZipUtils.zipData(context, encryptedJson, imagePaths, zipFile)

                // 5. 上传 ZIP 到 Drive
                driveServiceHelper?.uploadZipFile(zipFile)

                // 6. 清理临时文件
                if (zipFile.exists()) zipFile.delete()

                // 7. 成功提示
                withContext(Dispatchers.Main) {
                    Log.d("Drive", "ZIP Backup success")
                    if (showToast) {
                        Toast.makeText(context, "☁️ Cloud Backup (w/ Images) Success!", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (showToast) {
                        Toast.makeText(context, "Backup Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // 🟢 2. 云端恢复 (ZIP 版：解压图片 + 解密数据)
    fun performCloudRestore(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (driveServiceHelper == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Drive not connected", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "🔍 Searching backup...", Toast.LENGTH_SHORT).show()
                }

                // A. 搜索 ZIP 文件 (注意后缀是 .zip)
                val fileId = driveServiceHelper?.searchFile("expense_backup.zip")

                if (fileId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ No backup found in Drive.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // B. 下载 ZIP 到临时文件
                val tempZip = java.io.File(context.cacheDir, "restore_temp.zip")
                val success = driveServiceHelper?.downloadZipFile(tempZip, fileId)

                if (success == true) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "📦 Unzipping & Restoring...", Toast.LENGTH_SHORT).show()
                    }

                    // C. 解压！(图片会自动回到 files 目录)
                    // 返回值是那个加密的 JSON 字符串
                    val encryptedJson = ZipUtils.unzipData(context, tempZip)

                    if (!encryptedJson.isNullOrEmpty()) {
                        // D. 解密
                        val plainJson = EncryptionHelper.decrypt(encryptedJson)

                        if (plainJson.isNotEmpty() && plainJson.trim().startsWith("{")) {
                            // E. 恢复数据库
                            restoreFromJsonString(plainJson, context)

                            withContext(Dispatchers.Main) {
                                refreshBudgets()
                                Toast.makeText(context, "✅ Full Restore Complete!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "❌ Decryption Failed.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ Download Failed.", Toast.LENGTH_LONG).show()
                    }
                }

                // 清理
                if (tempZip.exists()) tempZip.delete()

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Restore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🟢 核心逻辑 1：从数据库读取数据 -> 生成 JSON 字符串
    private suspend fun generateBackupJsonString(context: Context): String {
        val expenses = _rawExpenses.first()
        val learnings = learningDao.getAllLearnings().first()
        val accounts = _accounts.first()
        val subscriptions = subscriptionDao.getAllSubscriptions().first()
        val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)

        val root = JSONObject()

        // 1. 账目数据 (Expenses)
        val expArray = JSONArray()
        expenses.forEach {
            val o = JSONObject()
            o.put("amount", it.amount)
            o.put("type", it.type)
            o.put("merchant", it.merchant)
            o.put("category", it.category)
            o.put("timestamp", it.timestamp)
            o.put("originalText", it.originalText)
            o.put("note", it.note)
            o.put("accountName", it.accountName)
            o.put("targetAccountName", it.targetAccountName)
            o.put("imagePath", it.imagePath) // 保存图片路径
            expArray.put(o)
        }
        root.put("expenses", expArray)

        // 2. AI 记忆 (Learnings)
        val learnArray = JSONArray()
        learnings.forEach {
            val o = JSONObject()
            o.put("merchant", it.merchant)
            o.put("timeSlot", it.timeSlot)
            o.put("category", it.category)
            o.put("note", it.note)
            o.put("useCount", it.useCount)
            learnArray.put(o)
        }
        root.put("memories", learnArray)

        // 3. 订阅数据 (Subscriptions)
        val subArray = JSONArray()
        subscriptions.forEach {
            val o = JSONObject()
            o.put("merchant", it.merchant)
            o.put("amount", it.amount)
            o.put("category", it.category)
            o.put("billingDate", it.billingDate)
            o.put("accountName", it.accountName)
            o.put("isActive", it.isActive)
            subArray.put(o)
        }
        root.put("subscriptions", subArray)

        // 4. 账户列表 (Accounts)
        val accArray = JSONArray()
        accounts.forEach {
            val o = JSONObject()
            o.put("name", it.name)
            o.put("initialBalance", it.initialBalance)
            accArray.put(o)
        }
        root.put("accounts", accArray)

        // 5. 偏好设置、VIP 身份及 AI 配置 (Preferences & VIP)
        val prefObj = JSONObject()

        // 基础预算
        prefObj.put("monthly_budget", prefs.getFloat("monthly_budget", 0f).toDouble())

        // 自定义分类
        val customCats = prefs.getStringSet("custom_cat_expense", emptySet()) ?: emptySet()
        prefObj.put("custom_cat_expense", JSONArray(customCats))

        // 🔴 关键：VIP 状态同步
        prefObj.put("is_lifetime_vip", prefs.getBoolean("is_lifetime_vip", false))
        prefObj.put("vip_expiry_time", prefs.getLong("vip_expiry_time", 0L))

        // 🔴 关键：AI 自定义配置同步
        prefObj.put("custom_api_key", prefs.getString("custom_api_key", ""))
        prefObj.put("custom_model_name", prefs.getString("custom_model_name", ""))

        // 分类预算 (budget_cat_...)
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("budget_cat_") && v is Float) {
                prefObj.put(k, v.toDouble())
            }
        }
        root.put("preferences", prefObj)

        return root.toString(4) // 格式化导出
    }

    // 🟢 核心逻辑 2：解析 JSON 字符串 -> 写入数据库
    // 🟢 智能恢复：防止重复数据 (Smart Merge)
    private suspend fun restoreFromJsonString(jsonString: String, context: Context) {
        val root = JSONObject(jsonString)

        // 1. 恢复偏好设置、VIP 身份及 AI 配置
        if (root.has("preferences")) {
            val prefObj = root.getJSONObject("preferences")
            val editor = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE).edit()

            // 🔴 恢复 VIP 权限
            if (prefObj.has("is_lifetime_vip")) {
                editor.putBoolean("is_lifetime_vip", prefObj.getBoolean("is_lifetime_vip"))
            }
            if (prefObj.has("vip_expiry_time")) {
                editor.putLong("vip_expiry_time", prefObj.getLong("vip_expiry_time"))
            }

            // 🔴 恢复 AI 配置
            if (prefObj.has("custom_api_key")) {
                editor.putString("custom_api_key", prefObj.getString("custom_api_key"))
            }
            if (prefObj.has("custom_model_name")) {
                editor.putString("custom_model_name", prefObj.getString("custom_model_name"))
            }

            // 恢复预算和自定义分类
            val keys = prefObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                when {
                    k == "custom_cat_expense" -> {
                        val arr = prefObj.getJSONArray(k)
                        val set = mutableSetOf<String>()
                        for (i in 0 until arr.length()) set.add(arr.getString(i))
                        editor.putStringSet(k, set)
                    }
                    k == "monthly_budget" || k.startsWith("budget_cat_") -> {
                        editor.putFloat(k, prefObj.optDouble(k, 0.0).toFloat())
                    }
                }
            }
            editor.apply()
        }

        // 2. 恢复账户
        if (root.has("accounts")) {
            val arr = root.getJSONArray("accounts")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val n = o.getString("name")
                if (accountDao.exists(n) == 0) {
                    accountDao.insert(AccountEntity(name = n, initialBalance = o.optDouble("initialBalance", 0.0)))
                }
            }
        }

        // 3. 恢复账目 (含图片路径修正 & 去重)
        if (root.has("expenses")) {
            val arr = root.getJSONArray("expenses")
            val currentExpenses = _rawExpenses.first()
            val existingSignatures = currentExpenses.map { "${it.timestamp}_${it.amount}_${it.merchant}" }.toHashSet()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val ts = o.getLong("timestamp")
                val amt = o.getDouble("amount")
                val merch = o.getString("merchant")
                val signature = "${ts}_${amt}_${merch}"

                if (!existingSignatures.contains(signature)) {
                    val acc = o.getString("accountName")
                    if (accountDao.exists(acc) == 0) accountDao.insert(AccountEntity(name = acc))

                    // 🔴 路径修正：确保新旧手机图片目录一致
                    var imgPath = if (o.has("imagePath") && !o.isNull("imagePath")) o.getString("imagePath") else null
                    if (imgPath != null) {
                        val fileName = java.io.File(imgPath).name
                        imgPath = java.io.File(context.filesDir, fileName).absolutePath
                    }

                    expenseDao.insert(ExpenseEntity(
                        amount = amt,
                        type = o.getString("type"),
                        merchant = merch,
                        category = o.getString("category"),
                        timestamp = ts,
                        originalText = o.optString("originalText", "Manual"),
                        note = o.optString("note", ""),
                        accountName = acc,
                        targetAccountName = if (o.has("targetAccountName") && !o.isNull("targetAccountName")) o.getString("targetAccountName") else null,
                        imagePath = imgPath
                    ))
                    existingSignatures.add(signature)
                }
            }
        }

        // 4. 恢复 AI 记忆
        if (root.has("memories")) {
            val arr = root.getJSONArray("memories")
            val currentLearnings = learningDao.getAllLearnings().first()
            val existingMemories = currentLearnings.map { "${it.merchant}_${it.category}" }.toHashSet()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val merch = o.getString("merchant")
                val cat = o.getString("category")
                if (!existingMemories.contains("${merch}_${cat}")) {
                    learningDao.insert(MerchantLearningEntity(
                        merchant = merch,
                        timeSlot = o.optString("timeSlot", "Any"),
                        category = cat,
                        note = o.optString("note", ""),
                        useCount = o.optInt("useCount", 1)
                    ))
                }
            }
        }

        // 5. 恢复订阅
        if (root.has("subscriptions")) {
            val arr = root.getJSONArray("subscriptions")
            val currentSubs = subscriptionDao.getAllSubscriptions().first()
            val existingSubs = currentSubs.map { "${it.merchant}_${it.amount}" }.toHashSet()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val merch = o.getString("merchant")
                val amt = o.getDouble("amount")
                if (!existingSubs.contains("${merch}_${amt}")) {
                    subscriptionDao.insert(SubscriptionEntity(
                        merchant = merch,
                        amount = amt,
                        category = o.getString("category"),
                        billingDate = o.getInt("billingDate"),
                        accountName = o.getString("accountName"),
                        isActive = o.optBoolean("isActive", true)
                    ))
                }
            }
        }
    }

    init {
        // 1. 初始化默认账户 (保持不变)
        viewModelScope.launch(Dispatchers.IO) {
            if (accountDao.getCount() == 0) {
                accountDao.insert(AccountEntity(name = "Cash", initialBalance = 0.0))
            }
        }

        // 2. 刷新预算 (保持不变)
        refreshBudgets()

        // 3. 监听 AI 分析报告 (保持不变)
        startListeningToReport()

        // 🟢 4. 新增：自动计算趋势图数据
        // 只要 _rawExpenses (账单列表) 发生变化，就重新计算最近 6 个月的趋势
        viewModelScope.launch {
            _rawExpenses.collect { expenses ->
                // 这里调用我们在上一步写的计算函数
                calculateMonthlyTrend()
            }
        }
    }

    // 🟢 手动管理 AI 报告监听
    private fun startListeningToReport() {
        reportListeningJob?.cancel()
        val currentMonthId = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(_currentMonth.value))
        reportListeningJob = viewModelScope.launch(Dispatchers.IO) {
            analysisDao.getReport(currentMonthId).collect { report ->
                _monthlyReport.value = report
            }
        }
    }

    fun isUserVip(context: Context): Boolean {
        val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        val isLifetime = prefs.getBoolean("is_lifetime_vip", false)
        val expiryTime = prefs.getLong("vip_expiry_time", 0L)

        // 只要是永久会员，或者当前时间还没到期，就是 VIP！
        return isLifetime || System.currentTimeMillis() < expiryTime
    }

    fun deleteIgnoredLog(log: IgnoredEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            ignoredDao.delete(log)
        }
    }

    fun recoverIgnoredLog(log: IgnoredEntity, context: Context) {
        checkAiQuotaAndProceed(
            context = context,
            onLimitReached = { _showPaywall.value = true },
            onProceed = { customKey, customModel ->
                viewModelScope.launch(Dispatchers.IO) {
                    val fullText = "App: ${log.packageName}. Title: ${log.title}. Message: ${log.text}"

                    try {
                        val result = AiProcessor.analyze(fullText, customKey, customModel)

                        if (result != null && result.valid) {
                            // 1. AI 验证成功，从垃圾桶删除
                            ignoredDao.delete(log)

                            // 🟢 2. 使用拦截时原本的真实时间！(不再用当前时间)
                            val originalTime = log.timestamp

                            // 🟢 3. 像正常监听一样，自动识别付款账户
                            val accountName = detectAccountName(log.packageName)
                            if (accountDao.exists(accountName) == 0) {
                                accountDao.insert(AccountEntity(name = accountName))
                            }

                            // 🟢 4. 像正常监听一样，加载 AI 记忆 (Merchant Learning)
                            val merchantName = result.merchant ?: "Unknown"
                            val timeSlot = getTimeSlot(originalTime)
                            val memory = if (merchantName != "Unknown") learningDao.getLearning(merchantName, timeSlot) else null

                            val finalCategory = memory?.category ?: result.category ?: "Uncategorized"
                            // 如果有记忆的 Note 就用记忆的，没有就标上 Recovered
                            val finalNote = if (memory?.note.isNullOrBlank()) "Recovered: ${log.title}" else memory!!.note

                            // 5. 存入账本，完美复原
                            expenseDao.insert(ExpenseEntity(
                                amount = result.amount ?: 0.0,
                                type = result.type ?: "EXPENSE",
                                merchant = merchantName,
                                category = finalCategory,
                                timestamp = originalTime, // 👈 关键：用回历史时间
                                originalText = log.text,
                                note = finalNote,
                                accountName = accountName
                            ))

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.toast_recover_success), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // 失败的分支
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.toast_recover_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    // 🟢 补上这个账户映射辅助方法 (从 NotificationListener 移植过来)
    private fun detectAccountName(pkg: String): String {
        return when (pkg) {
            "my.com.tngdigital.ewallet" -> "TNG eWallet"
            "com.grabtaxi.passenger" -> "GrabPay"
            "com.shopee.my" -> "ShopeePay"
            "com.maybank2u.life" -> "Maybank"
            "com.cimb.octo", "com.cimb.clicks.android" -> "CIMB"
            "my.com.rhbgroup.mobilebanking", "com.rhbgroup.rhbengineering", "com.rhbgroup.rhbmobilebanking" -> "RHB"
            "com.hongleong.pb" -> "Hong Leong"
            "my.com.publicbank.pbe" -> "Public Bank"
            "my.com.mybsn", "com.mybsn.mobile", "net.mybsn.secure" -> "BSN"
            "com.ambank.ambank" -> "AmBank"
            "com.airasia.bigpay" -> "BigPay"
            "com.bankislam.go" -> "Bank Islam"
            else -> "Cash"
        }
    }

    fun prevMonth() {
        val c = Calendar.getInstance()
        c.timeInMillis = _currentMonth.value
        c.add(Calendar.MONTH, -1)
        _currentMonth.value = c.timeInMillis
        startListeningToReport()
    }

    fun nextMonth() {
        val c = Calendar.getInstance()
        c.timeInMillis = _currentMonth.value
        c.add(Calendar.MONTH, 1)
        _currentMonth.value = c.timeInMillis
        startListeningToReport()
    }

    // 🟢 生成 AI 月度报告
    fun generateMonthlyReport(context: Context) {
        if (_isGeneratingReport.value) return

        checkAiQuotaAndProceed(
            context = context,
            onLimitReached = { _showPaywall.value = true }, // 拦截，弹窗！
            onProceed = { customKey, customModel ->
                viewModelScope.launch(Dispatchers.IO) {
                    _isGeneratingReport.value = true
                    try {
                        val currentMonthId = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(_currentMonth.value))
                        val expenses = filteredExpenses.first()

                        if (expenses.isEmpty()) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "No data this month!", Toast.LENGTH_SHORT).show() }
                            return@launch
                        }

                        val totalSpent = expenses.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                        val grouped = expenses.filter { it.type == "EXPENSE" }.groupBy { it.category }
                        val topCatEntry = grouped.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                        val topCategory = topCatEntry?.key ?: "None"
                        val topAmount = topCatEntry?.value?.sumOf { it.amount } ?: 0.0

                        val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
                        val budget = prefs.getFloat("monthly_budget", 3000f).toDouble()
                        val persona = prefs.getString("ai_persona", "NEUTRAL") ?: "NEUTRAL"
                        val currentLocale = androidx.core.os.LocaleListCompat.getAdjustedDefault()[0]
                        val lang = if (currentLocale?.language == "zh") "zh" else "en"

                        // 🟢 呼叫 AI，传入自定义 Key 和 Model
                        val insightText = AiProcessor.generateInsight(
                            totalSpent, budget, topCategory, topAmount, persona, lang,
                            customKey, customModel
                        )

                        val report = AnalysisReportEntity(
                            monthId = currentMonthId, totalSpent = totalSpent, summaryText = insightText, persona = persona
                        )
                        analysisDao.insert(report)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    } finally {
                        _isGeneratingReport.value = false
                    }
                }
            }
        )
    }

    fun setAiPersona(context: Context, persona: String) {
        context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putString("ai_persona", persona).apply()
    }

    fun getAiPersona(context: Context): String {
        return context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .getString("ai_persona", "NEUTRAL") ?: "NEUTRAL"
    }

    fun refreshBudgets() {
        val prefs = getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        _monthlyBudget.value = prefs.getFloat("monthly_budget", 3000f).toDouble()
        val allPrefs = prefs.all
        val catMap = mutableMapOf<String, Double>()
        allPrefs.forEach { (key, value) ->
            if (key.startsWith("budget_cat_"))
                catMap[key.removePrefix("budget_cat_")] = (value as? Float)?.toDouble() ?: 0.0
        }
        _categoryBudgets.value = catMap
    }

    fun updateMonthlyBudget(newBudget: Double) {
        getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
        _monthlyBudget.value = newBudget
    }

    fun updateCategoryBudget(category: String, newBudget: Double) {
        getApplication<Application>().getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .edit().putFloat("budget_cat_$category", newBudget.toFloat()).apply()
        refreshBudgets()
    }

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 🟢 P0 新增核心逻辑：图片转存
            // 如果 imagePath 存在且是临时 URI (content://)，说明是刚选的图，需要转存到私有目录
            var finalExpense = expense
            if (!expense.imagePath.isNullOrBlank() && expense.imagePath.startsWith("content://")) {
                // 调用我们在 Utils 里写的保存工具
                val savedPath = ImageUtils.saveImageToInternalStorage(
                    getApplication(),
                    android.net.Uri.parse(expense.imagePath)
                )
                // 如果保存成功，更新对象里的路径为本地绝对路径
                if (savedPath != null) {
                    finalExpense = expense.copy(imagePath = savedPath)
                }
            }

            // 1. 检查账户是否存在 (注意：后续所有操作都要用 finalExpense)
            if (accountDao.exists(finalExpense.accountName) == 0)
                accountDao.insert(AccountEntity(name = finalExpense.accountName))

            finalExpense.targetAccountName?.let {
                if (accountDao.exists(it) == 0)
                    accountDao.insert(AccountEntity(name = it))
            }

            // 2. 插入或更新
            if (finalExpense.id == 0) {
                expenseDao.insert(finalExpense)
            } else {
                expenseDao.update(finalExpense)
            }

            // 3. AI 学习与订阅检查
            if (finalExpense.type == "EXPENSE") {
                if (finalExpense.merchant.isNotBlank()) learnUserHabit(finalExpense)
                // 🟢 修复：使用 getApplication() 替代 context，使用 finalExpense 替代 newExpense
                checkLatestExpenseForSubscription(getApplication(), finalExpense)
            }

            // 4. 刷新首页预算条 UI
            refreshBudgets()

            // 5. Google Drive 自动同步
            if (driveServiceHelper != null) {
                performCloudBackup(getApplication())
            }
        }
    }

    private suspend fun learnUserHabit(expense: ExpenseEntity) {
        withContext(Dispatchers.IO) {
            val timeSlot = getTimeSlot(expense.timestamp)
            val existing = learningDao.getLearning(expense.merchant, timeSlot)
            if (existing != null)
                learningDao.updateLearning(existing.id, expense.category, expense.note)
            else
                learningDao.insert(MerchantLearningEntity(
                    merchant = expense.merchant,
                    timeSlot = timeSlot,
                    category = expense.category,
                    note = expense.note
                ))
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 删除数据库记录
            expenseDao.delete(expense)

            // 2. 刷新首页预算条 UI
            refreshBudgets()

            // 🟢 3. Google Drive 自动同步
            // 数据变动了，同步到云端以免云端数据滞后
            if (driveServiceHelper != null) {
                performCloudBackup(getApplication())
            }
        }
    }

    suspend fun getExpenseById(id: Int): ExpenseEntity? =
        withContext(Dispatchers.IO) { _rawExpenses.first().find { it.id == id } }

    fun checkLatestExpenseForSubscription(context: Context, expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (subscriptionDao.countActiveByMerchant(expense.merchant) > 0) return@launch
            val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            val ignoredIds = prefs.getStringSet("ignored_expense_ids", emptySet()) ?: emptySet()
            if (ignoredIds.contains(expense.id.toString())) return@launch
            val ignoredMerchants = prefs.getStringSet("ignored_sub_merchants", emptySet()) ?: emptySet()
            if (ignoredMerchants.contains(expense.merchant)) return@launch

            val strictCategories = listOf("Digital Services", "Communication")
            val isCategoryMatch = strictCategories.any { expense.category.contains(it, true) }
            val strongKeywords = listOf("subscription", "premium", "会员", "月", "年", "vip", "舰长", "包月", "连续包")
            val isKeywordMatch = strongKeywords.any { expense.merchant.contains(it, true) || expense.originalText.contains(it, true) || expense.note.contains(it, true) }
            val knownSubMerchants = listOf("spotify", "netflix", "youtube", "yt", "apple", "google", "icloud", "bilibili", "bili", "哔哩", "iqiyi", "爱奇艺", "disney", "patreon", "tencent", "腾讯", "youku", "优酷", "netease", "网易", "taobao", "淘宝", "baidu", "百度", "astro", "unifi", "maxis", "celcom", "digi", "umobile", "yes")
            val isKnownMerchant = knownSubMerchants.any { expense.merchant.contains(it, true) }

            val maybeSub = isCategoryMatch || isKeywordMatch || isKnownMerchant

            if (maybeSub) {
                // 必须切回主线程去检查额度，因为检查逻辑可能会操作 SharedPreferences 和状态
                withContext(Dispatchers.Main) {
                    checkAiQuotaAndProceed(
                        context = context,
                        onLimitReached = {
                            // 🤫 关键：这里是后台自动检查，额度用完时千万别弹窗，默默放过就好！
                        },
                        onProceed = { customKey, customModel ->
                            viewModelScope.launch(Dispatchers.IO) {
                                // 🟢 呼叫 AI，传入自定义 Key 和 Model
                                val isRealSub = AiProcessor.checkIfSubscription(expense.merchant, expense.category, expense.note, customKey, customModel)
                                if (isRealSub) {
                                    val suggestion = SubscriptionSuggestion(
                                        expense,
                                        Calendar.getInstance().apply { timeInMillis = expense.timestamp }.get(Calendar.DAY_OF_MONTH)
                                    )
                                    _subscriptionSuggestions.update { currentList ->
                                        if (currentList.any { it.expense.id == expense.id }) currentList else currentList + suggestion
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // 🟢 升级 1：分级忽略 (修复点两次才关的 Bug)
    fun dismissSuggestion(context: Context, suggestion: SubscriptionSuggestion, ignoreMerchantForever: Boolean) {
        val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        if (ignoreMerchantForever) {
            val set = prefs.getStringSet("ignored_sub_merchants", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.add(suggestion.expense.merchant)
            prefs.edit().putStringSet("ignored_sub_merchants", set).apply()

            // 🟢 修复点 2：永久拉黑后，一键清空队列里【所有这个商家】的检测项
            _subscriptionSuggestions.update { list ->
                list.filter { it.expense.merchant != suggestion.expense.merchant }
            }
        } else {
            val set = prefs.getStringSet("ignored_expense_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            set.add(suggestion.expense.id.toString())
            prefs.edit().putStringSet("ignored_expense_ids", set).apply()

            // 仅忽略当前这笔账单
            _subscriptionSuggestions.update { list ->
                list.filter { it.expense.id != suggestion.expense.id }
            }
        }
    }

    // 🟢 升级 3：确认保存 (修复一直弹新窗口的 Bug)
    fun confirmSubscription(suggestion: SubscriptionSuggestion, finalDate: Int, finalAmount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionDao.insert(SubscriptionEntity(
                merchant = suggestion.expense.merchant,
                amount = finalAmount,
                category = suggestion.expense.category,
                billingDate = finalDate,
                accountName = suggestion.expense.accountName
            ))

            // 🟢 修复点 3：既然已经把这个商家加入订阅了，就一键清空队列里【所有这个商家】的检测项，不再烦你
            _subscriptionSuggestions.update { list ->
                list.filter { it.expense.merchant != suggestion.expense.merchant }
            }
        }
    }

    // 🟢 升级 1 附带：获取和移出黑名单
    fun getIgnoredMerchants(context: Context): List<String> {
        return context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            .getStringSet("ignored_sub_merchants", emptySet())?.toList()?.sorted() ?: emptyList()
    }

    fun removeIgnoredMerchant(context: Context, merchant: String) {
        val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("ignored_sub_merchants", emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(merchant)
        prefs.edit().putStringSet("ignored_sub_merchants", set).apply()
    }

    fun deleteSubscription(sub: SubscriptionEntity) {
        viewModelScope.launch(Dispatchers.IO) { subscriptionDao.delete(sub) }
    }
    fun deleteLearning(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { learningDao.deleteById(id) }
    }
    fun clearIgnoredLogs() {
        viewModelScope.launch(Dispatchers.IO) { ignoredDao.clearAll() }
    }
    fun addNewAccount(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank() && accountDao.exists(name) == 0)
                accountDao.insert(AccountEntity(name = name))
        }
    }
    fun deleteAccount(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name != "Cash") accountDao.deleteByName(name)
        }
    }

    fun adjustAccountBalance(accountName: String, actualBalance: Double) {
        viewModelScope.launch {
            val currentCalcBalance = accountBalances.first().find { it.name == accountName }?.balance ?: 0.0
            val diff = actualBalance - currentCalcBalance
            if (diff == 0.0) return@launch
            val type = if (diff > 0) "INCOME" else "EXPENSE"
            expenseDao.insert(ExpenseEntity(
                amount = Math.abs(diff),
                type = type,
                merchant = "System Adjustment",
                category = "Other",
                timestamp = System.currentTimeMillis(),
                originalText = "Manual Correction",
                note = "Adj: ${String.format("%.2f", currentCalcBalance)} -> ${String.format("%.2f", actualBalance)}",
                accountName = accountName
            ))
        }
    }

    val accountBalances = combine(_accounts, _rawExpenses) { accounts, expenses ->
        accounts.map { acc ->
            var bal = acc.initialBalance
            expenses.forEach { exp ->
                when (exp.type) {
                    "INCOME" -> if (exp.accountName == acc.name) bal += exp.amount
                    "EXPENSE" -> if (exp.accountName == acc.name) bal -= exp.amount
                    "TRANSFER" -> {
                        if (exp.accountName == acc.name) bal -= exp.amount
                        if (exp.targetAccountName == acc.name) bal += exp.amount
                    }
                }
            }
            AccountBalance(acc.name, bal)
        }
    }

    val filteredExpenses = combine(_rawExpenses, _currentMonth, _searchQuery) { expenses, monthTime, query ->
        val localizedExpenses = expenses.map { expense ->
            expense.copy(category = mapCategoryName(getApplication(), expense.category))
        }
        if (query.isBlank()) {
            val (start, end) = getMonthRange(monthTime)
            localizedExpenses.filter { it.timestamp in start..end }
        } else {
            localizedExpenses.filter {
                it.merchant.contains(query, true) ||
                        it.category.contains(query, true) ||
                        it.note.contains(query, true)
            }
        }
    }

    val expenseStats = combine(filteredExpenses, _statsType) { list, type ->
        val filteredList = list.filter { it.type == type }
        val totalAmount = filteredList.sumOf { it.amount }
        filteredList.groupBy { it.category }.map { (category, items) ->
            CategoryStat(
                category,
                items.sumOf { it.amount },
                if (totalAmount > 0) (items.sumOf { it.amount } / totalAmount).toFloat() else 0f
            )
        }.sortedByDescending { it.totalAmount }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun setStatsType(type: String) { _statsType.value = type }
    fun triggerEditFromNotification(id: Int) { _editRequest.value = id }
    fun clearEditRequest() { _editRequest.value = null }

    private fun mapCategoryName(context: Context, rawName: String): String {
        return when (rawName) {
            "饮食", "Food" -> context.getString(R.string.cat_food)
            "交通", "Transport" -> context.getString(R.string.cat_transport)
            "购物", "Shopping" -> context.getString(R.string.cat_shopping)
            "娱乐", "Entertainment" -> context.getString(R.string.cat_entertainment)
            "居家", "Home" -> context.getString(R.string.cat_home)
            "医疗", "Medical" -> context.getString(R.string.cat_medical)
            "其他", "Other" -> context.getString(R.string.cat_other)
            "薪水", "Salary" -> context.getString(R.string.cat_salary)
            "奖金", "Bonus" -> context.getString(R.string.cat_bonus)
            "津贴", "Allowance" -> context.getString(R.string.cat_allowance)
            "投资", "Investment" -> context.getString(R.string.cat_investment)
            "红包", "Red Packet" -> context.getString(R.string.cat_red_packet)
            else -> rawName
        }
    }

    private fun getMonthRange(timeInMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis - 1
        return Pair(start, end)
    }

    companion object {
        fun getTimeSlot(timestamp: Long): String {
            val h = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)
            return when (h) {
                in 5..10 -> "breakfast"
                in 11..14 -> "lunch"
                in 17..21 -> "dinner"
                in 22..23, in 0..4 -> "supper"
                else -> "other"
            }
        }
    }

    fun exportCsvForExcel(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expenses = _rawExpenses.first()
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write("\ufeffID,TIME,AMOUNT,MERCHANT,CATEGORY,TYPE,ACCOUNT,TARGET,NOTE\n")
                    expenses.forEach { e ->
                        // 🟢 格式化清洗：防止 CSV 破损
                        val safeNote = e.note.replace("\n", " | ").replace(",", "，").replace("\"", "'")
                        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(e.timestamp))

                        writer.write("${e.id},$timeStr,${e.amount},${e.merchant},${e.category},${e.type},${e.accountName},${e.targetAccountName?:""},\"$safeNote\"\n")
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 本地备份 (导出加密文件)
    fun backupData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 生成原始 JSON 字符串
                val jsonString = generateBackupJsonString(context)

                // 🟢 2. 加密 (Encrypt)
                // 将明文 JSON 变成乱码
                val encryptedData = EncryptionHelper.encrypt(jsonString)

                // 🟢 3. 写入文件 (写入的是加密后的乱码)
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(encryptedData.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Encrypted Backup Saved! 🔒", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 本地恢复 (读取加密文件)
    fun restoreData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 读取文件内容 (此时可能是乱码，也可能是旧版的明文)
                val fileContent = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() } ?: return@launch

                // 🟢 2. 尝试解密 (Decrypt)
                var finalJson = EncryptionHelper.decrypt(fileContent)

                // 🟢 3. 兼容性处理 (Legacy Support)
                // 如果解密结果为空(失败)，但文件内容看起来像是合法的 JSON (以 "{" 开头)，
                // 说明用户可能在恢复旧版本的明文备份，直接使用原内容。
                if (finalJson.isEmpty() && fileContent.trim().startsWith("{")) {
                    finalJson = fileContent
                }

                // 4. 执行恢复
                if (finalJson.isNotEmpty() && finalJson.trim().startsWith("{")) {
                    restoreFromJsonString(finalJson, context) // 这一步里包含了去重逻辑

                    withContext(Dispatchers.Main) {
                        refreshBudgets()
                        Toast.makeText(context, "Restored Successfully! 🎉", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 既解密失败，又不是明文 JSON，说明文件损坏或密钥错误
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restore Failed: Invalid File or Key 🚫", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
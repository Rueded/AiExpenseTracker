package com.example.aiexpensetracker.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.aiexpensetracker.MainActivity
import com.example.aiexpensetracker.R
import com.example.aiexpensetracker.database.AccountEntity
import com.example.aiexpensetracker.database.AppDatabase
import com.example.aiexpensetracker.database.ExpenseEntity
import com.example.aiexpensetracker.database.IgnoredEntity
import com.example.aiexpensetracker.network.AiProcessor
import com.example.aiexpensetracker.utils.VipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "AiListener"
        private const val ACTION_STOP_SERVICE = "com.example.aiexpensetracker.STOP_SERVICE"
        private const val PREFS_NAME = "ai_tracker_prefs"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"

        private const val MATCH_WINDOW_MS = 2 * 60 * 1000L
        private const val DEDUP_WINDOW_MS = 10_000L
        private const val NOTIFICATION_ID = 1001 // 🟢 新增：固定 ID 避免冲突

        private val dbMutex = kotlinx.coroutines.sync.Mutex()

        private val TARGET_PACKAGES = setOf(
            // ==========================================
            // 1. 你原有的包名 (原封不动保留)
            // ==========================================
            "my.com.tngdigital.ewallet",    // TNG eWallet
            "com.grabtaxi.passenger",       // Grab
            "com.shopee.my",                // Shopee
            "com.shopeepay.my",             // ShopeePay
            "my.com.myboost",               // Boost
            "com.airasia.bigpay",           // BigPay (旧版)
            "com.maybank2u.life",           // MAE
            "com.cimb.octo",                // CIMB Octo (可能无效)
            "com.cimb.clicks.android",      // CIMB Clicks (可能无效)
            "my.com.rhbgroup.mobilebanking",// RHB Old
            "my.com.rhbgroup.rhbmobilebanking",// RHB Old
            "com.rhbgroup.rhbengineering",  // RHB New
            "com.rhbgroup.rhbmobilebanking",// RHB New (准确的最新版)
            "com.hongleong.pb",             // HLB
            "my.com.mybsn",                 // BSN
            "com.mybsn.mobile",             // BSN
            "com.bsn.mybsn",                // BSN (准确)
            "net.mybsn.secure",             // BSN
            "com.ambank.ambank",            // AmBank
            "my.com.publicbank.pbe",        // Public Bank
            "com.alliancebank.allianceonline", // Alliance
            "com.bankislam.go",             // Bank Islam
            "com.bankrakyat.irakyat",       // Bank Rakyat
            "com.sc.breeze.my",             // Standard Chartered
            "my.com.hsbc.hsbcmobilebanking",// HSBC
            "com.ocbc.mobile",              // OCBC
            "com.uob.mighty.my",            // UOB (旧版 Mighty)

            // ==========================================
            // 2. 🟢 新增：传统银行 (修正后的最新准确包名)
            // ==========================================
            "com.maybank2u.m2u",            // Maybank2u (旧版黄App，很多人还在用)
            "com.cimb.cimbocto",            // CIMB OCTO MY (目前真正的包名)
            "com.cimbmalaysia",             // CIMB Clicks Malaysia (目前真正的包名)
            "my.com.rhb.mobilebanking",     // RHB Mobile Banking (常见旧版)
            "my.com.hongleongconnect.mobile",// HLB Connect (目前真正的包名)
            "com.publicbank.pbengage",      // PB engage MY (目前真正的包名)
            "com.ambank.ambankonline",      // AmOnline (目前真正的包名)
            "com.ambank.amonline",          // AmOnline (备用)
            "com.alliance.online.mobile",   // Alliance online mobile (目前真正的包名)
            "com.bankislam.bimbmobile",     // Bank Islam GO (目前真正的包名)
            "my.com.bankrakyat.irakyat",    // iRakyat Mobile Banking
            "com.affinbank.affinalways",    // AffinAlways (Affin Bank 新版)
            "com.affinonline.rib",          // Affin Bank (旧版)
            "com.uob.tmrw.my",              // UOB TMRW Malaysia (取代了 Mighty)
            "com.ocbc.my",                  // OCBC Malaysia
            "com.ocbc.mobilebanking.my",    // OCBC Malaysia (备用)
            "hk.com.hsbc.hsbcmalaysia",     // HSBC Malaysia (本地版)
            "com.htsu.hsbcpersonalbanking", // HSBC (全球通用版)
            "com.standardchartered.breeze.my", // SC Mobile (备用)

            // ==========================================
            // 3. 🟣 新增：最新数字银行 (Digital Banks - 记账必备)
            // ==========================================
            "my.gxbank.my",                 // GXBank (目前最火的数字银行)
            "my.com.aeonbank.app",          // AEON Bank (伊斯兰数字银行)
            "com.bankislam.beu",            // Be U by Bank Islam
            "com.alrajhi.rize",             // Rize (Al Rajhi Bank 数字银行)

            // ==========================================
            // 4. 🟡 新增：电子钱包与其他生活支付 (E-Wallets)
            // ==========================================
            "com.tpa.airasiacard",          // BigPay (目前真正的包名，极其重要)
            "com.setel.mobile",             // Setel (Petronas 打油必备)
            "com.aeoncredit.wallet.my",     // AEON Wallet Malaysia
            "com.lazada.android",           // Lazada (Lazada Wallet 扣款)
            "com.google.android.apps.walletnfcrel", // Google Wallet (NFC 刷卡通知)
            "com.samsung.android.spay",     // Samsung Pay
            "com.paypal.android.p2pmobile", // PayPal
            "com.transferwise.android",     // Wise (跨国汇款/支付)
            "com.eg.android.AlipayGphone"   // Alipay (支付宝，如果你有时用的话)
        )
    }

    private var isServiceRunning = true
    private var lastProcessedText: String = ""
    private var lastPostTime: Long = 0L

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_SERVICE) {
                context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.edit()?.putBoolean(KEY_TRACKING_ENABLED, false)?.apply()
                showToast(getString(R.string.service_paused_desc))
                updateNotification(isPaused = true)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return Service.START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, NotificationListener::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        showToast(getString(R.string.service_started_toast))
        startForegroundService()
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_STOP_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, stopReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stopReceiver) } catch (e: Exception) {}
    }

    private fun startForegroundService() { updateNotification(isPaused = false) }

    // 🟢 修改 4：核心通知逻辑 (划不掉 + Android 14 兼容)
    private fun updateNotification(isPaused: Boolean) {
        isServiceRunning = true
        val channelId = "expense_service_v5"
        val channelName = getString(R.string.service_channel_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(ACTION_STOP_SERVICE).apply { setPackage(packageName) }
        val stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val contentText = if (isPaused) getString(R.string.service_paused_desc) else getString(R.string.service_listening)
        val actionText = if (isPaused) getString(R.string.service_btn_paused) else getString(R.string.service_btn_pause)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            // 🟢 关键：设置为 ONGOING 用户划不掉
            .setOngoing(true)
            // 🟢 关键：Android 14 要求前台服务通知立即显示，不等待系统调度
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply { if (!isPaused) addAction(android.R.drawable.ic_media_pause, actionText, stopPendingIntent) }
            .build()

        try {
            // 🟢 关键：Android 14 (API 34) 必须显式声明前台服务类型
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot start foreground service: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_TRACKING_ENABLED, true)) return
        if (sbn == null) return

        val currentPostTime = sbn.postTime
        val packageName = sbn.packageName
        if (!TARGET_PACKAGES.contains(packageName)) return

        val extras = sbn.notification.extras

        var title = extras.getString(android.app.Notification.EXTRA_TITLE)
            ?: extras.getString(android.app.Notification.EXTRA_TITLE_BIG)
            ?: ""
        var text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(android.app.Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?: ""

        if (text.isBlank()) text = sbn.notification.tickerText?.toString() ?: ""
        if (title.isBlank()) title = detectAccountName(packageName)

        val fullContent = "$title $text".uppercase().replace("\\s+".toRegex(), "")

        if (fullContent == lastProcessedText && (currentPostTime - lastPostTime) < DEDUP_WINDOW_MS) {
            Log.d("AutoDelivery", "🛑 10秒内重复通知，已拦截")
            return
        }
        lastProcessedText = fullContent
        lastPostTime = currentPostTime

        val currentTime = System.currentTimeMillis()

        // ==========================================
        // 🚀 VIP 自动发卡雷达（终极版）
        // ==========================================
        val amountRegex = Regex("RM\\s*(\\d+(\\.\\d{1,2})?)")
        val amountMatch = amountRegex.find(fullContent)
        val earnedAmount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        val vipPrices = setOf(9.90, 89.90, 199.00)
        val isVipPrice = vipPrices.any { Math.abs(earnedAmount - it) < 0.001 }

        val isIncoming = fullContent.contains("RECEIVED") ||
                fullContent.contains("TOYOU") ||
                fullContent.contains("CREDITED") ||
                fullContent.contains("FROM") ||
                fullContent.contains("收款") ||  // 🚀 中文收入
                fullContent.contains("入账") ||
                fullContent.contains("收到")

        val isOutgoing = fullContent.contains("YOU'VETRANSFERRED") ||
                fullContent.contains("PAID") ||
                fullContent.contains("SPENT") ||
                fullContent.contains("PAYMENTTO") ||
                fullContent.contains("付款") ||  // 🚀 中文支出
                fullContent.contains("转出") ||
                fullContent.contains("支付")

        if (isVipPrice && isIncoming && !isOutgoing) {
            Log.d("AutoDelivery", "🎯 嗅探到真实的 VIP 商业收款: $fullContent")

            val database = com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://ai-expense-tracker-0-default-rtdb.asia-southeast1.firebasedatabase.app/"
            )
            database.goOnline()

            val receiptData = mapOf(
                "text" to fullContent,
                "timestamp" to currentTime,
                "claimed" to false
            )
            database.getReference("global_payments/history").push().setValue(receiptData)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val expenseDao = db.expenseDao()
                    val detectedAccount = detectAccountName(packageName)

                    val vipIncome = ExpenseEntity(
                        amount = earnedAmount,
                        type = "INCOME",
                        merchant = "VIP Subscription",
                        category = "Business / 商业收入",
                        timestamp = currentTime,
                        originalText = "Auto-detected VIP Payment: $fullContent",
                        note = "App 自动发货收款",
                        accountName = detectedAccount,
                        targetAccountName = null
                    )

                    dbMutex.withLock {
                        expenseDao.insert(vipIncome)
                    }
                    Log.d("AutoDelivery", "💰 商业收入已写入！金额: RM$earnedAmount")

                } catch (e: Exception) {
                    Log.e("AutoDelivery", "记账失败", e)
                }
            }
            return
        }
        // ==========================================

        if (isSensitive(text) || isSensitive(title)) return

        // ✅ 本地保安：关键词足够丰富，通过了就直接送 AI 提取，不再让 AI 当门神
        if (!isValidTransaction(title, text)) {
            Log.d(TAG, "🗑️ Ignored spam: $title")
            saveIgnoredLog(packageName, title, text, "Keyword Filter (Spam/Ad)")
            return
        }

        Log.e(TAG, "📨 Captured: $packageName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fullText = "App: $packageName. Title: $title. Message: $text"
                val aiConfig = getAiConfigAndCheckQuota(applicationContext)

                if (!aiConfig.isAllowed) {
                    Log.w(TAG, "🚫 AI Quota exceeded for today.")
                    saveIgnoredLog(packageName, title, text, "Daily AI Limit Reached (Need VIP)")
                    sendQuotaExceededNotification()
                    return@launch
                }

                Log.e(TAG, "🤖 Asking Gemini AI...")
                val result = AiProcessor.analyze(fullText, aiConfig.customKey, aiConfig.customModel)

                // ✅ 剥夺 AI 拒签权：不看 result.valid，只要 amount > 0 就入库
                if (result != null && (result.amount ?: 0.0) > 0.0) {
                    dbMutex.withLock {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val learningDao = db.merchantLearningDao()
                        val expenseDao = db.expenseDao()
                        val accountDao = db.accountDao()

                        val detectedAccount = detectAccountName(packageName)
                        if (accountDao.exists(detectedAccount) == 0) {
                            Log.d(TAG, "🆕 Creating new account: $detectedAccount")
                            accountDao.insert(AccountEntity(name = detectedAccount))
                        }

                        val timeSlot = getTimeSlot(currentTime)
                        val merchantName = result.merchant ?: "Unknown"
                        val memory = if (merchantName != "Unknown") learningDao.getLearning(merchantName, timeSlot) else null

                        val autoCategory = memory?.category ?: result.category ?: "Uncategorized"
                        val autoNote = memory?.note ?: ""

                        val amount = result.amount ?: 0.0
                        var currentType = result.type ?: "EXPENSE"
                        var currentTarget: String? = null
                        var currentNote = autoNote

                        val startTime = currentTime - MATCH_WINDOW_MS
                        val endTime = currentTime + MATCH_WINDOW_MS

                        var isMerged = false
                        var transferId = 0

                        if (currentType == "INCOME") {
                            val match = expenseDao.findMatchingTransaction(amount, "EXPENSE", startTime, endTime)
                            if (match != null && match.accountName != detectedAccount) {
                                Log.e(TAG, "🔗 Auto-Merge (Income): Transfer from ${match.accountName} to $detectedAccount")
                                expenseDao.convertToTransfer(match.id, detectedAccount)
                                isMerged = true
                                transferId = match.id
                            }
                        } else if (currentType == "EXPENSE") {
                            val match = expenseDao.findMatchingTransaction(amount, "INCOME", startTime, endTime)
                            if (match != null &&
                                match.merchant != "VIP Subscription" &&
                                match.accountName != detectedAccount) {
                                Log.e(TAG, "🔗 Auto-Merge (Expense): Transfer from $detectedAccount to ${match.accountName}")
                                expenseDao.delete(match)
                                currentType = "TRANSFER"
                                currentTarget = match.accountName
                                if (currentNote.isBlank()) currentNote = "Auto-merged Transfer"
                                isMerged = false
                            }
                        }

                        if (isMerged) {
                            val msg = getString(R.string.notif_saved_title, "Transfer RM ${String.format("%.2f", amount)}")
                            showToast(msg)
                            sendInputRequiredNotification(transferId, amount, "Transfer", "Auto-merged")
                        } else {
                            val newRecord = ExpenseEntity(
                                amount = amount,
                                type = currentType,
                                merchant = merchantName,
                                category = autoCategory,
                                timestamp = currentTime,
                                originalText = text,
                                note = currentNote,
                                accountName = detectedAccount,
                                targetAccountName = currentTarget
                            )
                            val id = expenseDao.insert(newRecord)

                            val savedMsg = getString(R.string.notif_saved_title, "RM ${String.format("%.2f", newRecord.amount)}")
                            showToast(savedMsg)
                            sendInputRequiredNotification(id.toInt(), newRecord.amount, newRecord.merchant, currentNote)
                        }
                    } // 🔒 锁结束
                } else {
                    Log.d(TAG, "🤖 AI returned null or zero amount")
                    saveIgnoredLog(packageName, title, text, "AI Extraction Failed (null/zero amount)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun saveIgnoredLog(pkg: String, title: String, text: String, reason: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.ignoredDao().insert(
                    IgnoredEntity(packageName = pkg, title = title, text = text, reason = reason)
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun detectAccountName(pkg: String): String {
        return when (pkg) {
            "my.com.tngdigital.ewallet" -> "TNG eWallet"
            "com.grabtaxi.passenger" -> "GrabPay"
            "com.shopee.my" -> "ShopeePay"
            "com.maybank2u.life" -> "Maybank"
            "com.cimb.octo", "com.cimb.clicks.android" -> "CIMB"
            "my.com.rhbgroup.mobilebanking",
            "com.rhbgroup.rhbengineering",
            "com.rhbgroup.rhbmobilebanking" -> "RHB"
            "com.hongleong.pb" -> "Hong Leong"
            "my.com.publicbank.pbe" -> "Public Bank"
            "my.com.mybsn", "com.mybsn.mobile", "net.mybsn.secure" -> "BSN"
            "com.ambank.ambank" -> "AmBank"
            "com.airasia.bigpay" -> "BigPay"
            "com.bankislam.go" -> "Bank Islam"
            else -> "Cash"
        }
    }

    // ✅ 本地保安（关键词超丰富版）：正则当盾，AI 当矛
    private fun isValidTransaction(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()

        // 🚀 货币不仅要认 RM/MYR，还要防一手中文的“令吉”或“马币”
        val hasCurrency = combined.contains("rm") || combined.contains("myr") ||
                combined.contains("令吉") || combined.contains("马币")
        if (!hasCurrency) return false

        // 🚀 终极加强版：英语 + 马来语 + 中文（简/繁）
        val validKeywords = listOf(
            // English
            "paid", "spent", "transfer", "transferred", "has transferred",
            "you've transferred", "sent", "received", "funds received",
            "you've received", "to your account", "into your account",
            "credit", "credited", "debit", "debited", "deducted",
            "payment", "purchase", "transaction", "inward", "outward",
            "top up", "topup", "reload", "reloaded",
            "duitnow", "fpx", "jompay", "paywave", "interbank", "ibg", "rentas", "swift",
            "succes", "successful", "approved", "confirmed",
            "has been debited", "has been credited", "account ending", "your account", "bank account",
            // Bahasa Melayu
            "pembayaran", "pindahan", "diterima", "dibayar", "ditolak", "bayaran",
            "masuk", "keluar", "telah berjaya", "berjaya",
            // 中文 (简体 & 繁体)
            "转账", "轉賬", "支付", "收款", "汇款", "匯款", "扣款", "充值", "退款",
            "支出", "收入", "成功", "入账", "入賬", "付款", "已扣除", "转出", "轉出"
        )
        val hasValidKeyword = validKeywords.any { combined.contains(it) }

        val invalidKeywords = listOf(
            // English
            "promo", "cashback", "voucher", "discount", "winner", "win", "GO+",
            "campaign", "apply now", "deal", "login", "tac", "otp",
            "unsuccessful", "failed", "declined", "rejected",
            "password", "pin", "security", "reminder", // 移除了 alert
            "statement", "e-statement", "monthly statement",
            "maintenance", "system update", "downtime",
            "congratulations", "lucky draw", "due date", // 移除了 loan, balance, outstanding
            // Bahasa Melayu
            "tahniah", // 移除了 baki (balance的马来文)
            // 中文 (简体 & 繁体)
            "验证码", "驗證碼", "登录", "登入", "密码", "密碼", "失败", "失敗",
            "优惠", "優惠", "折扣", "抽奖", "抽獎", "赢取", "贏取",
            "推广", "推廣", "提醒", "账单", "賬單", "拒绝", "拒絶",
            "安全", "警告", "维护", "維護", "更新", "逾期" // 移除了 余额, 餘額
        )

        val hasInvalidKeyword = invalidKeywords.any { combined.contains(it) }

        return hasValidKeyword && !hasInvalidKeyword
    }

    private fun getTimeSlot(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 17..21 -> "dinner"
            in 22..23, in 0..4 -> "supper"
            else -> "other"
        }
    }

    private fun sendInputRequiredNotification(expenseId: Int, amount: Double, merchant: String, autoNote: String) {
        val channelId = "expense_input_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Transaction Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EDIT_EXPENSE_ID", expenseId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, expenseId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isAutoMatched = autoNote.isNotBlank() && merchant != "Unknown"
        val titleText = if (isAutoMatched) "✅ Saved: RM ${String.format("%.2f", amount)}"
        else "📝 Saved: RM ${String.format("%.2f", amount)}"
        val contentText = if (isAutoMatched) "$merchant • $autoNote (Tap to edit)"
        else "$merchant • Tap to add details"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setPriority(if (isAutoMatched) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(expenseId, notification)
    }

    private fun isSensitive(content: String): Boolean {
        val lower = content.lowercase()
        return lower.contains("tac") || lower.contains("otp") ||
                lower.contains("code") || lower.contains("login") || lower.contains("verify")
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    data class AiConfig(val isAllowed: Boolean, val customKey: String?, val customModel: String?)

    private suspend fun getAiConfigAndCheckQuota(context: Context): AiConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isVip = VipUtils.isUserVip(context)
        val customKey = prefs.getString("custom_api_key", "") ?: ""
        val customModel = prefs.getString("custom_model_name", "") ?: ""

        if (isVip || (customKey.isNotBlank() && customModel.isNotBlank())) {
            return AiConfig(true, customKey.takeIf { it.isNotBlank() }, customModel.takeIf { it.isNotBlank() })
        }

        val today = AiProcessor.getNetworkDate()
        val lastDate = prefs.getString("last_ai_usage_date", "")
        var usage = prefs.getInt("daily_ai_usage", 0)

        if (today != lastDate) {
            usage = 0
            prefs.edit().putString("last_ai_usage_date", today).apply()
        }

        return if (usage >= 5) {
            AiConfig(false, null, null)
        } else {
            prefs.edit().putInt("daily_ai_usage", usage + 1).apply()
            AiConfig(true, null, null)
        }
    }

    private fun sendQuotaExceededNotification() {
        val channelId = "expense_input_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ AI Limit Reached / AI 额度已用完")
            .setContentText("A transaction was ignored. Tap to upgrade VIP or add your API Key.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(999, notification)
    }

}
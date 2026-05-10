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
import com.example.aiexpensetracker.network.TransactionResult
import com.example.aiexpensetracker.utils.VipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        private const val NOTIFICATION_ID = 1001

        private val dbMutex = kotlinx.coroutines.sync.Mutex()

        private val TARGET_PACKAGES = setOf(
            "my.com.tngdigital.ewallet",
            "com.grabtaxi.passenger",
            "com.shopee.my",
            "com.shopeepay.my",
            "my.com.myboost",
            "com.airasia.bigpay",
            "com.maybank2u.life",
            "com.cimb.octo",
            "com.cimb.clicks.android",
            "my.com.rhbgroup.mobilebanking",
            "my.com.rhbgroup.rhbmobilebanking",
            "com.rhbgroup.rhbengineering",
            "com.rhbgroup.rhbmobilebanking",
            "com.hongleong.pb",
            "my.com.mybsn",
            "com.mybsn.mobile",
            "com.bsn.mybsn",
            "net.mybsn.secure",
            "com.ambank.ambank",
            "my.com.publicbank.pbe",
            "com.alliancebank.allianceonline",
            "com.bankislam.go",
            "com.bankrakyat.irakyat",
            "com.sc.breeze.my",
            "my.com.hsbc.hsbcmobilebanking",
            "com.ocbc.mobile",
            "com.uob.mighty.my",
            "com.maybank2u.m2u",
            "com.cimb.cimbocto",
            "com.cimbmalaysia",
            "my.com.rhb.mobilebanking",
            "my.com.hongleongconnect.mobile",
            "com.publicbank.pbengage",
            "com.ambank.ambankonline",
            "com.ambank.amonline",
            "com.alliance.online.mobile",
            "com.bankislam.bimbmobile",
            "my.com.bankrakyat.irakyat",
            "com.affinbank.affinalways",
            "com.affinonline.rib",
            "com.uob.tmrw.my",
            "com.ocbc.my",
            "com.ocbc.mobilebanking.my",
            "hk.com.hsbc.hsbcmalaysia",
            "com.htsu.hsbcpersonalbanking",
            "com.standardchartered.breeze.my",
            "my.gxbank.my",
            "my.com.aeonbank.app",
            "com.bankislam.beu",
            "com.alrajhi.rize",
            "com.tpa.airasiacard",
            "com.setel.mobile",
            "com.aeoncredit.wallet.my",
            "com.lazada.android",
            "com.google.android.apps.walletnfcrel",
            "com.samsung.android.spay",
            "com.paypal.android.p2pmobile",
            "com.transferwise.android",
            "com.eg.android.AlipayGphone"
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
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply { if (!isPaused) addAction(android.R.drawable.ic_media_pause, actionText, stopPendingIntent) }
            .build()

        try {
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
        // VIP 自动发卡雷达
        // ==========================================
        val amountRegex = Regex("RM\\s*(\\d+(\\.\\d{1,2})?)")
        val amountMatch = amountRegex.find(fullContent)
        val earnedAmount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        val vipPrices = setOf(9.90, 89.90, 199.00)
        val isVipPrice = vipPrices.any { Math.abs(earnedAmount - it) < 0.001 }

        val isIncoming = fullContent.containsAny("RECEIVED", "TOYOU", "CREDITED", "FROM",
            "收款", "入账", "收到")
        val isOutgoing = fullContent.containsAny("YOU'VETRANSFERRED", "PAID", "SPENT",
            "PAYMENTTO", "付款", "转出", "支付")

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
                        category = getString(R.string.category_business_income),
                        timestamp = currentTime,
                        originalText = "Auto-detected VIP Payment: $fullContent",
                        note = getString(R.string.note_auto_vip_income),
                        accountName = detectedAccount,
                        targetAccountName = null
                    )

                    dbMutex.withLock { expenseDao.insert(vipIncome) }
                    Log.d("AutoDelivery", "💰 商业收入已写入！金额: RM$earnedAmount")

                } catch (e: Exception) {
                    Log.e("AutoDelivery", "记账失败", e)
                }
            }
            return
        }
        // ==========================================

        if (isSensitive(text) || isSensitive(title)) return

        // ✅ 第一层：评分制过滤器
        val filterScore = calcFilterScore(title, text)
        if (filterScore < 3) {
            Log.d(TAG, "🗑️ Score=$filterScore, ignored: $title")
            saveIgnoredLog(packageName, title, text, "Score Filter (score=$filterScore)")
            return
        }

        Log.e(TAG, "📨 Captured (score=$filterScore): $packageName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fullText = "App: $packageName. Title: $title. Message: $text"
                val aiConfig = getAiConfigAndCheckQuota(applicationContext)

                if (!aiConfig.isAllowed) {
                    Log.w(TAG, "🚫 AI Quota exceeded for today.")
                    // 配额耗尽时也尝试本地兜底，不直接丢弃
                    val fallback = localRegexFallback(title, text, packageName)
                    if (fallback != null) {
                        saveFallbackRecord(fallback, text, packageName, currentTime, "AI Quota Exceeded")
                    } else {
                        saveIgnoredLog(packageName, title, text, "Daily AI Limit Reached (Need VIP)")
                        sendQuotaExceededNotification()
                    }
                    return@launch
                }

                // ✅ 第二层：AI 提取（带重试）
                Log.e(TAG, "🤖 Asking AI (with retry)...")
                val result = retryAiExtraction(fullText, aiConfig, maxRetries = 2)

                if (result != null && (result.amount ?: 0.0) > 0.0) {
                    // AI 成功，正常入库
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
                                if (currentNote.isBlank()) currentNote = getString(R.string.note_auto_merged_transfer)
                                isMerged = false
                            }
                        }

                        if (isMerged) {
                            val msg = getString(R.string.notif_saved_title, "Transfer RM ${String.format("%.2f", amount)}")
                            showToast(msg)
                            sendInputRequiredNotification(transferId, amount, "Transfer", getString(R.string.note_auto_merged))
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
                    }
                } else {
                    // ✅ 第三层：本地 Regex 兜底
                    Log.w(TAG, "🤖 AI failed after retries, trying local fallback...")
                    val fallback = localRegexFallback(title, text, packageName)
                    if (fallback != null) {
                        saveFallbackRecord(fallback, text, packageName, currentTime, "AI Failed")
                    } else {
                        Log.d(TAG, "🗑️ Both AI and fallback failed")
                        saveIgnoredLog(packageName, title, text, "AI + Regex Fallback both failed")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // 第二层：AI 重试机制
    // ==========================================
    private suspend fun retryAiExtraction(
        fullText: String,
        aiConfig: AiConfig,
        maxRetries: Int = 2
    ): TransactionResult? {
        repeat(maxRetries) { attempt ->
            try {
                val result = AiProcessor.analyze(fullText, aiConfig.customKey, aiConfig.customModel)
                if (result != null && (result.amount ?: 0.0) > 0.0) {
                    Log.d(TAG, "✅ AI success on attempt ${attempt + 1}")
                    return result
                }
                Log.w(TAG, "⚠️ AI attempt ${attempt + 1} returned null/zero, retrying...")
                if (attempt < maxRetries - 1) delay(1500L)
            } catch (e: Exception) {
                Log.e(TAG, "💥 AI attempt ${attempt + 1} threw: ${e.message}")
                if (attempt < maxRetries - 1) delay(1500L)
            }
        }
        return null
    }

    // ==========================================
    // 第三层：本地 Regex 兜底提取
    // ==========================================
    private fun localRegexFallback(title: String, text: String, pkg: String): TransactionResult? {
        val combined = "$title $text"

        // 提取金额（支持逗号千位分隔符）
        val amountRegex = Regex("""RM\s*(\d{1,6}(?:,\d{3})*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val amountStr = amountRegex.find(combined)?.groupValues?.get(1)
            ?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val upper = combined.uppercase()
        val type = when {
            upper.containsAny("RECEIVED", "CREDITED", "INWARD", "TO YOU",
                "FUNDS RECEIVED", "INTO YOUR ACCOUNT", "收款", "入账", "收到") -> "INCOME"
            else -> "EXPENSE"
        }

        Log.d(TAG, "🛠️ Regex fallback extracted: RM$amount ($type)")
        return TransactionResult(
            valid = true,
            amount = amount,
            merchant = "Unknown",
            category = "Uncategorized",
            type = type
        )
    }

    // ==========================================
    // 兜底记录写入（标注待审核，存入账单列表）
    // ==========================================
    private suspend fun saveFallbackRecord(
        fallback: TransactionResult,
        originalText: String,
        packageName: String,
        currentTime: Long,
        reason: String
    ) {
        dbMutex.withLock {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val expenseDao = db.expenseDao()
                val accountDao = db.accountDao()
                val detectedAccount = detectAccountName(packageName)

                if (accountDao.exists(detectedAccount) == 0) {
                    accountDao.insert(AccountEntity(name = detectedAccount))
                }

                val record = ExpenseEntity(
                    amount = fallback.amount ?: 0.0,
                    type = fallback.type ?: "EXPENSE",
                    merchant = "Unknown",
                    category = "Uncategorized",
                    timestamp = currentTime,
                    originalText = originalText,
                    note = getString(R.string.note_pending_review, reason),
                    accountName = detectedAccount,
                    targetAccountName = null
                )
                val id = expenseDao.insert(record)
                Log.d(TAG, "🛠️ Fallback record saved, id=$id, amount=${fallback.amount}")

                showToast(getString(R.string.toast_pending_review, String.format("%.2f", fallback.amount)))
                sendInputRequiredNotification(
                    id.toInt(),
                    fallback.amount ?: 0.0,
                    getString(R.string.merchant_pending_review),
                    getString(R.string.notif_tap_to_confirm)
                )
            } catch (e: Exception) {
                Log.e(TAG, "💥 Fallback save failed: ${e.message}")
            }
        }
    }

    // ==========================================
    // 第一层：评分制过滤器（替换原 isValidTransaction）
    // ==========================================
    private fun calcFilterScore(title: String, text: String): Int {
        val combined = "$title $text".lowercase()
        var score = 0

        // 没有货币符号直接拦截
        val hasCurrency = combined.containsAny("rm", "myr", "令吉", "马币")
        if (!hasCurrency) return -99

        // 加分项
        val highPositive = listOf(
            "paid", "transferred", "you've transferred", "has been debited",
            "deducted", "purchase", "payment to", "duitnow", "fpx", "jompay", "paywave", "ibg"
        )
        val medPositive = listOf(
            "received", "credited", "to your account", "funds received",
            "top up", "topup", "reload", "收款", "入账", "转账", "付款", "充值"
        )
        val lowPositive = listOf(
            "successful", "approved", "berjaya", "成功", "telah berjaya"
        )

        if (highPositive.any { combined.contains(it) }) score += 3
        if (medPositive.any { combined.contains(it) }) score += 3
        if (lowPositive.any { combined.contains(it) }) score += 2

        // 扣分项
        val criticalNegative = listOf("tac", "otp", "verification code", "验证码", "驗證碼")
        val highNegative = listOf(
            "unsuccessful", "failed", "declined", "rejected", "失败", "失敗", "拒绝", "拒絕"
        )
        val medNegative = listOf(
            "promo", "voucher", "cashback offer", "lucky draw", "winner",
            "抽奖", "抽獎", "优惠券", "優惠券", "campaign"
        )
        val lowNegative = listOf(
            "maintenance", "downtime", "system update", "monthly statement",
            "e-statement", "维护", "維護", "账单提醒", "賬單提醒"
        )

        if (criticalNegative.any { combined.contains(it) }) score -= 10  // 一票否决
        if (highNegative.any { combined.contains(it) }) score -= 8
        if (medNegative.any { combined.contains(it) }) score -= 5
        if (lowNegative.any { combined.contains(it) }) score -= 4

        Log.d(TAG, "🎯 FilterScore=$score | $combined")
        return score
    }

    // ==========================================
    // 工具方法
    // ==========================================
    private fun String.containsAny(vararg keywords: String) =
        keywords.any { this.contains(it, ignoreCase = true) }

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

        val isAutoMatched = autoNote.isNotBlank() && merchant != "Unknown" && merchant != "待审核"
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
            .setContentTitle(getString(R.string.notif_quota_title))
            .setContentText(getString(R.string.notif_quota_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(999, notification)
    }
}
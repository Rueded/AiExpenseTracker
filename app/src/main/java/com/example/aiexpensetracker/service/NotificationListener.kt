package com.example.aiexpensetracker.service

import android.app.AlarmManager
import android.app.Notification
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
import com.example.aiexpensetracker.database.IgnoredEntity // 🟢 记得导入
import com.example.aiexpensetracker.network.AiProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "AiListener"
        private const val ACTION_STOP_SERVICE = "com.example.aiexpensetracker.STOP_SERVICE"
        private const val PREFS_NAME = "ai_tracker_prefs"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"

        // 匹配时间窗：2分钟
        private const val MATCH_WINDOW_MS = 2 * 60 * 1000L

        private val TARGET_PACKAGES = setOf(
            "my.com.tngdigital.ewallet",    // TNG eWallet
            "com.grabtaxi.passenger",       // Grab
            "com.shopee.my",                // Shopee
            "my.com.myboost",               // Boost
            "com.airasia.bigpay",           // BigPay
            "com.maybank2u.life",           // MAE
            "com.cimb.octo",                // CIMB Octo
            "com.cimb.clicks.android",      // CIMB Clicks
            "my.com.rhbgroup.mobilebanking",// RHB Old
            "com.rhbgroup.rhbengineering",  // RHB New
            "com.hongleong.pb",             // HLB
            "my.com.mybsn",                 // BSN
            "com.mybsn.mobile",             // BSN
            "net.mybsn.secure",             // BSN
            "com.ambank.ambank",            // AmBank
            "my.com.publicbank.pbe",        // Public Bank
            "com.alliancebank.allianceonline", // Alliance
            "com.bankislam.go",             // Bank Islam
            "com.bankrakyat.irakyat",       // Bank Rakyat
            "com.sc.breeze.my",             // Standard Chartered
            "my.com.hsbc.hsbcmobilebanking",// HSBC
            "com.ocbc.mobile",              // OCBC
            "com.uob.mighty.my"             // UOB
        )
    }

    private var isServiceRunning = true

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
        super.onStartCommand(intent, flags, startId)
        startForegroundService()
        return Service.START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, NotificationListener::class.java).also { it.setPackage(packageName) }
        val restartServicePendingIntent = PendingIntent.getService(applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply { if (!isPaused) addAction(android.R.drawable.ic_media_pause, actionText, stopPendingIntent) }
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) { Log.e(TAG, "Cannot start foreground service: ${e.message}") }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_TRACKING_ENABLED, true)) return
        if (sbn == null) return
        val packageName = sbn.packageName
        if (!TARGET_PACKAGES.contains(packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (isSensitive(text) || isSensitive(title)) return

        // 🟢 1. 记录本地正则拦截 (Keyword Filter)
        if (!isValidTransaction(title, text)) {
            Log.d(TAG, "🗑️ Ignored spam: $title")
            saveIgnoredLog(packageName, title, text, "Keyword Filter (Spam/Ad)")
            return
        }

        Log.e(TAG, "📨 Captured: $packageName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fullText = "App: $packageName. Title: $title. Message: $text"
                Log.e(TAG, "🤖 Asking Gemini AI...")

                val result = AiProcessor.analyze(fullText)

                if (result != null && result.valid) {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val learningDao = db.merchantLearningDao()
                    val expenseDao = db.expenseDao()
                    val accountDao = db.accountDao()

                    val currentTime = System.currentTimeMillis()

                    // 1. Detect & Auto-Create Account
                    val detectedAccount = detectAccountName(packageName)
                    if (accountDao.exists(detectedAccount) == 0) {
                        Log.d(TAG, "🆕 Creating new account: $detectedAccount")
                        accountDao.insert(AccountEntity(name = detectedAccount))
                    }

                    // 2. Memory Retrieval
                    val timeSlot = getTimeSlot(currentTime)
                    val merchantName = result.merchant ?: "Unknown"
                    val memory = if (merchantName != "Unknown") learningDao.getLearning(merchantName, timeSlot) else null

                    val autoCategory = memory?.category ?: result.category ?: "Uncategorized"
                    val autoNote = memory?.note ?: ""

                    // 🟢 3. 智能转账合并逻辑 (Smart Auto-Merge)
                    val amount = result.amount ?: 0.0
                    var currentType = result.type ?: "EXPENSE"
                    var currentTarget: String? = null
                    var currentNote = autoNote

                    val startTime = currentTime - MATCH_WINDOW_MS
                    val endTime = currentTime + MATCH_WINDOW_MS

                    var isMerged = false // 是否已完成合并（不需要插入新记录）
                    var transferId = 0   // 用于通知跳转

                    if (currentType == "INCOME") {
                        // 场景 A: 收到【收入】。去查之前有没有【支出】?
                        // 如果有，说明是别人转给我的，或者是我的另一个号转过来的。
                        val match = expenseDao.findMatchingTransaction(amount, "EXPENSE", startTime, endTime)
                        if (match != null && match.accountName != detectedAccount) {
                            Log.e(TAG, "🔗 Auto-Merge (Income Trigger): Transfer from ${match.accountName} to $detectedAccount")

                            // 动作：修改那笔旧的 Expense，变成 Transfer
                            expenseDao.convertToTransfer(match.id, detectedAccount)

                            // 标记：已合并，不需要再存这笔 Income 了
                            isMerged = true
                            transferId = match.id
                        }
                    } else if (currentType == "EXPENSE") {
                        // 场景 B: 收到【支出】。去查之前有没有【收入】?
                        // (这种情况较少见，通常是银行扣款慢了，钱包入账快了)
                        val match = expenseDao.findMatchingTransaction(amount, "INCOME", startTime, endTime)
                        if (match != null && match.accountName != detectedAccount) {
                            Log.e(TAG, "🔗 Auto-Merge (Expense Trigger): Transfer from $detectedAccount to ${match.accountName}")

                            // 动作 1：删除那笔旧的 Income (因为它现在被视为这笔 Transfer 的终点)
                            expenseDao.delete(match)

                            // 动作 2：把当前这笔 Expense 直接存为 Transfer
                            currentType = "TRANSFER"
                            currentTarget = match.accountName
                            if (currentNote.isBlank()) currentNote = "Auto-merged Transfer"

                            // 标记：isMerged = false，因为我们需要插入当前这笔 (作为 Transfer)
                            isMerged = false
                        }
                    }

                    if (isMerged) {
                        // 场景 A 的结果：只发通知，不存库
                        val msg = getString(R.string.notif_saved_title, "Transfer RM ${String.format("%.2f", amount)}")
                        showToast(msg)
                        sendInputRequiredNotification(transferId, amount, "Transfer", "Auto-merged")
                    } else {
                        // 场景 B 的结果 或 普通账单：存库
                        val newRecord = ExpenseEntity(
                            amount = amount,
                            type = currentType, // 可能是 EXPENSE 或 TRANSFER
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
                } else {
                    // 🟢 2. 记录 AI 拦截 (AI Rejected)
                    Log.d(TAG, "🤖 AI Rejected: Not a valid transaction")
                    saveIgnoredLog(packageName, title, text, "AI Rejected (Invalid Data)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 🟢 辅助：保存被忽略的日志
    private fun saveIgnoredLog(pkg: String, title: String, text: String, reason: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.ignoredDao().insert(
                    IgnoredEntity(
                        packageName = pkg,
                        title = title,
                        text = text,
                        reason = reason
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun detectAccountName(pkg: String): String {
        return when (pkg) {
            "my.com.tngdigital.ewallet" -> "TNG eWallet"
            "com.grabtaxi.passenger" -> "GrabPay"
            "com.shopee.my" -> "ShopeePay"
            "com.maybank2u.life" -> "Maybank"
            "com.cimb.octo", "com.cimb.clicks.android" -> "CIMB"
            "my.com.rhbgroup.mobilebanking", "com.rhbgroup.rhbengineering" -> "RHB"
            "com.hongleong.pb" -> "Hong Leong"
            "my.com.publicbank.pbe" -> "Public Bank"
            "my.com.mybsn", "com.mybsn.mobile", "net.mybsn.secure" -> "BSN"
            "com.ambank.ambank" -> "AmBank"
            "com.airasia.bigpay" -> "BigPay"
            "com.bankislam.go" -> "Bank Islam"
            else -> "Cash"
        }
    }

    private fun isValidTransaction(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()
        if (!combined.contains("rm")) return false
        val validKeywords = listOf("paid", "spent", "transfer", "sent", "received", "credit", "debit", "payment", "purchase", "top up", "reload", "succes", "deducted", "transferred")
        val hasValidKeyword = validKeywords.any { combined.contains(it) }
        val invalidKeywords = listOf("promo", "off", "cashback", "voucher", "discount", "winner", "win", "campaign", "apply now", "deal", "login", "tac", "otp")
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
            // 🟢 重要：把频道重要性从 HIGH 改为 DEFAULT 或 LOW，这样就不会在大庭广众下发出 huge sound
            // 既然是自动记账，就不应该太打扰用户
            val channel = NotificationChannel(channelId, "Transaction Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        // 点击通知跳转到编辑页面
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EDIT_EXPENSE_ID", expenseId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, expenseId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🟢 动态文案逻辑
        val isAutoMatched = autoNote.isNotBlank() && merchant != "Unknown"

        val titleText = if (isAutoMatched) {
            // ✅ 已自动搞定
            "✅ Saved: RM ${String.format("%.2f", amount)}"
        } else {
            // 📝 需要人工介入
            "📝 Saved: RM ${String.format("%.2f", amount)}"
        }

        val contentText = if (isAutoMatched) {
            "$merchant • $autoNote (Tap to edit)"
        } else {
            "$merchant • Tap to add details"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(contentText)
            // 🟢 只有没匹配到的才设置高优先级弹窗，匹配到的就静默一点
            .setPriority(if (isAutoMatched) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(expenseId, notification)
    }

    private fun isSensitive(content: String): Boolean {
        val lower = content.lowercase()
        return lower.contains("tac") || lower.contains("otp") || lower.contains("code") || lower.contains("login") || lower.contains("verify")
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post { Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show() }
    }
}
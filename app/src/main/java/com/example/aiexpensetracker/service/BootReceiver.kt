package com.example.aiexpensetracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Action Received: $action")

        // 🟢 修改点 1：增加 ACTION_MY_PACKAGE_REPLACED (App 更新完成)
        // 这样你更新代码或覆盖安装后，服务会自动重启
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {

            Log.d("BootReceiver", "🚀 触发自启逻辑: $action")

            val prefs = context.getSharedPreferences("ai_tracker_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("tracking_enabled", true)

            if (isEnabled) {
                val serviceIntent = Intent(context, NotificationListener::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d("BootReceiver", "✅ Service start requested.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "❌ Failed to start service: ${e.message}")
                }
            }
        }
    }
}
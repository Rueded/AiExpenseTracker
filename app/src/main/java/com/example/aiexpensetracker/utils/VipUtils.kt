package com.example.aiexpensetracker.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VIP 状态全局管理工具
 */
object VipUtils {

    private const val PREFS_NAME = "ai_tracker_prefs"
    private const val KEY_IS_LIFETIME = "is_lifetime_vip"
    private const val KEY_EXPIRY_TIME = "vip_expiry_time"
    private const val KEY_OLD_VIP = "is_vip" // 用于数据迁移

    /**
     * 核心判断：用户是否拥有 VIP 权限
     */
    fun isUserVip(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. 先做个简单的数据迁移（兼容老版本兑换码用户）
        if (prefs.getBoolean(KEY_OLD_VIP, false)) {
            prefs.edit().putBoolean(KEY_IS_LIFETIME, true).remove(KEY_OLD_VIP).apply()
        }

        val isLifetime = prefs.getBoolean(KEY_IS_LIFETIME, false)
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        // 只要是永久会员，或者当前时间还没到期，就是 VIP
        return isLifetime || currentTime < expiryTime
    }

    /**
     * 获取格式化后的到期时间文字
     */
    fun getVipStatusString(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_IS_LIFETIME, false)) {
            // 如果是永久，直接返回 Resource 字符串（需在 Screen 中调用，这里仅返回占位或逻辑）
            return "LIFETIME"
        }

        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0L)
        if (expiryTime == 0L) return ""

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(expiryTime))
    }

    /**
     * 获取剩余有效期时间戳（用于计算）
     */
    fun getVipExpiryTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_EXPIRY_TIME, 0L)
    }

    /**
     * 判断是否为永久会员
     */
    fun isLifetimeVip(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_LIFETIME, false)
    }
}
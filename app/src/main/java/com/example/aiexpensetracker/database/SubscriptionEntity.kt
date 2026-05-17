package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,       // e.g. "Spotify"
    val amount: Double,         // e.g. 15.90
    val category: String,       // e.g. "Entertainment"
    val billingDate: Int,       // 扣款日 (1-31), e.g. 5
    val accountName: String,    // e.g. "Maybank"
    val originalTextKeywords: String = "", // 记录触发的关键词，方便后续验证
    val isActive: Boolean = true
)
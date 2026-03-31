package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_learning")
data class MerchantLearningEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,   // 商家名 (例如: McDonald's)
    val timeSlot: String,   // 时间段 (例如: "lunch", "dinner", "morning")
    val category: String,   // 记住的分类 (例如: Food)
    val note: String,       // 记住的备注 (例如: Lunch Set)
    val useCount: Int = 1   // 使用次数 (次数越多，记忆越深)
)
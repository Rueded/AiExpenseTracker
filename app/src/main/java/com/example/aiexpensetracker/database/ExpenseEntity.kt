package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val originalText: String, // OCR 识别原文
    val note: String,
    val accountName: String,
    val targetAccountName: String? = null,

    // 🟢 新增 P0 字段：存储图片在手机内部的路径
    val imagePath: String? = null
)
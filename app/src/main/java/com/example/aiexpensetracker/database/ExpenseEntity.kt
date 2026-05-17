package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,      // EXPENSE / INCOME / TRANSFER (🟢 新增类型)
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val originalText: String,
    val note: String = "",
    val accountName: String = "Cash",       // 🟢 支出账户 / 转出账户 (From)
    val targetAccountName: String? = null   // 🟢 转入账户 (To, 仅 Transfer 用)
)
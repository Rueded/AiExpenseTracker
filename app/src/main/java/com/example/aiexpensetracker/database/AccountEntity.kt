package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,         // 账户名 (e.g. "Maybank", "TNG", "Cash")
    val initialBalance: Double = 0.0, // 初始余额 (刚开始用APP时里面有多少钱)
    val icon: String = ""     // 图标 (预留字段)
)
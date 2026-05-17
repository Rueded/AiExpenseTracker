package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_logs")
data class IgnoredEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val reason: String, // e.g. "Keyword Filter", "AI Rejected"
    val timestamp: Long = System.currentTimeMillis()
)
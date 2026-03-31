package com.example.aiexpensetracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_reports")
data class AnalysisReportEntity(
    @PrimaryKey val monthId: String, // 格式 "2026-01" (作为主键，每个月只有一份报告)
    val totalSpent: Double,
    val summaryText: String,     // AI 生成的评语
    val persona: String,         // "STRICT", "GENTLE", "NEUTRAL"
    val generatedAt: Long = System.currentTimeMillis()
)
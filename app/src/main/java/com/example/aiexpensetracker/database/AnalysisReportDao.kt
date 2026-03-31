package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisReportDao {
    @Query("SELECT * FROM analysis_reports WHERE monthId = :monthId")
    fun getReport(monthId: String): Flow<AnalysisReportEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: AnalysisReportEntity)
}
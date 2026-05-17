package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredDao {
    @Insert
    suspend fun insert(log: IgnoredEntity)

    @Query("SELECT * FROM ignored_logs ORDER BY timestamp DESC LIMIT 50") // 只留最近50条，省空间
    fun getRecentLogs(): Flow<List<IgnoredEntity>>

    @Query("DELETE FROM ignored_logs")
    suspend fun clearAll()
}
package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredDao {
    @Insert
    suspend fun insert(log: IgnoredEntity)

    @Query("SELECT * FROM ignored_logs ORDER BY timestamp DESC")
    fun getRecentLogs(): Flow<List<IgnoredEntity>>

    @Delete
    suspend fun delete(log: IgnoredEntity)

    @Query("DELETE FROM ignored_logs")
    suspend fun clearAll()
}
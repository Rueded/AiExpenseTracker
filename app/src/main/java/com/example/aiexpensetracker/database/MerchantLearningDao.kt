package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantLearningDao {
    @Query("SELECT * FROM merchant_learning WHERE merchant = :merchant AND timeSlot = :timeSlot LIMIT 1")
    suspend fun getLearning(merchant: String, timeSlot: String): MerchantLearningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MerchantLearningEntity)

    @Query("UPDATE merchant_learning SET useCount = useCount + 1, category = :category, note = :note WHERE id = :id")
    suspend fun updateLearning(id: Int, category: String, note: String)

    // 🟢 新增：获取所有记忆（按使用频率排序）供管理界面使用
    @Query("SELECT * FROM merchant_learning ORDER BY useCount DESC")
    fun getAllLearnings(): Flow<List<MerchantLearningEntity>>

    // 🟢 新增：删除某条记忆
    @Query("DELETE FROM merchant_learning WHERE id = :id")
    suspend fun deleteById(id: Int)
}
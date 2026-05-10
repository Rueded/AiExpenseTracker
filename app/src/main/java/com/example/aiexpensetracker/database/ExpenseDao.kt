package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Delete
    suspend fun delete(expense: ExpenseEntity)
    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    // 🟢 修复：强制更新 Type 为 TRANSFER，并且保留原备注，只追加说明
    @Query("UPDATE expenses SET type = 'TRANSFER', targetAccountName = :targetAccount WHERE id = :id")
    suspend fun convertToTransfer(id: Int, targetAccount: String)

    @Query("""
        SELECT * FROM expenses 
        WHERE amount = :amount 
        AND type = :targetType 
        AND timestamp BETWEEN :startTime AND :endTime 
        LIMIT 1
    """)
    suspend fun findMatchingTransaction(amount: Double, targetType: String, startTime: Long, endTime: Long): ExpenseEntity?

    @Query("""
        SELECT * FROM expenses 
        WHERE merchant LIKE '%' || :keyword || '%' 
        OR note LIKE '%' || :keyword || '%' 
        OR category LIKE '%' || :keyword || '%'
        ORDER BY timestamp DESC
    """)
    suspend fun searchByKeyword(keyword: String): List<ExpenseEntity>

    // 🟢 新增：用于根据时间段查账
    @Query("""
        SELECT * FROM expenses 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    suspend fun getExpensesByTimeRange(startTime: Long, endTime: Long): List<ExpenseEntity>
}
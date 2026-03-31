package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY billingDate ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sub: SubscriptionEntity)

    @Update
    suspend fun update(sub: SubscriptionEntity)

    @Delete
    suspend fun delete(sub: SubscriptionEntity)

    // 用于检测重复：查找同一个商家的活跃订阅
    @Query("SELECT COUNT(*) FROM subscriptions WHERE merchant = :merchant AND isActive = 1")
    suspend fun countActiveByMerchant(merchant: String): Int
}
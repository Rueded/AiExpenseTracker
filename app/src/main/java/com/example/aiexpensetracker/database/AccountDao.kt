package com.example.aiexpensetracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) // 🟢 改为 IGNORE，防止重复创建报错
    suspend fun insert(account: AccountEntity)

    // 🟢 新增：根据名字删除账户
    @Query("DELETE FROM accounts WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getCount(): Int

    // 🟢 新增：检查账户是否存在
    @Query("SELECT COUNT(*) FROM accounts WHERE name = :name")
    suspend fun exists(name: String): Int
}
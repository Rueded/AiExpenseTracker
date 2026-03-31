package com.example.aiexpensetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🟢 核心修复：将版本号从 8 改为 9
@Database(
    entities = [
        ExpenseEntity::class,
        MerchantLearningEntity::class,
        AccountEntity::class,
        IgnoredEntity::class,
        SubscriptionEntity::class,
        AnalysisReportEntity::class
    ],
    version = 9, // ⬅️ 必须升级版本号，数据库才会更新字段
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun merchantLearningDao(): MerchantLearningDao
    abstract fun accountDao(): AccountDao
    abstract fun ignoredDao(): IgnoredDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun analysisReportDao(): AnalysisReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    // 允许数据库结构变动时自动清空重建 (防止闪退)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
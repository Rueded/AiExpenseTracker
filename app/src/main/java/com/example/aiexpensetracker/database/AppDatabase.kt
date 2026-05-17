package com.example.aiexpensetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🟢 版本号 8
@Database(
    entities = [
        ExpenseEntity::class,
        MerchantLearningEntity::class,
        AccountEntity::class,
        IgnoredEntity::class,
        SubscriptionEntity::class,
        AnalysisReportEntity::class // 🟢 新增
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun merchantLearningDao(): MerchantLearningDao
    abstract fun accountDao(): AccountDao
    abstract fun ignoredDao(): IgnoredDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun analysisReportDao(): AnalysisReportDao // 🟢

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.example.ocrjizhang.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ocrjizhang.data.local.converter.AppTypeConverters
import com.example.ocrjizhang.data.local.dao.CategoryDao
import com.example.ocrjizhang.data.local.dao.OcrRecordDao
import com.example.ocrjizhang.data.local.dao.SyncOperationDao
import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.local.dao.UserDao
import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.OcrRecordEntity
import com.example.ocrjizhang.data.local.entity.SyncOperationEntity
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        OcrRecordEntity::class,
        SyncOperationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun ocrRecordDao(): OcrRecordDao
    abstract fun syncOperationDao(): SyncOperationDao
}

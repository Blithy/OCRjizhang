package com.example.ocrjizhang.di

import android.content.Context
import androidx.room.Room
import com.example.ocrjizhang.data.local.dao.AccountDao
import com.example.ocrjizhang.data.local.dao.CategoryDao
import com.example.ocrjizhang.data.local.dao.OcrRecordDao
import com.example.ocrjizhang.data.local.dao.SyncOperationDao
import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.local.dao.UserDao
import com.example.ocrjizhang.data.local.database.AppDatabase
import com.example.ocrjizhang.data.local.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ocr_jizhang.db",
        ).addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideOcrRecordDao(database: AppDatabase): OcrRecordDao = database.ocrRecordDao()

    @Provides
    fun provideSyncOperationDao(database: AppDatabase): SyncOperationDao = database.syncOperationDao()
}

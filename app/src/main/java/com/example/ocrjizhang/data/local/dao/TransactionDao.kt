package com.example.ocrjizhang.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY transactionTime DESC")
    fun observeTransactions(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId AND transactionTime BETWEEN :startTime AND :endTime
        ORDER BY transactionTime DESC
        """
    )
    fun observeTransactionsBetween(
        userId: Long,
        startTime: Long,
        endTime: Long,
    ): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Long)

    @Query(
        """
        UPDATE transactions
        SET categoryId = :newCategoryId,
            categoryName = :newCategoryName,
            updatedAt = :updatedAt,
            syncStatus = :syncStatus
        WHERE userId = :userId AND categoryId = :oldCategoryId
        """
    )
    suspend fun reassignCategory(
        userId: Long,
        oldCategoryId: Long,
        newCategoryId: Long,
        newCategoryName: String,
        updatedAt: Long,
        syncStatus: SyncStatus,
    )

    @Query(
        """
        UPDATE transactions
        SET categoryName = :newCategoryName,
            updatedAt = :updatedAt,
            syncStatus = :syncStatus
        WHERE userId = :userId AND categoryId = :categoryId
        """
    )
    suspend fun renameCategoryInTransactions(
        userId: Long,
        categoryId: Long,
        newCategoryName: String,
        updatedAt: Long,
        syncStatus: SyncStatus,
    )

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)
}

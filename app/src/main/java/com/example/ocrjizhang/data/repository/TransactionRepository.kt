package com.example.ocrjizhang.data.repository

import androidx.room.withTransaction
import com.example.ocrjizhang.data.local.dao.CategoryDao
import com.example.ocrjizhang.data.local.dao.SyncOperationDao
import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.local.database.AppDatabase
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncEntityType
import com.example.ocrjizhang.data.local.entity.SyncOperationEntity
import com.example.ocrjizhang.data.local.entity.SyncOperationType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.TransactionSource
import com.example.ocrjizhang.utils.LocalIdGenerator
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val syncOperationDao: SyncOperationDao,
    private val gson: Gson,
) {

    fun observeTransactions(userId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactions(userId)

    fun observeTransactionsBetween(
        userId: Long,
        startTime: Long,
        endTime: Long,
    ): Flow<List<TransactionEntity>> = transactionDao.observeTransactionsBetween(
        userId = userId,
        startTime = startTime,
        endTime = endTime,
    )

    suspend fun createTransaction(
        userId: Long,
        type: RecordType,
        amountFen: Long,
        categoryId: Long,
        transactionTime: Long,
        merchantName: String,
        remark: String,
    ) {
        val category = validateCategory(userId, type, categoryId)
        val now = System.currentTimeMillis()
        val entity = TransactionEntity(
            id = LocalIdGenerator.nextId(),
            userId = userId,
            type = type,
            amountFen = amountFen,
            categoryId = category.id,
            categoryName = category.name,
            remark = remark.trim().ifBlank { null },
            merchantName = merchantName.trim().ifBlank { null },
            transactionTime = transactionTime,
            source = TransactionSource.MANUAL,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE,
        )

        database.withTransaction {
            transactionDao.upsert(entity)
            enqueueTransactionSync(entity, SyncOperationType.CREATE, now)
        }
    }

    suspend fun updateTransaction(
        userId: Long,
        transactionId: Long,
        type: RecordType,
        amountFen: Long,
        categoryId: Long,
        transactionTime: Long,
        merchantName: String,
        remark: String,
    ) {
        val existing = getOwnedTransaction(userId, transactionId)
        val category = validateCategory(userId, type, categoryId)
        val now = System.currentTimeMillis()
        val entity = existing.copy(
            type = type,
            amountFen = amountFen,
            categoryId = category.id,
            categoryName = category.name,
            remark = remark.trim().ifBlank { null },
            merchantName = merchantName.trim().ifBlank { null },
            transactionTime = transactionTime,
            updatedAt = now,
            syncStatus = if (existing.syncStatus == SyncStatus.PENDING_CREATE) {
                SyncStatus.PENDING_CREATE
            } else {
                SyncStatus.PENDING_UPDATE
            },
        )

        database.withTransaction {
            transactionDao.upsert(entity)
            enqueueTransactionSync(entity, SyncOperationType.UPDATE, now)
        }
    }

    suspend fun deleteTransaction(userId: Long, transactionId: Long) {
        val existing = getOwnedTransaction(userId, transactionId)
        val now = System.currentTimeMillis()
        database.withTransaction {
            transactionDao.deleteById(transactionId)
            enqueueTransactionSync(existing, SyncOperationType.DELETE, now)
        }
    }

    private suspend fun validateCategory(userId: Long, type: RecordType, categoryId: Long) =
        categoryDao.getCategoryById(categoryId)
            ?.takeIf { it.userId == userId && it.type == type }
            ?: error("当前分类不可用，请重新选择")

    private suspend fun getOwnedTransaction(userId: Long, transactionId: Long): TransactionEntity =
        transactionDao.getTransactionById(transactionId)
            ?.takeIf { it.userId == userId }
            ?: error("交易不存在或无权访问")

    private suspend fun enqueueTransactionSync(
        entity: TransactionEntity,
        operationType: SyncOperationType,
        createdAt: Long,
    ) {
        syncOperationDao.enqueue(
            SyncOperationEntity(
                entityType = SyncEntityType.TRANSACTION,
                entityId = entity.id,
                operationType = operationType,
                payloadJson = gson.toJson(entity),
                createdAt = createdAt,
                retryCount = 0,
            ),
        )
    }
}

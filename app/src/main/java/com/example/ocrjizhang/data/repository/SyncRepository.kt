package com.example.ocrjizhang.data.repository

import androidx.room.withTransaction
import com.example.ocrjizhang.data.local.dao.CategoryDao
import com.example.ocrjizhang.data.local.dao.SyncOperationDao
import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.local.database.AppDatabase
import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncEntityType
import com.example.ocrjizhang.data.local.entity.SyncOperationEntity
import com.example.ocrjizhang.data.local.entity.SyncOperationType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.TransactionSource
import com.example.ocrjizhang.data.remote.request.SyncPullRequest
import com.example.ocrjizhang.data.remote.request.SyncPushRequest
import com.example.ocrjizhang.data.remote.response.CategoryDto
import com.example.ocrjizhang.data.remote.response.SyncPullPayloadDto
import com.example.ocrjizhang.data.remote.response.TransactionDto
import com.example.ocrjizhang.data.remote.service.SyncService
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class SyncResult(
    val pushedCount: Int,
    val categoryCount: Int,
    val transactionCount: Int,
)

@Singleton
class SyncRepository @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val syncOperationDao: SyncOperationDao,
    private val syncService: SyncService,
    private val gson: Gson,
) {

    suspend fun syncNow(): Result<SyncResult> = runCatching {
        val session = sessionManager.sessionFlow.first()
        val userId = session.userId ?: error("当前还没有登录账号")
        compactLocalCategories(userId)
        val pendingOperations = syncOperationDao.getPendingOperations()
        val pushRequest = buildPushRequest(pendingOperations)

        if (pushRequest.hasChanges()) {
            val pushResponse = syncService.pushChanges(pushRequest)
            if (pushResponse.code != 0) {
                error(pushResponse.msg.ifBlank { "同步上传失败" })
            }
            pendingOperations.forEach { operation ->
                syncOperationDao.deleteById(operation.id)
            }
        }

        val pullResponse = syncService.pullChanges(SyncPullRequest(lastSyncTime = 0L))
        if (pullResponse.code != 0 || pullResponse.data == null) {
            error(pullResponse.msg.ifBlank { "同步拉取失败" })
        }

        var payload = pullResponse.data
        var uploadedCount = pendingOperations.size

        if (
            pendingOperations.isEmpty() &&
            payload.categories.isEmpty() &&
            payload.transactions.isEmpty()
        ) {
            val fullPushRequest = buildFullSyncPushRequest(userId)
            if (fullPushRequest.hasChanges()) {
                val fullPushResponse = syncService.pushChanges(fullPushRequest)
                if (fullPushResponse.code != 0) {
                    error(fullPushResponse.msg.ifBlank { "首次全量同步失败" })
                }
                uploadedCount = fullPushRequest.totalItemCount()
                val refreshedPullResponse = syncService.pullChanges(SyncPullRequest(lastSyncTime = 0L))
                if (refreshedPullResponse.code != 0 || refreshedPullResponse.data == null) {
                    error(refreshedPullResponse.msg.ifBlank { "同步拉取失败" })
                }
                payload = refreshedPullResponse.data
            }
        }

        val hasRemoteData = payload.categories.isNotEmpty() || payload.transactions.isNotEmpty()
        if (hasRemoteData || pendingOperations.isNotEmpty()) {
            applyRemoteSnapshot(userId, payload)
        }

        SyncResult(
            pushedCount = uploadedCount,
            categoryCount = if (hasRemoteData || pendingOperations.isNotEmpty()) payload.categories.size else 0,
            transactionCount = if (hasRemoteData || pendingOperations.isNotEmpty()) payload.transactions.size else 0,
        )
    }

    private suspend fun applyRemoteSnapshot(userId: Long, payload: SyncPullPayloadDto) {
        database.withTransaction {
            transactionDao.deleteByUserId(userId)
            categoryDao.deleteByUserId(userId)

            if (payload.categories.isNotEmpty()) {
                categoryDao.upsertAll(payload.categories.map(::toCategoryEntity))
            }
            if (payload.transactions.isNotEmpty()) {
                transactionDao.upsertAll(payload.transactions.map(::toTransactionEntity))
            }
        }
    }

    private fun buildPushRequest(operations: List<SyncOperationEntity>): SyncPushRequest {
        val createCategories = mutableListOf<CategoryDto>()
        val updateCategories = mutableListOf<CategoryDto>()
        val deleteCategoryIds = linkedSetOf<Long>()
        val createTransactions = mutableListOf<TransactionDto>()
        val updateTransactions = mutableListOf<TransactionDto>()
        val deleteTransactionIds = linkedSetOf<Long>()

        operations.forEach { operation ->
            when (operation.entityType) {
                SyncEntityType.CATEGORY -> {
                    when (operation.operationType) {
                        SyncOperationType.CREATE -> operation.payloadJson
                            ?.let { gson.fromJson(it, CategoryEntity::class.java) }
                            ?.let(::toCategoryDto)
                            ?.let(createCategories::add)

                        SyncOperationType.UPDATE -> operation.payloadJson
                            ?.let { gson.fromJson(it, CategoryEntity::class.java) }
                            ?.let(::toCategoryDto)
                            ?.let(updateCategories::add)

                        SyncOperationType.DELETE -> deleteCategoryIds += operation.entityId
                    }
                }

                SyncEntityType.TRANSACTION -> {
                    when (operation.operationType) {
                        SyncOperationType.CREATE -> operation.payloadJson
                            ?.let { gson.fromJson(it, TransactionEntity::class.java) }
                            ?.let(::toTransactionDto)
                            ?.let(createTransactions::add)

                        SyncOperationType.UPDATE -> operation.payloadJson
                            ?.let { gson.fromJson(it, TransactionEntity::class.java) }
                            ?.let(::toTransactionDto)
                            ?.let(updateTransactions::add)

                        SyncOperationType.DELETE -> deleteTransactionIds += operation.entityId
                    }
                }
            }
        }

        return SyncPushRequest(
            createCategories = createCategories,
            updateCategories = updateCategories,
            deleteCategoryIds = deleteCategoryIds.toList(),
            createTransactions = createTransactions,
            updateTransactions = updateTransactions,
            deleteTransactionIds = deleteTransactionIds.toList(),
        )
    }

    private suspend fun buildFullSyncPushRequest(userId: Long): SyncPushRequest {
        val categories = categoryDao.getCategories(userId).map(::toCategoryDto)
        val transactions = transactionDao.getTransactions(userId).map(::toTransactionDto)
        return SyncPushRequest(
            createCategories = categories,
            createTransactions = transactions,
        )
    }

    private fun SyncPushRequest.hasChanges(): Boolean =
        createCategories.isNotEmpty() ||
            updateCategories.isNotEmpty() ||
            deleteCategoryIds.isNotEmpty() ||
            createTransactions.isNotEmpty() ||
            updateTransactions.isNotEmpty() ||
            deleteTransactionIds.isNotEmpty()

    private fun SyncPushRequest.totalItemCount(): Int =
        createCategories.size +
            updateCategories.size +
            deleteCategoryIds.size +
            createTransactions.size +
            updateTransactions.size +
            deleteTransactionIds.size

    private suspend fun compactLocalCategories(userId: Long) {
        val categories = categoryDao.getCategories(userId)
        val duplicateGroups = categories
            .groupBy { "${it.type.name}|${it.name.lowercase()}" }
            .values
            .filter { it.size > 1 }

        if (duplicateGroups.isEmpty()) return

        val now = System.currentTimeMillis()
        database.withTransaction {
            duplicateGroups.forEach { group ->
                val canonical = group
                    .sortedWith(
                        compareByDescending<CategoryEntity> { it.isDefault }
                            .thenByDescending { it.updatedAt }
                            .thenBy { it.createdAt }
                            .thenBy { it.id },
                    )
                    .first()

                group
                    .filter { it.id != canonical.id }
                    .forEach { duplicate ->
                        transactionDao.reassignCategory(
                            userId = userId,
                            oldCategoryId = duplicate.id,
                            newCategoryId = canonical.id,
                            newCategoryName = canonical.name,
                            updatedAt = now,
                            syncStatus = SyncStatus.SYNCED,
                        )
                        categoryDao.deleteById(duplicate.id)
                    }
            }
        }
    }

    private fun toCategoryDto(entity: CategoryEntity): CategoryDto =
        CategoryDto(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            type = entity.type.name,
            icon = entity.icon,
            color = entity.color,
            isDefault = entity.isDefault,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    private fun toTransactionDto(entity: TransactionEntity): TransactionDto =
        TransactionDto(
            id = entity.id,
            userId = entity.userId,
            type = entity.type.name,
            amountFen = entity.amountFen,
            categoryId = entity.categoryId,
            categoryName = entity.categoryName,
            remark = entity.remark,
            merchantName = entity.merchantName,
            transactionTime = entity.transactionTime,
            source = entity.source.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    private fun toCategoryEntity(dto: CategoryDto): CategoryEntity =
        CategoryEntity(
            id = dto.id,
            userId = dto.userId,
            name = dto.name,
            type = RecordType.valueOf(dto.type),
            icon = dto.icon,
            color = dto.color,
            isDefault = dto.isDefault,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            syncStatus = SyncStatus.SYNCED,
        )

    private fun toTransactionEntity(dto: TransactionDto): TransactionEntity =
        TransactionEntity(
            id = dto.id,
            userId = dto.userId,
            type = RecordType.valueOf(dto.type),
            amountFen = dto.amountFen,
            categoryId = dto.categoryId,
            categoryName = dto.categoryName,
            remark = dto.remark,
            merchantName = dto.merchantName,
            transactionTime = dto.transactionTime,
            source = TransactionSource.valueOf(dto.source),
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            syncStatus = SyncStatus.SYNCED,
        )
}

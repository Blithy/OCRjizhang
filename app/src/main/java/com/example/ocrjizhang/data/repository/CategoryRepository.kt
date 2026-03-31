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
import com.example.ocrjizhang.utils.LocalIdGenerator
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CategoryRepository @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val syncOperationDao: SyncOperationDao,
    private val gson: Gson,
) {

    fun observeCategories(userId: Long, type: RecordType): Flow<List<CategoryEntity>> =
        categoryDao.observeCategories(userId, type)

    suspend fun ensureDefaultCategories(userId: Long) {
        val missingDefaults = CategoryDefaults.buildMissingDefaults(
            userId = userId,
            existingCategories = categoryDao.getCategories(userId),
        )
        if (missingDefaults.isEmpty()) return

        database.withTransaction {
            categoryDao.upsertAll(missingDefaults)
            missingDefaults.forEach { category ->
                enqueueCategorySync(category, SyncOperationType.CREATE, category.createdAt)
            }
        }
    }

    suspend fun createCategory(userId: Long, type: RecordType, rawName: String) {
        val name = rawName.trim()
        validateName(userId, type, name, excludedId = -1L)

        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            id = LocalIdGenerator.nextId(),
            userId = userId,
            name = name,
            type = type,
            icon = null,
            color = null,
            isDefault = false,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE,
        )

        database.withTransaction {
            categoryDao.upsert(category)
            enqueueCategorySync(category, SyncOperationType.CREATE, now)
        }
    }

    suspend fun updateCategory(userId: Long, categoryId: Long, rawName: String) {
        val category = getOwnedCategory(userId, categoryId)
        if (category.isDefault) {
            error("默认分类暂不支持编辑")
        }

        val name = rawName.trim()
        validateName(userId, category.type, name, excludedId = category.id)

        val now = System.currentTimeMillis()
        val updated = category.copy(
            name = name,
            updatedAt = now,
            syncStatus = if (category.syncStatus == SyncStatus.PENDING_CREATE) {
                SyncStatus.PENDING_CREATE
            } else {
                SyncStatus.PENDING_UPDATE
            },
        )

        database.withTransaction {
            categoryDao.upsert(updated)
            transactionDao.renameCategoryInTransactions(
                userId = userId,
                categoryId = category.id,
                newCategoryName = updated.name,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )
            enqueueCategorySync(updated, SyncOperationType.UPDATE, now)
        }
    }

    suspend fun deleteCategory(userId: Long, categoryId: Long) {
        val category = getOwnedCategory(userId, categoryId)
        if (category.isDefault) {
            error("默认分类不能删除")
        }

        val fallbackCategory = categoryDao.findByExactName(
            userId = userId,
            type = category.type,
            name = CategoryDefaults.UNCATEGORIZED_NAME,
        ) ?: error("未找到默认“未分类”类别")

        val now = System.currentTimeMillis()
        database.withTransaction {
            transactionDao.reassignCategory(
                userId = userId,
                oldCategoryId = category.id,
                newCategoryId = fallbackCategory.id,
                newCategoryName = fallbackCategory.name,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )
            categoryDao.deleteById(category.id)
            enqueueCategorySync(category, SyncOperationType.DELETE, now)
        }
    }

    private suspend fun getOwnedCategory(userId: Long, categoryId: Long): CategoryEntity {
        val category = categoryDao.getCategoryById(categoryId) ?: error("分类不存在")
        if (category.userId != userId) {
            error("当前账号无法操作该分类")
        }
        return category
    }

    private suspend fun validateName(
        userId: Long,
        type: RecordType,
        name: String,
        excludedId: Long,
    ) {
        if (name.isBlank()) {
            error("分类名称不能为空")
        }
        val duplicateCount = categoryDao.countByName(
            userId = userId,
            type = type,
            name = name,
            excludedId = excludedId,
        )
        if (duplicateCount > 0) {
            error("同类型下已存在同名分类")
        }
    }

    private suspend fun enqueueCategorySync(
        category: CategoryEntity,
        operationType: SyncOperationType,
        createdAt: Long,
    ) {
        syncOperationDao.enqueue(
            SyncOperationEntity(
                entityType = SyncEntityType.CATEGORY,
                entityId = category.id,
                operationType = operationType,
                payloadJson = gson.toJson(category),
                createdAt = createdAt,
                retryCount = 0,
            ),
        )
    }
}

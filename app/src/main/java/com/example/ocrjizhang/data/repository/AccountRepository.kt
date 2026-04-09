package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.dao.AccountDao
import com.example.ocrjizhang.data.local.dao.SyncOperationDao
import com.example.ocrjizhang.data.local.entity.AccountEntity
import com.example.ocrjizhang.data.local.entity.SyncEntityType
import com.example.ocrjizhang.data.local.entity.SyncOperationEntity
import com.example.ocrjizhang.data.local.entity.SyncOperationType
import com.example.ocrjizhang.utils.LocalIdGenerator
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val syncOperationDao: SyncOperationDao,
    private val syncRepository: SyncRepository,
    private val gson: Gson,
) {

    fun observeAccounts(userId: Long): Flow<List<AccountEntity>> =
        accountDao.observeAccounts(userId)

    suspend fun ensureDefaultAccounts(userId: Long) {
        val existingAccounts = accountDao.getAccounts(userId)
        val defaultUpgrades = AccountDefaults.buildDefaultUpgrades(existingAccounts)
        val missingDefaults = AccountDefaults.buildMissingDefaults(
            userId = userId,
            existingAccounts = existingAccounts,
        )
        if (missingDefaults.isEmpty() && defaultUpgrades.isEmpty()) return

        val now = System.currentTimeMillis()
        accountDao.upsertAll(missingDefaults + defaultUpgrades)
        missingDefaults.forEach { account ->
            enqueueAccountSync(
                entity = account,
                operationType = SyncOperationType.CREATE,
                createdAt = now,
            )
        }
        defaultUpgrades.forEach { account ->
            enqueueAccountSync(
                entity = account,
                operationType = SyncOperationType.UPDATE,
                createdAt = now,
            )
        }
        syncRepository.pushPendingChangesBestEffort()
    }

    suspend fun createAccount(userId: Long, rawName: String, balanceFen: Long) {
        val name = rawName.trim()
        validateName(userId, name, excludedId = -1L)
        validateBalance(balanceFen)

        val now = System.currentTimeMillis()
        val account = AccountEntity(
            id = LocalIdGenerator.nextId(),
            userId = userId,
            name = name,
            symbol = AccountDefaults.buildSymbol(name),
            balanceFen = balanceFen,
            isDefault = false,
            createdAt = now,
            updatedAt = now,
        )
        accountDao.upsert(account)
        enqueueAccountSync(account, SyncOperationType.CREATE, now)
        syncRepository.pushPendingChangesBestEffort()
    }

    suspend fun updateAccount(userId: Long, accountId: Long, rawName: String, balanceFen: Long) {
        val existing = getOwnedAccount(userId, accountId)
        val name = rawName.trim()
        validateName(userId, name, excludedId = accountId)
        validateBalance(balanceFen)

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            name = name,
            symbol = AccountDefaults.buildSymbol(name),
            balanceFen = balanceFen,
            updatedAt = now,
        )
        accountDao.upsert(updated)
        enqueueAccountSync(updated, SyncOperationType.UPDATE, now)
        syncRepository.pushPendingChangesBestEffort()
    }

    suspend fun deleteAccount(userId: Long, accountId: Long) {
        getOwnedAccount(userId, accountId)
        val now = System.currentTimeMillis()
        accountDao.deleteById(accountId)
        syncOperationDao.enqueue(
            SyncOperationEntity(
                entityType = SyncEntityType.ACCOUNT,
                entityId = accountId,
                operationType = SyncOperationType.DELETE,
                payloadJson = null,
                createdAt = now,
                retryCount = 0,
            ),
        )
        syncRepository.pushPendingChangesBestEffort()
    }

    private suspend fun getOwnedAccount(userId: Long, accountId: Long): AccountEntity =
        accountDao.getAccountById(accountId)
            ?.takeIf { it.userId == userId }
            ?: error("账户不存在或无权访问")

    private suspend fun validateName(userId: Long, name: String, excludedId: Long) {
        if (name.isBlank()) {
            error("账户名称不能为空")
        }
        val duplicateCount = accountDao.countByName(userId, name, excludedId)
        if (duplicateCount > 0) {
            error("已存在同名账户")
        }
    }

    private fun validateBalance(balanceFen: Long) {
        if (balanceFen < 0L) {
            error("账户余额不能为负数")
        }
    }

    private suspend fun enqueueAccountSync(
        entity: AccountEntity,
        operationType: SyncOperationType,
        createdAt: Long,
    ) {
        syncOperationDao.enqueue(
            SyncOperationEntity(
                entityType = SyncEntityType.ACCOUNT,
                entityId = entity.id,
                operationType = operationType,
                payloadJson = gson.toJson(entity),
                createdAt = createdAt,
                retryCount = 0,
            ),
        )
    }
}

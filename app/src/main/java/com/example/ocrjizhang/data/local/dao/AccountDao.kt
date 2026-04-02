package com.example.ocrjizhang.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocrjizhang.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        """
        SELECT * FROM accounts
        WHERE userId = :userId
        ORDER BY isDefault DESC, updatedAt DESC, name COLLATE NOCASE ASC
        """
    )
    fun observeAccounts(userId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAccounts(userId: Long): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Long): AccountEntity?

    @Query(
        """
        SELECT COUNT(*) FROM accounts
        WHERE userId = :userId AND LOWER(name) = LOWER(:name) AND id != :excludedId
        """
    )
    suspend fun countByName(userId: Long, name: String, excludedId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteById(accountId: Long)
}

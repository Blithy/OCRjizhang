package com.example.ocrjizhang.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocrjizhang.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_operations ORDER BY createdAt ASC")
    fun observePendingOperations(): Flow<List<SyncOperationEntity>>

    @Query("SELECT * FROM sync_operations ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_operations WHERE id = :operationId")
    suspend fun deleteById(operationId: Long)
}

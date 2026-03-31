package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entityType: SyncEntityType,
    val entityId: Long,
    val operationType: SyncOperationType,
    val payloadJson: String?,
    val createdAt: Long,
    val retryCount: Int,
)

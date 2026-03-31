package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val type: RecordType,
    val amountFen: Long,
    val categoryId: Long,
    val categoryName: String,
    val remark: String?,
    val merchantName: String?,
    val transactionTime: Long,
    val source: TransactionSource,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
)

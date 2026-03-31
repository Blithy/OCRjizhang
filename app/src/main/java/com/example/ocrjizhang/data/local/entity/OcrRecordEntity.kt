package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_records")
data class OcrRecordEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val imageUri: String,
    val amountText: String?,
    val amountFen: Long?,
    val dateText: String?,
    val merchantName: String?,
    val rawJson: String?,
    val createdAt: Long,
)

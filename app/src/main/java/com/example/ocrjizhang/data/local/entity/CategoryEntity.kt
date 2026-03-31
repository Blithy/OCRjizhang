package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val type: RecordType,
    val icon: String?,
    val color: String?,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus,
)

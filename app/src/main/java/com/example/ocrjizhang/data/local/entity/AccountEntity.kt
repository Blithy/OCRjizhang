package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val name: String,
    val symbol: String,
    val balanceFen: Long,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

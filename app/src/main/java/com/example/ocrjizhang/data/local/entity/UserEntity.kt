package com.example.ocrjizhang.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val nickname: String?,
    val email: String?,
    val phone: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

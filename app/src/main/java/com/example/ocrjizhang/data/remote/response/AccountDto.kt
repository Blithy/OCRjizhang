package com.example.ocrjizhang.data.remote.response

data class AccountDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val symbol: String,
    val balanceFen: Long,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

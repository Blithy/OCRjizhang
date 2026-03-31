package com.example.ocrjizhang.data.remote.response

data class CategoryDto(
    val id: Long,
    val userId: Long,
    val name: String,
    val type: String,
    val icon: String?,
    val color: String?,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

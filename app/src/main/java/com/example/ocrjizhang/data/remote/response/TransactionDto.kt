package com.example.ocrjizhang.data.remote.response

data class TransactionDto(
    val id: Long,
    val userId: Long,
    val type: String,
    val amountFen: Long,
    val accountId: Long?,
    val accountName: String?,
    val categoryId: Long,
    val categoryName: String,
    val remark: String?,
    val merchantName: String?,
    val transactionTime: Long,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long,
)

package com.example.ocrjizhang.data.remote.response

data class SyncPullPayloadDto(
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val serverTime: Long,
)

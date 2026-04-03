package com.example.ocrjizhang.data.remote.response

data class SyncPullPayloadDto(
    val accounts: List<AccountDto>,
    val categories: List<CategoryDto>,
    val transactions: List<TransactionDto>,
    val serverTime: Long,
)

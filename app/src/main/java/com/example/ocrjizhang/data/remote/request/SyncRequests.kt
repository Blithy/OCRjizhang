package com.example.ocrjizhang.data.remote.request

import com.example.ocrjizhang.data.remote.response.CategoryDto
import com.example.ocrjizhang.data.remote.response.TransactionDto

data class SyncPushRequest(
    val createCategories: List<CategoryDto> = emptyList(),
    val updateCategories: List<CategoryDto> = emptyList(),
    val deleteCategoryIds: List<Long> = emptyList(),
    val createTransactions: List<TransactionDto> = emptyList(),
    val updateTransactions: List<TransactionDto> = emptyList(),
    val deleteTransactionIds: List<Long> = emptyList(),
)

data class SyncPullRequest(
    val lastSyncTime: Long,
)

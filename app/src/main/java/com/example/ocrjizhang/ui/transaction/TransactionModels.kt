package com.example.ocrjizhang.ui.transaction

import com.example.ocrjizhang.data.local.entity.RecordType

data class CategoryOption(
    val id: Long,
    val name: String,
)

data class TransactionListItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val meta: String,
    val amountLabel: String,
    val type: RecordType,
)

data class TransactionUiState(
    val isLoading: Boolean = true,
    val selectedType: RecordType = RecordType.EXPENSE,
    val categories: List<CategoryOption> = emptyList(),
    val selectedCategoryId: Long? = null,
    val amountInput: String = "",
    val dateLabel: String = "\u9009\u62e9\u65e5\u671f",
    val dateMillis: Long = System.currentTimeMillis(),
    val merchantInput: String = "",
    val remarkInput: String = "",
    val isEditing: Boolean = false,
    val submitLabel: String = "\u4fdd\u5b58\u8fd9\u7b14\u8bb0\u8d26",
    val secondaryLabel: String = "\u6e05\u7a7a\u8868\u5355",
    val showOcrPrefillHint: Boolean = false,
    val ocrPrefillTitle: String = "",
    val ocrPrefillBody: String = "",
    val transactions: List<TransactionListItem> = emptyList(),
    val emptyTitle: String = "\u8fd8\u6ca1\u6709\u4ea4\u6613\u8bb0\u5f55",
    val emptyBody: String = "\u5148\u4fdd\u5b58\u4e00\u7b14\u6536\u5165\u6216\u652f\u51fa\uff0c\u8fd9\u91cc\u5c31\u4f1a\u5f00\u59cb\u51fa\u73b0\u771f\u5b9e\u8bb0\u5f55\u3002",
)

sealed interface TransactionEvent {
    data class Message(val message: String) : TransactionEvent
}

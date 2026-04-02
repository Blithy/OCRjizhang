package com.example.ocrjizhang.ui.transaction

import com.example.ocrjizhang.data.local.entity.RecordType

data class CategoryOption(
    val id: Long,
    val name: String,
    val symbol: String,
    val isSelected: Boolean,
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
    val amountDisplay: String = "\u00a50.00",
    val dateLabel: String = "\u9009\u62e9\u65e5\u671f\u65f6\u95f4",
    val dateMillis: Long = System.currentTimeMillis(),
    val merchantInput: String = "",
    val remarkInput: String = "",
    val detailLabel: String = "\u70b9\u51fb\u8865\u5145\u5907\u6ce8",
    val isEditing: Boolean = false,
    val submitLabel: String = "\u5b8c\u6210",
    val secondaryLabel: String = "\u4fdd\u5b58\u518d\u8bb0",
    val showDeleteButton: Boolean = false,
    val showOcrPrefillHint: Boolean = false,
    val ocrPrefillTitle: String = "",
    val ocrPrefillBody: String = "",
)

sealed interface TransactionEvent {
    data class Message(val message: String) : TransactionEvent
    data object SavedAndClose : TransactionEvent
}

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
    val dateLabel: String = "选择日期",
    val dateMillis: Long = System.currentTimeMillis(),
    val merchantInput: String = "",
    val remarkInput: String = "",
    val isEditing: Boolean = false,
    val submitLabel: String = "保存这笔记账",
    val secondaryLabel: String = "清空表单",
    val transactions: List<TransactionListItem> = emptyList(),
    val emptyTitle: String = "还没有交易记录",
    val emptyBody: String = "先保存一笔收入或支出，这里就会开始出现真实记录。",
)

sealed interface TransactionEvent {
    data class Message(val message: String) : TransactionEvent
}

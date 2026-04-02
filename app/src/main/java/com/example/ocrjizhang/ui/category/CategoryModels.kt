package com.example.ocrjizhang.ui.category

import com.example.ocrjizhang.data.local.entity.RecordType

data class CategoryListItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val detail: String,
    val isDefault: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
)

data class CategoryUiState(
    val isLoading: Boolean = true,
    val selectedType: RecordType = RecordType.EXPENSE,
    val categories: List<CategoryListItem> = emptyList(),
    val emptyTitle: String = "",
    val emptyBody: String = "",
    val actionLabel: String = "",
)

sealed interface CategoryEvent {
    data class Message(val message: String) : CategoryEvent
}

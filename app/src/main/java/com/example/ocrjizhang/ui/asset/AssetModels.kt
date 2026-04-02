package com.example.ocrjizhang.ui.asset

data class AssetAccountItem(
    val id: Long,
    val name: String,
    val symbol: String,
    val detail: String,
    val balanceLabel: String,
    val isDefault: Boolean,
)

data class AssetUiState(
    val isLoading: Boolean = true,
    val totalAssetLabel: String = "￥0.00",
    val accountCountLabel: String = "0 个账户",
    val statusLabel: String = "等待账户数据",
    val defaultAccountsLabel: String = "",
    val accounts: List<AssetAccountItem> = emptyList(),
    val emptyTitle: String = "",
    val emptyBody: String = "",
)

sealed interface AssetEvent {
    data class Message(val value: String) : AssetEvent
}

package com.example.ocrjizhang.ui.profile

data class ProfileUiState(
    val isSyncing: Boolean = false,
)

sealed interface ProfileEvent {
    data object LogoutSuccess : ProfileEvent
    data class Message(val value: String) : ProfileEvent
}

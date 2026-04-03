package com.example.ocrjizhang.ui.profile

data class ProfileUiState(
    val isSyncing: Boolean = false,
    val isUpdatingUser: Boolean = false,
    val nickname: String = "",
    val email: String = "",
    val phone: String = "",
)

sealed interface ProfileEvent {
    data object LogoutSuccess : ProfileEvent
    data object UserUpdated : ProfileEvent
    data class Message(val value: String) : ProfileEvent
}

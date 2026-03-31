package com.example.ocrjizhang.ui.auth

data class AuthFieldState(
    val value: String = "",
    val error: String? = null,
)

data class LoginUiState(
    val username: AuthFieldState = AuthFieldState(),
    val password: AuthFieldState = AuthFieldState(),
    val isLoading: Boolean = false,
)

data class RegisterUiState(
    val username: AuthFieldState = AuthFieldState(),
    val password: AuthFieldState = AuthFieldState(),
    val nickname: AuthFieldState = AuthFieldState(),
    val email: AuthFieldState = AuthFieldState(),
    val phone: AuthFieldState = AuthFieldState(),
    val isLoading: Boolean = false,
)

sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object RegisterSuccess : AuthEvent
    data object LogoutSuccess : AuthEvent
    data class Error(val message: String) : AuthEvent
}

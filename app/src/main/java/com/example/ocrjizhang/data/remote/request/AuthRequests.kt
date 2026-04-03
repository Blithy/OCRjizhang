package com.example.ocrjizhang.data.remote.request

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String?,
    val phone: String?,
    val nickname: String?,
)

data class UpdateCurrentUserRequest(
    val nickname: String?,
    val email: String?,
    val phone: String?,
    val password: String?,
)

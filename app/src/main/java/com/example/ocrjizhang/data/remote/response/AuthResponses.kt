package com.example.ocrjizhang.data.remote.response

data class AuthPayloadDto(
    val token: String,
    val userId: Long,
    val username: String,
    val nickname: String?,
)

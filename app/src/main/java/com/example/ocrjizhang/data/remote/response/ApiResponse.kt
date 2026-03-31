package com.example.ocrjizhang.data.remote.response

data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?,
)

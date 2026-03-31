package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.remote.request.LoginRequest
import com.example.ocrjizhang.data.remote.request.RegisterRequest
import com.example.ocrjizhang.data.remote.service.AuthService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val sessionManager: SessionManager,
) {

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val response = authService.login(
            LoginRequest(
                username = username.trim(),
                password = password,
            ),
        )
        if (response.code != 0 || response.data == null) {
            error(response.msg.ifBlank { "登录失败" })
        }
        sessionManager.saveSession(
            SessionSnapshot(
                token = response.data.token,
                userId = response.data.userId,
                username = response.data.username,
                nickname = response.data.nickname.orEmpty(),
            ),
        )
    }

    suspend fun register(
        username: String,
        password: String,
        nickname: String,
        email: String,
        phone: String,
    ): Result<Unit> = runCatching {
        val response = authService.register(
            RegisterRequest(
                username = username.trim(),
                password = password,
                nickname = nickname.trim().ifBlank { null },
                email = email.trim().ifBlank { null },
                phone = phone.trim().ifBlank { null },
            ),
        )
        if (response.code != 0) {
            error(response.msg.ifBlank { "注册失败" })
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }
}

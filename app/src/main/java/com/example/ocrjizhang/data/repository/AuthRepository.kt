package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.remote.request.LoginRequest
import com.example.ocrjizhang.data.remote.request.RegisterRequest
import com.example.ocrjizhang.data.remote.service.AuthService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
) {

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        if (DemoAccount.matches(username, password)) {
            sessionManager.saveSession(
                SessionSnapshot(
                    token = DemoAccount.TOKEN,
                    userId = DemoAccount.USER_ID,
                    username = DemoAccount.USERNAME,
                    nickname = DemoAccount.NICKNAME,
                ),
            )
            categoryRepository.ensureDefaultCategories(DemoAccount.USER_ID)
            return@runCatching
        }

        try {
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
            categoryRepository.ensureDefaultCategories(response.data.userId)
        } catch (throwable: IOException) {
            error("当前未连接本地后端。你可以先使用演示账号 demo / 123456 登录体验。")
        }
    }

    suspend fun register(
        username: String,
        password: String,
        nickname: String,
        email: String,
        phone: String,
    ): Result<Unit> = runCatching {
        if (username.trim() == DemoAccount.USERNAME) {
            error("演示账号已经预置，请直接返回登录页体验。")
        }

        try {
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
        } catch (throwable: IOException) {
            error("当前注册仍依赖本地后端。若你只是先测试前端，请直接使用演示账号 demo / 123456 登录。")
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }
}

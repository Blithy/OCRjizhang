package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.dao.UserDao
import com.example.ocrjizhang.data.local.entity.UserEntity
import com.example.ocrjizhang.data.remote.request.LoginRequest
import com.example.ocrjizhang.data.remote.request.RegisterRequest
import com.example.ocrjizhang.data.remote.request.UpdateCurrentUserRequest
import com.example.ocrjizhang.data.remote.service.AuthService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class CurrentUserProfile(
    val userId: Long,
    val username: String,
    val nickname: String,
    val email: String,
    val phone: String,
)

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    private val userDao: UserDao,
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
            saveUserSnapshot(
                userId = DemoAccount.USER_ID,
                username = DemoAccount.USERNAME,
                nickname = DemoAccount.NICKNAME,
                email = null,
                phone = null,
                preserveContactValues = true,
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
            saveUserSnapshot(
                userId = response.data.userId,
                username = response.data.username,
                nickname = response.data.nickname.orEmpty(),
                email = null,
                phone = null,
                preserveContactValues = true,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCurrentUserProfile(): Flow<CurrentUserProfile?> =
        sessionManager.sessionFlow.flatMapLatest { session ->
            val userId = session.userId ?: return@flatMapLatest flowOf(null)
            userDao.observeUser(userId).map { user ->
                CurrentUserProfile(
                    userId = userId,
                    username = user?.username ?: session.username,
                    nickname = user?.nickname ?: session.nickname,
                    email = user?.email.orEmpty(),
                    phone = user?.phone.orEmpty(),
                )
            }
        }

    suspend fun updateCurrentUserProfile(
        nickname: String,
        email: String,
        phone: String,
        password: String,
    ): Result<Unit> = runCatching {
        val session = sessionManager.sessionFlow.first()
        val userId = session.userId ?: error("请先登录后再编辑用户信息。")

        val nicknameValue = nickname.trim().ifBlank { null }
        val emailValue = email.trim().ifBlank { null }
        val phoneValue = phone.trim().ifBlank { null }
        val passwordValue = password.ifBlank { null }

        try {
            val response = authService.updateCurrentUser(
                UpdateCurrentUserRequest(
                    nickname = nicknameValue,
                    email = emailValue,
                    phone = phoneValue,
                    password = passwordValue,
                ),
            )
            if (response.code != 0 || response.data == null) {
                error(response.msg.ifBlank { "用户信息更新失败" })
            }

            val payload = response.data
            sessionManager.saveSession(
                session.copy(
                    username = payload.username,
                    nickname = payload.nickname.orEmpty(),
                ),
            )
            saveUserSnapshot(
                userId = userId,
                username = payload.username,
                nickname = payload.nickname.orEmpty(),
                email = emailValue,
                phone = phoneValue,
                preserveContactValues = false,
            )
        } catch (throwable: IOException) {
            error("更新失败，请检查本地后端是否已启动。")
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    private suspend fun saveUserSnapshot(
        userId: Long,
        username: String,
        nickname: String,
        email: String?,
        phone: String?,
        preserveContactValues: Boolean,
    ) {
        val existing = userDao.observeUser(userId).first()
        val now = System.currentTimeMillis()
        val resolvedEmail = if (preserveContactValues) {
            email ?: existing?.email
        } else {
            email
        }
        val resolvedPhone = if (preserveContactValues) {
            phone ?: existing?.phone
        } else {
            phone
        }
        userDao.upsert(
            UserEntity(
                id = userId,
                username = username,
                nickname = nickname.ifBlank { null },
                email = resolvedEmail,
                phone = resolvedPhone,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }
}

package com.example.ocrjizhang.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.repository.AuthRepository
import com.example.ocrjizhang.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<ProfileEvent>()
    val eventFlow: SharedFlow<ProfileEvent> = _eventFlow.asSharedFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUserProfile().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    nickname = profile?.nickname.orEmpty(),
                    email = profile?.email.orEmpty(),
                    phone = profile?.phone.orEmpty(),
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _eventFlow.emit(ProfileEvent.LogoutSuccess)
        }
    }

    fun syncNow() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val message = syncRepository.pullLatest().fold(
                onSuccess = { result ->
                    when {
                        result.pushedCount == 0 &&
                            result.accountCount == 0 &&
                            result.categoryCount == 0 &&
                            result.transactionCount == 0 ->
                            "已检查云端，当前还没有可拉取的数据。"

                        result.pushedCount == 0 ->
                            "已拉取云端最新数据：当前云端有 ${result.accountCount} 个账户、${result.categoryCount} 个分类、${result.transactionCount} 条交易。"

                        else ->
                            "已先补传 ${result.pushedCount} 条未上传本地变更，再拉取云端最新数据：当前云端有 ${result.accountCount} 个账户、${result.categoryCount} 个分类、${result.transactionCount} 条交易。"
                    }
                },
                onFailure = { throwable ->
                    throwable.message ?: "拉取失败，请检查本地后端和 base.url 配置。"
                },
            )
            _uiState.value = _uiState.value.copy(isSyncing = false)
            _eventFlow.emit(ProfileEvent.Message(message))
        }
    }

    fun updateUserProfile(
        nickname: String,
        email: String,
        phone: String,
        password: String,
    ) {
        if (_uiState.value.isUpdatingUser) return

        if (password.isNotBlank() && password.length < 6) {
            viewModelScope.launch {
                _eventFlow.emit(ProfileEvent.Message("密码至少需要 6 位"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingUser = true)
            authRepository.updateCurrentUserProfile(
                nickname = nickname,
                email = email,
                phone = phone,
                password = password,
            ).fold(
                onSuccess = {
                    _eventFlow.emit(ProfileEvent.UserUpdated)
                },
                onFailure = { throwable ->
                    _eventFlow.emit(
                        ProfileEvent.Message(
                            throwable.message ?: "用户信息更新失败，请稍后重试。",
                        ),
                    )
                },
            )
            _uiState.value = _uiState.value.copy(isUpdatingUser = false)
        }
    }
}

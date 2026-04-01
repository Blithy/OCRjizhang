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
            val message = syncRepository.syncNow().fold(
                onSuccess = { result ->
                    if (result.pushedCount == 0 && result.categoryCount == 0 && result.transactionCount == 0) {
                        "当前没有新的本地改动，远端也还没有数据。"
                    } else {
                        "同步完成：上传 ${result.pushedCount} 条，本地回写 ${result.categoryCount} 个分类、${result.transactionCount} 条交易。"
                    }
                },
                onFailure = { throwable ->
                    throwable.message ?: "同步失败，请检查本地后端和 base.url 配置。"
                },
            )
            _uiState.value = _uiState.value.copy(isSyncing = false)
            _eventFlow.emit(ProfileEvent.Message(message))
        }
    }
}

package com.example.ocrjizhang.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.repository.AuthRepository
import com.example.ocrjizhang.data.repository.DemoAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AuthEvent>()
    val eventFlow: SharedFlow<AuthEvent> = _eventFlow.asSharedFlow()

    fun onUsernameChanged(value: String) {
        _uiState.update {
            it.copy(username = it.username.copy(value = value, error = null))
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(password = it.password.copy(value = value, error = null))
        }
    }

    fun fillDemoAccount() {
        _uiState.update {
            it.copy(
                username = it.username.copy(value = DemoAccount.USERNAME, error = null),
                password = it.password.copy(value = DemoAccount.PASSWORD, error = null),
            )
        }
        loginWith(
            username = DemoAccount.USERNAME,
            password = DemoAccount.PASSWORD,
        )
    }

    fun login() {
        val currentState = _uiState.value
        val usernameError = if (currentState.username.value.isBlank()) {
            "请输入用户名"
        } else {
            null
        }
        val passwordError = when {
            currentState.password.value.isBlank() -> "请输入密码"
            currentState.password.value.length < 6 -> "密码至少需要 6 位"
            else -> null
        }
        if (usernameError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    username = it.username.copy(error = usernameError),
                    password = it.password.copy(error = passwordError),
                )
            }
            return
        }

        loginWith(
            username = currentState.username.value,
            password = currentState.password.value,
        )
    }

    private fun loginWith(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.login(
                username = username,
                password = password,
            )
            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = {
                    _eventFlow.emit(AuthEvent.LoginSuccess)
                },
                onFailure = {
                    _eventFlow.emit(
                        AuthEvent.Error(
                            it.message ?: "请求失败，请稍后重试",
                        ),
                    )
                },
            )
        }
    }
}

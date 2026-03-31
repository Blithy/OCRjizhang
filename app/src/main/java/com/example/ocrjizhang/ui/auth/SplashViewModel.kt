package com.example.ocrjizhang.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    data object Home : SplashDestination
    data object Login : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _destinationFlow = MutableSharedFlow<SplashDestination>(replay = 0)
    val destinationFlow: SharedFlow<SplashDestination> = _destinationFlow.asSharedFlow()

    fun decideStartDestination() {
        viewModelScope.launch {
            val snapshot = sessionManager.sessionFlow.first()
            if (snapshot.token.isBlank()) {
                _destinationFlow.emit(SplashDestination.Login)
            } else {
                _destinationFlow.emit(SplashDestination.Home)
            }
        }
    }
}

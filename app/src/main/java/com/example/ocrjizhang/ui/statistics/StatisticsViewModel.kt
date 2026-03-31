package com.example.ocrjizhang.ui.statistics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StatisticsUiState(
    val selectedPeriod: String = "本月",
)

@HiltViewModel
class StatisticsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()
}

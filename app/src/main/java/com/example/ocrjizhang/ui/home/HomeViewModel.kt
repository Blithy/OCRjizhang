package com.example.ocrjizhang.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.repository.SessionManager
import com.example.ocrjizhang.data.repository.TransactionRepository
import com.example.ocrjizhang.ui.transaction.TransactionListItem
import com.example.ocrjizhang.utils.AccountingFormatters
import com.example.ocrjizhang.utils.TransactionSummaryCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun currentMonthLabel(): String =
    LocalDate.now()
        .format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA))

data class HomeUiState(
    val welcomeTitle: String = "",
    val welcomeSubtitle: String = "",
    val summaryPeriodLabel: String = currentMonthLabel(),
    val incomeLabel: String = "收入 ¥0.00",
    val expenseLabel: String = "支出 ¥0.00",
    val surplusLabel: String = "结余 ¥0.00",
    val statusAccountValue: String = "未登录",
    val statusRecordsValue: String = "0 笔记录",
    val statusLatestValue: String = "等待第一笔记账",
    val recentTransactions: List<TransactionListItem> = emptyList(),
    val recentEmptyTitle: String = "",
    val recentEmptyBody: String = "",
)

sealed interface HomeEvent {
    data class Message(val message: String) : HomeEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val currentUserId = MutableStateFlow<Long?>(null)
    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow: SharedFlow<HomeEvent> = _eventFlow.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = sessionManager.sessionFlow
        .map { snapshot ->
            currentUserId.value = snapshot.userId
            snapshot
        }
        .distinctUntilChanged()
        .flatMapLatest { snapshot ->
            val userId = snapshot.userId
            if (userId == null) {
                flowOf(HomeUiState())
            } else {
                transactionRepository.observeTransactions(userId).map { transactions ->
                    val monthlySummary = TransactionSummaryCalculator.calculateMonthlySummary(transactions)
                    val latestRecord = transactions.firstOrNull()
                    HomeUiState(
                        welcomeTitle = "",
                        welcomeSubtitle = "",
                        summaryPeriodLabel = currentMonthLabel(),
                        incomeLabel = "收入 ${AccountingFormatters.formatFen(monthlySummary.incomeFen)}",
                        expenseLabel = "支出 ${AccountingFormatters.formatFen(monthlySummary.expenseFen)}",
                        surplusLabel = "结余 ${AccountingFormatters.formatFen(monthlySummary.surplusFen)}",
                        statusAccountValue = snapshot.username.ifBlank { "演示账号" },
                        statusRecordsValue = "本地 ${transactions.size} 笔",
                        statusLatestValue = latestRecord?.let {
                            AccountingFormatters.formatDateTime(it.transactionTime)
                        } ?: "等待第一笔记账",
                        recentTransactions = transactions.take(5).map { transaction ->
                            TransactionListItem(
                                id = transaction.id,
                                title = transaction.categoryName,
                                subtitle = buildList {
                                    transaction.merchantName?.takeIf { it.isNotBlank() }?.let(::add)
                                    transaction.remark?.takeIf { it.isNotBlank() }?.let(::add)
                                }.joinToString(" · ").ifBlank { "无备注信息" },
                                meta = AccountingFormatters.formatDateTime(transaction.transactionTime),
                                amountLabel = AccountingFormatters.formatFen(transaction.amountFen),
                                type = transaction.type,
                            )
                        },
                        recentEmptyTitle = "最近还没有记账记录",
                        recentEmptyBody = "先新增一笔收入或支出，首页就会开始显示真实摘要。",
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun deleteTransaction(transactionId: Long) {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("登录状态已失效，请重新登录")
            return
        }

        viewModelScope.launch {
            runCatching {
                transactionRepository.deleteTransaction(userId, transactionId)
            }.onSuccess {
                emitMessage("交易已删除")
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "删除失败，请稍后重试")
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.Message(message))
        }
    }
}

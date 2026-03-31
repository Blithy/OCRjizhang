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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val incomeLabel: String = "收入 ￥0.00",
    val expenseLabel: String = "支出 ￥0.00",
    val surplusLabel: String = "结余 ￥0.00",
    val recentTransactions: List<TransactionListItem> = emptyList(),
    val recentEmptyTitle: String = "",
    val recentEmptyBody: String = "",
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    sessionManager: SessionManager,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = sessionManager.sessionFlow
        .map { it.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(HomeUiState())
            } else {
                transactionRepository.observeTransactions(userId).map { transactions ->
                    val monthlySummary = TransactionSummaryCalculator.calculateMonthlySummary(transactions)
                    HomeUiState(
                        incomeLabel = "收入 ${AccountingFormatters.formatFen(monthlySummary.incomeFen)}",
                        expenseLabel = "支出 ${AccountingFormatters.formatFen(monthlySummary.expenseFen)}",
                        surplusLabel = "结余 ${AccountingFormatters.formatFen(monthlySummary.surplusFen)}",
                        recentTransactions = transactions.take(4).map { transaction ->
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
}

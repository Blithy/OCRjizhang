package com.example.ocrjizhang.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsSnapshot
import com.example.ocrjizhang.data.repository.SessionManager
import com.example.ocrjizhang.data.repository.StatisticsRepository
import com.example.ocrjizhang.utils.AccountingFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val statisticsRepository: StatisticsRepository,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(StatisticsPeriod.MONTH)

    val uiState: StateFlow<StatisticsUiState> = combine(
        sessionManager.sessionFlow
            .map { it.userId }
            .distinctUntilChanged(),
        selectedPeriod,
    ) { userId, period ->
        userId to period
    }.flatMapLatest { (userId, period) ->
        if (userId == null) {
            flowOf(
                StatisticsUiState(
                    isLoading = false,
                    selectedPeriod = period,
                    emptyTitle = "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u67e5\u770b\u7edf\u8ba1",
                    emptyBody = "\u767b\u5f55\u6210\u529f\u540e\uff0c\u7edf\u8ba1\u9875\u5c31\u4f1a\u6839\u636e\u4f60\u7684\u771f\u5b9e\u8bb0\u8d26\u6570\u636e\u81ea\u52a8\u5237\u65b0\u3002",
                ),
            )
        } else {
            statisticsRepository.observeStatistics(userId, period).map { snapshot ->
                snapshot.toUiState(period)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(),
    )

    fun selectPeriod(period: StatisticsPeriod) {
        selectedPeriod.value = period
    }

    private fun StatisticsSnapshot.toUiState(period: StatisticsPeriod): StatisticsUiState =
        StatisticsUiState(
            isLoading = false,
            selectedPeriod = period,
            rangeLabel = range.label,
            incomeLabel = "\u6536\u5165 ${AccountingFormatters.formatFen(summary.incomeFen)}",
            expenseLabel = "\u652f\u51fa ${AccountingFormatters.formatFen(summary.expenseFen)}",
            surplusLabel = "\u7ed3\u4f59 ${AccountingFormatters.formatFen(summary.surplusFen)}",
            hasTransactions = hasTransactions,
            categoryItems = categoryBreakdown.map { category ->
                StatisticsCategoryUiModel(
                    categoryName = category.categoryName,
                    amountLabel = AccountingFormatters.formatFen(category.amountFen),
                    shareLabel = "${(category.share * 100).toInt()}%",
                    amountFen = category.amountFen,
                )
            },
            trendItems = trendBreakdown.map { trend ->
                StatisticsTrendUiModel(
                    label = trend.label,
                    incomeFen = trend.incomeFen,
                    expenseFen = trend.expenseFen,
                )
            },
            emptyTitle = emptyTitleFor(period),
            emptyBody = emptyBodyFor(period),
            categoryEmptyText = categoryEmptyTextFor(period),
            trendEmptyText = trendEmptyTextFor(period),
        )

    private fun emptyTitleFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.DAY -> "今天还没有记账数据"
        StatisticsPeriod.WEEK -> "本周还没有记账数据"
        StatisticsPeriod.MONTH -> "本月还没有记账数据"
    }

    private fun emptyBodyFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.DAY -> "先记一两笔今天的收入或支出，这里就会自动生成当天统计。"
        StatisticsPeriod.WEEK -> "先补充本周的几笔记录，这里就会开始展示本周收支概览。"
        StatisticsPeriod.MONTH -> "先新增本月的几笔记录，这里就会自动汇总月度统计结果。"
    }

    private fun categoryEmptyTextFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.DAY -> "今天还没有可展示的支出分类占比"
        StatisticsPeriod.WEEK -> "本周还没有可展示的支出分类占比"
        StatisticsPeriod.MONTH -> "本月还没有可展示的支出分类占比"
    }

    private fun trendEmptyTextFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.DAY -> "今天还没有可展示的收支趋势"
        StatisticsPeriod.WEEK -> "本周还没有可展示的收支趋势"
        StatisticsPeriod.MONTH -> "本月还没有可展示的收支趋势"
    }
}

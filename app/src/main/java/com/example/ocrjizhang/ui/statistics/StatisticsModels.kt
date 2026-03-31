package com.example.ocrjizhang.ui.statistics

import com.example.ocrjizhang.data.model.StatisticsPeriod

data class StatisticsCategoryUiModel(
    val categoryName: String,
    val amountLabel: String,
    val shareLabel: String,
    val amountFen: Long,
)

data class StatisticsTrendUiModel(
    val label: String,
    val incomeFen: Long,
    val expenseFen: Long,
)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: StatisticsPeriod = StatisticsPeriod.MONTH,
    val rangeLabel: String = "",
    val incomeLabel: String = "\u6536\u5165 \u00a50.00",
    val expenseLabel: String = "\u652f\u51fa \u00a50.00",
    val surplusLabel: String = "\u7ed3\u4f59 \u00a50.00",
    val hasTransactions: Boolean = false,
    val categoryItems: List<StatisticsCategoryUiModel> = emptyList(),
    val trendItems: List<StatisticsTrendUiModel> = emptyList(),
    val emptyTitle: String = "\u5f53\u524d\u5468\u671f\u8fd8\u6ca1\u6709\u8bb0\u8d26\u6570\u636e",
    val emptyBody: String = "\u5148\u65b0\u589e\u51e0\u7b14\u6536\u5165\u6216\u652f\u51fa\uff0c\u8fd9\u91cc\u5c31\u4f1a\u81ea\u52a8\u751f\u6210\u7edf\u8ba1\u56fe\u8868\u3002",
    val categoryEmptyText: String = "\u5f53\u524d\u5468\u671f\u8fd8\u6ca1\u6709\u652f\u51fa\u5206\u7c7b\u5360\u6bd4",
    val trendEmptyText: String = "\u5f53\u524d\u5468\u671f\u8fd8\u6ca1\u6709\u53ef\u5c55\u793a\u7684\u6536\u652f\u8d8b\u52bf",
)

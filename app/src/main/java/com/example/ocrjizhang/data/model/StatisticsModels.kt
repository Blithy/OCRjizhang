package com.example.ocrjizhang.data.model

enum class StatisticsPeriod {
    DAY,
    WEEK,
    MONTH,
}

data class StatisticsRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
)

data class StatisticsSummary(
    val incomeFen: Long,
    val expenseFen: Long,
) {
    val surplusFen: Long
        get() = incomeFen - expenseFen
}

data class CategoryBreakdown(
    val categoryName: String,
    val amountFen: Long,
    val share: Float,
)

data class TrendBreakdown(
    val label: String,
    val incomeFen: Long,
    val expenseFen: Long,
)

data class StatisticsSnapshot(
    val range: StatisticsRange,
    val summary: StatisticsSummary,
    val categoryBreakdown: List<CategoryBreakdown>,
    val trendBreakdown: List<TrendBreakdown>,
    val hasTransactions: Boolean,
)

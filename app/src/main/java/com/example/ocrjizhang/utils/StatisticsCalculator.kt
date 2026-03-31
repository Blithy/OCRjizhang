package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.model.CategoryBreakdown
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsRange
import com.example.ocrjizhang.data.model.StatisticsSnapshot
import com.example.ocrjizhang.data.model.StatisticsSummary
import com.example.ocrjizhang.data.model.TrendBreakdown
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object StatisticsCalculator {
    private const val TODAY_LABEL = "\u4eca\u5929"
    private const val WEEK_PREFIX = "\u7b2c"
    private const val WEEK_SUFFIX = "\u5468"

    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val weekFormatter = DateTimeFormatter.ofPattern("MM.dd", Locale.CHINA)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708", Locale.CHINA)

    fun rangeFor(
        period: StatisticsPeriod,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatisticsRange {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return when (period) {
            StatisticsPeriod.DAY -> {
                val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
                StatisticsRange(
                    startMillis = start,
                    endMillis = end,
                    label = today.format(dayFormatter),
                )
            }

            StatisticsPeriod.WEEK -> {
                val startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endDate = startDate.plusDays(6)
                StatisticsRange(
                    startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1,
                    label = "${startDate.format(weekFormatter)} - ${endDate.format(weekFormatter)}",
                )
            }

            StatisticsPeriod.MONTH -> {
                val startDate = today.withDayOfMonth(1)
                val endDate = startDate.plusMonths(1).minusDays(1)
                StatisticsRange(
                    startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1,
                    label = startDate.format(monthFormatter),
                )
            }
        }
    }

    fun buildSnapshot(
        period: StatisticsPeriod,
        transactions: List<TransactionEntity>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatisticsSnapshot {
        val range = rangeFor(period, nowMillis, zoneId)
        val summary = StatisticsSummary(
            incomeFen = transactions
                .asSequence()
                .filter { it.type == RecordType.INCOME }
                .sumOf { it.amountFen },
            expenseFen = transactions
                .asSequence()
                .filter { it.type == RecordType.EXPENSE }
                .sumOf { it.amountFen },
        )
        val expenseTotal = summary.expenseFen.toFloat()
        val categoryBreakdown = transactions
            .asSequence()
            .filter { it.type == RecordType.EXPENSE }
            .groupBy { it.categoryName }
            .map { (categoryName, categoryTransactions) ->
                val amountFen = categoryTransactions.sumOf { it.amountFen }
                CategoryBreakdown(
                    categoryName = categoryName,
                    amountFen = amountFen,
                    share = if (expenseTotal == 0f) 0f else amountFen / expenseTotal,
                )
            }
            .sortedByDescending { it.amountFen }

        val trendBreakdown = buildTrendBreakdown(
            period = period,
            transactions = transactions,
            range = range,
            zoneId = zoneId,
        )

        return StatisticsSnapshot(
            range = range,
            summary = summary,
            categoryBreakdown = categoryBreakdown,
            trendBreakdown = trendBreakdown,
            hasTransactions = transactions.isNotEmpty(),
        )
    }

    private fun buildTrendBreakdown(
        period: StatisticsPeriod,
        transactions: List<TransactionEntity>,
        range: StatisticsRange,
        zoneId: ZoneId,
    ): List<TrendBreakdown> {
        val startDate = Instant.ofEpochMilli(range.startMillis).atZone(zoneId).toLocalDate()
        return when (period) {
            StatisticsPeriod.DAY -> listOf(
                summarizeBucket(
                    label = TODAY_LABEL,
                    transactions = transactions,
                ),
            )

            StatisticsPeriod.WEEK -> (0L..6L).map { offset ->
                val date = startDate.plusDays(offset)
                summarizeBucket(
                    label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA),
                    transactions = transactions.filter { transaction ->
                        Instant.ofEpochMilli(transaction.transactionTime).atZone(zoneId).toLocalDate() == date
                    },
                )
            }

            StatisticsPeriod.MONTH -> {
                val endDate = Instant.ofEpochMilli(range.endMillis).atZone(zoneId).toLocalDate()
                generateMonthlyBuckets(startDate, endDate).mapIndexed { index, bucketRange ->
                    summarizeBucket(
                        label = "$WEEK_PREFIX${index + 1}$WEEK_SUFFIX",
                        transactions = transactions.filter { transaction ->
                            val transactionDate = Instant.ofEpochMilli(transaction.transactionTime)
                                .atZone(zoneId)
                                .toLocalDate()
                            !transactionDate.isBefore(bucketRange.first) &&
                                !transactionDate.isAfter(bucketRange.second)
                        },
                    )
                }
            }
        }
    }

    private fun generateMonthlyBuckets(
        monthStart: LocalDate,
        monthEnd: LocalDate,
    ): List<Pair<LocalDate, LocalDate>> {
        val buckets = mutableListOf<Pair<LocalDate, LocalDate>>()
        var currentStart = monthStart
        while (!currentStart.isAfter(monthEnd)) {
            val currentEnd = minOf(currentStart.plusDays(6), monthEnd)
            buckets += currentStart to currentEnd
            currentStart = currentEnd.plusDays(1)
        }
        return buckets
    }

    private fun summarizeBucket(
        label: String,
        transactions: List<TransactionEntity>,
    ): TrendBreakdown = TrendBreakdown(
        label = label,
        incomeFen = transactions
            .asSequence()
            .filter { it.type == RecordType.INCOME }
            .sumOf { it.amountFen },
        expenseFen = transactions
            .asSequence()
            .filter { it.type == RecordType.EXPENSE }
            .sumOf { it.amountFen },
    )
}

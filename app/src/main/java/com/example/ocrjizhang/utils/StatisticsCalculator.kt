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
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object StatisticsCalculator {
    const val MIN_SUPPORTED_YEAR: Int = 1970
    const val FUTURE_YEAR_SPAN: Int = 10

    private const val WEEK_PREFIX = "\u7b2c"
    private const val WEEK_SUFFIX = "\u5468"

    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    private val weekFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val shortDayFormatter = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708", Locale.CHINA)
    private val yearFormatter = DateTimeFormatter.ofPattern("yyyy年", Locale.CHINA)
    private val monthBucketFormatter = DateTimeFormatter.ofPattern("M月", Locale.CHINA)
    private val allLabel = "全部时间"

    fun rangeFor(
        period: StatisticsPeriod,
        referenceMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxYear: Int = maxSupportedYear(),
    ): StatisticsRange {
        val safeReference = clampToYearRange(referenceMillis, MIN_SUPPORTED_YEAR, maxYear, zoneId)
        val referenceDate = Instant.ofEpochMilli(safeReference).atZone(zoneId).toLocalDate()
        return when (period) {
            StatisticsPeriod.WEEK -> {
                val startDate = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endDate = startDate.plusDays(6)
                statisticsRange(startDate, endDate, zoneId) {
                    "${startDate.format(weekFormatter)} - ${endDate.format(weekFormatter)}"
                }
            }

            StatisticsPeriod.MONTH -> {
                val startDate = referenceDate.withDayOfMonth(1)
                val endDate = startDate.plusMonths(1).minusDays(1)
                statisticsRange(startDate, endDate, zoneId) {
                    startDate.format(monthFormatter)
                }
            }

            StatisticsPeriod.YEAR -> {
                val startDate = referenceDate.withDayOfYear(1)
                val endDate = startDate.plusYears(1).minusDays(1)
                statisticsRange(startDate, endDate, zoneId) {
                    startDate.format(yearFormatter)
                }
            }

            StatisticsPeriod.ALL -> {
                val startDate = LocalDate.of(1970, 1, 1)
                val endDate = LocalDate.of(maxYear, 12, 31)
                statisticsRange(startDate, endDate, zoneId) { allLabel }
            }

            StatisticsPeriod.CUSTOM -> {
                defaultCustomRange(referenceMillis, zoneId)
            }
        }
    }

    fun defaultCustomRange(
        referenceMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxYear: Int = maxSupportedYear(),
    ): StatisticsRange {
        val safeReference = clampToYearRange(referenceMillis, MIN_SUPPORTED_YEAR, maxYear, zoneId)
        val endDate = Instant.ofEpochMilli(safeReference).atZone(zoneId).toLocalDate()
        val startDate = endDate.minusDays(29)
        return customRange(
            startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            zoneId = zoneId,
        )
    }

    fun customRange(
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatisticsRange {
        val startDate = Instant.ofEpochMilli(minOf(startMillis, endMillis)).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(maxOf(startMillis, endMillis)).atZone(zoneId).toLocalDate()
        return statisticsRange(startDate, endDate, zoneId) { formatCustomLabel(startDate, endDate) }
    }

    fun shiftReference(
        period: StatisticsPeriod,
        referenceMillis: Long,
        step: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
        minYear: Int = MIN_SUPPORTED_YEAR,
        maxYear: Int = maxSupportedYear(),
    ): Long {
        val referenceDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
        val shiftedDate = when (period) {
            StatisticsPeriod.WEEK -> referenceDate.plusWeeks(step.toLong())
            StatisticsPeriod.MONTH -> referenceDate.plusMonths(step.toLong())
            StatisticsPeriod.YEAR -> referenceDate.plusYears(step.toLong())
            else -> referenceDate
        }
        return clampToYearRange(
            shiftedDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            minYear = minYear,
            maxYear = maxYear,
            zoneId = zoneId,
        )
    }

    fun shiftRange(
        period: StatisticsPeriod,
        range: StatisticsRange,
        step: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
        minYear: Int = MIN_SUPPORTED_YEAR,
        maxYear: Int = maxSupportedYear(),
    ): StatisticsRange {
        if (period == StatisticsPeriod.ALL) return range
        val startDate = Instant.ofEpochMilli(range.startMillis).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(range.endMillis).atZone(zoneId).toLocalDate()
        val shifted = when (period) {
            StatisticsPeriod.WEEK -> statisticsRange(
                startDate.plusWeeks(step.toLong()),
                endDate.plusWeeks(step.toLong()),
                zoneId,
            ) { shiftedStart ->
                val shiftedEnd = endDate.plusWeeks(step.toLong())
                "${shiftedStart.format(weekFormatter)} - ${shiftedEnd.format(weekFormatter)}"
            }

            StatisticsPeriod.MONTH -> {
                val shiftedStart = startDate.plusMonths(step.toLong()).withDayOfMonth(1)
                val shiftedEnd = shiftedStart.plusMonths(1).minusDays(1)
                statisticsRange(shiftedStart, shiftedEnd, zoneId) {
                    shiftedStart.format(monthFormatter)
                }
            }

            StatisticsPeriod.YEAR -> {
                val shiftedStart = startDate.plusYears(step.toLong()).withDayOfYear(1)
                val shiftedEnd = shiftedStart.plusYears(1).minusDays(1)
                statisticsRange(shiftedStart, shiftedEnd, zoneId) {
                    shiftedStart.format(yearFormatter)
                }
            }

            StatisticsPeriod.CUSTOM -> {
                val spanDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
                val shiftedStart = startDate.plusDays(spanDays * step.toLong())
                val shiftedEnd = endDate.plusDays(spanDays * step.toLong())
                statisticsRange(shiftedStart, shiftedEnd, zoneId) {
                    formatCustomLabel(shiftedStart, shiftedEnd)
                }
            }

            StatisticsPeriod.ALL -> range
        }
        val minBoundary = LocalDate.of(minYear, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val maxBoundary = maxSupportedEndMillis(maxYear, zoneId)
        if (shifted.startMillis < minBoundary || shifted.endMillis > maxBoundary) {
            return range
        }
        return shifted
    }

    fun endOfTodayMillis(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }

    fun maxSupportedYear(
        nowMillis: Long = System.currentTimeMillis(),
        futureYearSpan: Int = FUTURE_YEAR_SPAN,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int = Instant.ofEpochMilli(nowMillis).atZone(zoneId).year + futureYearSpan

    fun maxSupportedEndMillis(
        maxYear: Int = maxSupportedYear(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long = LocalDate.of(maxYear, 12, 31)
        .plusDays(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli() - 1

    fun weekReferenceFromDate(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun monthReference(year: Int, month: Int, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.of(year, month, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun yearReference(year: Int, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun clampToYearRange(
        epochMillis: Long,
        minYear: Int = MIN_SUPPORTED_YEAR,
        maxYear: Int = maxSupportedYear(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val minBoundary = LocalDate.of(minYear, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val maxBoundary = maxSupportedEndMillis(maxYear, zoneId)
        return epochMillis.coerceIn(minBoundary, maxBoundary)
    }

    fun buildSnapshot(
        period: StatisticsPeriod,
        range: StatisticsRange,
        transactions: List<TransactionEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): StatisticsSnapshot {
        val dayCount = maxOf(
            1L,
            ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(range.startMillis).atZone(zoneId).toLocalDate(),
                Instant.ofEpochMilli(range.endMillis).atZone(zoneId).toLocalDate(),
            ) + 1,
        )
        val incomeTotal = transactions
            .asSequence()
            .filter { it.type == RecordType.INCOME }
            .sumOf { it.amountFen }
        val expenseTotalFen = transactions
            .asSequence()
            .filter { it.type == RecordType.EXPENSE }
            .sumOf { it.amountFen }
        val summary = StatisticsSummary(
            incomeFen = incomeTotal,
            expenseFen = expenseTotalFen,
            averageDailyExpenseFen = if (expenseTotalFen == 0L) 0L else expenseTotalFen / dayCount,
            pendingReimburseFen = transactions.sumByAmount(
                type = RecordType.EXPENSE,
                keywords = listOf("报销"),
            ),
            reimbursedInFen = transactions.sumByAmount(
                type = RecordType.INCOME,
                keywords = listOf("报销"),
            ),
            repaymentFen = transactions.sumByAmount(
                type = RecordType.EXPENSE,
                keywords = listOf("还款", "还贷"),
            ),
            collectionFen = transactions.sumByAmount(
                type = RecordType.INCOME,
                keywords = listOf("收款", "回款"),
            ),
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
        val endDate = Instant.ofEpochMilli(range.endMillis).atZone(zoneId).toLocalDate()
        return when (period) {
            StatisticsPeriod.WEEK -> generateDailyBuckets(startDate, endDate).map { bucket ->
                summarizeBucket(bucket, transactions, zoneId)
            }

            StatisticsPeriod.MONTH -> generateWeeklyBuckets(startDate, endDate).mapIndexed { index, bucket ->
                summarizeBucket(bucket.copy(label = "$WEEK_PREFIX${index + 1}$WEEK_SUFFIX"), transactions, zoneId)
            }

            StatisticsPeriod.YEAR -> generateMonthBuckets(startDate, endDate).map { bucket ->
                summarizeBucket(bucket, transactions, zoneId)
            }

            StatisticsPeriod.ALL -> buildAllBuckets(transactions, zoneId).map { bucket ->
                summarizeBucket(bucket, transactions, zoneId)
            }

            StatisticsPeriod.CUSTOM -> generateCustomBuckets(startDate, endDate).map { bucket ->
                summarizeBucket(bucket, transactions, zoneId)
            }
        }
    }

    private fun generateDailyBuckets(startDate: LocalDate, endDate: LocalDate): List<TrendBucket> {
        val buckets = mutableListOf<TrendBucket>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            buckets += TrendBucket(
                label = current.format(shortDayFormatter),
                start = current,
                end = current,
            )
            current = current.plusDays(1)
        }
        return buckets
    }

    private fun generateWeeklyBuckets(startDate: LocalDate, endDate: LocalDate): List<TrendBucket> {
        val buckets = mutableListOf<TrendBucket>()
        var currentStart = startDate
        while (!currentStart.isAfter(endDate)) {
            val currentEnd = minOf(currentStart.plusDays(6), endDate)
            buckets += TrendBucket(
                label = "${currentStart.format(weekFormatter)}-${currentEnd.format(weekFormatter)}",
                start = currentStart,
                end = currentEnd,
            )
            currentStart = currentEnd.plusDays(1)
        }
        return buckets
    }

    private fun generateMonthBuckets(startDate: LocalDate, endDate: LocalDate): List<TrendBucket> {
        val buckets = mutableListOf<TrendBucket>()
        var current = YearMonth.from(startDate)
        val last = YearMonth.from(endDate)
        while (!current.isAfter(last)) {
            val bucketStart = current.atDay(1)
            val bucketEnd = current.atEndOfMonth()
            buckets += TrendBucket(
                label = current.atDay(1).format(monthBucketFormatter),
                start = maxOf(bucketStart, startDate),
                end = minOf(bucketEnd, endDate),
            )
            current = current.plusMonths(1)
        }
        return buckets
    }

    private fun generateCustomBuckets(startDate: LocalDate, endDate: LocalDate): List<TrendBucket> {
        val spanDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
        return when {
            spanDays <= 31 -> generateDailyBuckets(startDate, endDate)
            spanDays <= 180 -> generateWeeklyBuckets(startDate, endDate)
            else -> generateMonthBuckets(startDate, endDate)
        }
    }

    private fun buildAllBuckets(
        transactions: List<TransactionEntity>,
        zoneId: ZoneId,
    ): List<TrendBucket> {
        if (transactions.isEmpty()) return emptyList()
        val months = transactions
            .asSequence()
            .map { transaction ->
                YearMonth.from(Instant.ofEpochMilli(transaction.transactionTime).atZone(zoneId).toLocalDate())
            }
            .distinct()
            .sorted()
            .toList()
        if (months.isEmpty()) return emptyList()
        val recentMonths = if (months.size <= 12) months else months.takeLast(12)
        return recentMonths.map { month ->
            TrendBucket(
                label = month.atDay(1).format(monthBucketFormatter),
                start = month.atDay(1),
                end = month.atEndOfMonth(),
            )
        }
    }

    private fun summarizeBucket(
        bucket: TrendBucket,
        transactions: List<TransactionEntity>,
        zoneId: ZoneId,
    ): TrendBreakdown {
        val bucketTransactions = transactions.filter { transaction ->
            val transactionDate = Instant.ofEpochMilli(transaction.transactionTime).atZone(zoneId).toLocalDate()
            !transactionDate.isBefore(bucket.start) && !transactionDate.isAfter(bucket.end)
        }
        return TrendBreakdown(
            label = bucket.label,
            incomeFen = bucketTransactions
            .asSequence()
            .filter { it.type == RecordType.INCOME }
            .sumOf { it.amountFen },
            expenseFen = bucketTransactions
            .asSequence()
            .filter { it.type == RecordType.EXPENSE }
            .sumOf { it.amountFen },
        )
    }

    private fun List<TransactionEntity>.sumByAmount(
        type: RecordType,
        keywords: List<String>,
    ): Long = asSequence()
        .filter { it.type == type }
        .filter { transaction -> transaction.matchesKeyword(keywords) }
        .sumOf { it.amountFen }

    private fun TransactionEntity.matchesKeyword(keywords: List<String>): Boolean {
        val searchText = buildString {
            append(categoryName)
            append(' ')
            append(remark.orEmpty())
            append(' ')
            append(merchantName.orEmpty())
        }
        return keywords.any { keyword -> searchText.contains(keyword, ignoreCase = true) }
    }

    private fun statisticsRange(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId,
        labelBuilder: (LocalDate) -> String,
    ): StatisticsRange = StatisticsRange(
        startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1,
        label = labelBuilder(startDate),
    )

    private fun formatCustomLabel(startDate: LocalDate, endDate: LocalDate): String {
        if (startDate == endDate) {
            return startDate.format(fullDateFormatter)
        }
        return "${startDate.format(fullDateFormatter)} - ${endDate.format(fullDateFormatter)}"
    }

    private data class TrendBucket(
        val label: String,
        val start: LocalDate,
        val end: LocalDate,
    )
}

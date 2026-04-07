package com.example.ocrjizhang.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsRange
import com.example.ocrjizhang.data.model.StatisticsSnapshot
import com.example.ocrjizhang.data.repository.SessionManager
import com.example.ocrjizhang.data.repository.StatisticsRepository
import com.example.ocrjizhang.utils.AccountingFormatters
import com.example.ocrjizhang.utils.StatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateLabelFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA)

    private val minYear: Int = StatisticsCalculator.MIN_SUPPORTED_YEAR
    private val maxYear: Int = StatisticsCalculator.maxSupportedYear(zoneId = zoneId)
    private val maxEndMillis: Long = StatisticsCalculator.maxSupportedEndMillis(maxYear, zoneId)

    private val selectedPeriod = MutableStateFlow(StatisticsPeriod.MONTH)
    private val referenceMillis = MutableStateFlow(System.currentTimeMillis())
    private val customRange = MutableStateFlow<StatisticsRange?>(null)

    val uiState: StateFlow<StatisticsUiState> = combine(
        sessionManager.sessionFlow
            .map { it.userId }
            .distinctUntilChanged(),
        selectedPeriod,
        referenceMillis,
        customRange,
    ) { userId, period, reference, custom ->
        StatisticsQuery(
            userId = userId,
            period = period,
            referenceMillis = StatisticsCalculator.clampToYearRange(
                epochMillis = reference,
                minYear = minYear,
                maxYear = maxYear,
                zoneId = zoneId,
            ),
            customRange = custom,
        )
    }.flatMapLatest { query ->
        val userId = query.userId
        val period = query.period
        if (userId == null) {
            flowOf(
                StatisticsUiState(
                    isLoading = false,
                    selectedPeriod = period,
                    minYear = minYear,
                    maxYear = maxYear,
                    emptyTitle = "请先登录后再查看统计",
                    emptyBody = "登录成功后，统计页就会根据你的真实记账数据自动刷新。",
                ),
            )
        } else {
            val range = resolveRange(period, query.referenceMillis, query.customRange)
            statisticsRepository.observeStatistics(userId, period, range).map { snapshot ->
                snapshot.toUiState(
                    period = period,
                    range = range,
                    canNavigatePrevious = canNavigate(period, range, step = -1),
                    canNavigateNext = canNavigate(period, range, step = 1),
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(minYear = minYear, maxYear = maxYear),
    )

    fun selectPeriod(period: StatisticsPeriod) {
        selectedPeriod.value = period
        when (period) {
            StatisticsPeriod.CUSTOM -> {
                if (customRange.value == null) {
                    customRange.value = StatisticsCalculator.defaultCustomRange(
                        referenceMillis = referenceMillis.value,
                        zoneId = zoneId,
                        maxYear = maxYear,
                    )
                }
            }

            StatisticsPeriod.WEEK,
            StatisticsPeriod.MONTH,
            StatisticsPeriod.YEAR,
            -> referenceMillis.value = StatisticsCalculator.clampToYearRange(
                epochMillis = referenceMillis.value,
                minYear = minYear,
                maxYear = maxYear,
                zoneId = zoneId,
            )

            StatisticsPeriod.ALL -> Unit
        }
    }

    fun selectWeekByDate(epochMillis: Long) {
        selectedPeriod.value = StatisticsPeriod.WEEK
        referenceMillis.value = StatisticsCalculator.weekReferenceFromDate(
            epochMillis = StatisticsCalculator.clampToYearRange(epochMillis, minYear, maxYear, zoneId),
            zoneId = zoneId,
        )
    }

    fun selectMonth(year: Int, month: Int) {
        selectedPeriod.value = StatisticsPeriod.MONTH
        referenceMillis.value = StatisticsCalculator.monthReference(
            year = year.coerceIn(minYear, maxYear),
            month = month.coerceIn(1, 12),
            zoneId = zoneId,
        )
    }

    fun selectYear(year: Int) {
        selectedPeriod.value = StatisticsPeriod.YEAR
        referenceMillis.value = StatisticsCalculator.yearReference(
            year = year.coerceIn(minYear, maxYear),
            zoneId = zoneId,
        )
    }

    fun selectCustomRange(startMillis: Long, endMillis: Long) {
        customRange.value = StatisticsCalculator.customRange(
            startMillis = StatisticsCalculator.clampToYearRange(startMillis, minYear, maxYear, zoneId),
            endMillis = StatisticsCalculator.clampToYearRange(endMillis, minYear, maxYear, zoneId),
            zoneId = zoneId,
        )
        selectedPeriod.value = StatisticsPeriod.CUSTOM
    }

    fun selectRangeStart(startMillis: Long) {
        val existingRange = resolveRange(StatisticsPeriod.CUSTOM, referenceMillis.value, customRange.value)
        val updated = normalizeCustomRange(existingRange, startMillis = startMillis, endMillis = null, zoneId = zoneId)
        selectCustomRange(updated.startMillis, updated.endMillis)
    }

    fun selectRangeEnd(endMillis: Long) {
        val existingRange = resolveRange(StatisticsPeriod.CUSTOM, referenceMillis.value, customRange.value)
        val updated = normalizeCustomRange(existingRange, startMillis = null, endMillis = endMillis, zoneId = zoneId)
        selectCustomRange(updated.startMillis, updated.endMillis)
    }

    fun moveToPreviousRange() {
        val period = selectedPeriod.value
        if (period == StatisticsPeriod.ALL) return
        val currentRange = resolveRange(period, referenceMillis.value, customRange.value)
        val shiftedRange = StatisticsCalculator.shiftRange(
            period = period,
            range = currentRange,
            step = -1,
            zoneId = zoneId,
            minYear = minYear,
            maxYear = maxYear,
        )
        if (shiftedRange == currentRange) return
        applyShiftedRange(period, shiftedRange)
    }

    fun moveToNextRange() {
        val period = selectedPeriod.value
        if (period == StatisticsPeriod.ALL) return
        val currentRange = resolveRange(period, referenceMillis.value, customRange.value)
        val shiftedRange = StatisticsCalculator.shiftRange(
            period = period,
            range = currentRange,
            step = 1,
            zoneId = zoneId,
            minYear = minYear,
            maxYear = maxYear,
        )
        if (shiftedRange == currentRange || shiftedRange.endMillis > maxEndMillis) return
        applyShiftedRange(period, shiftedRange)
    }

    private fun applyShiftedRange(period: StatisticsPeriod, shiftedRange: StatisticsRange) {
        when (period) {
            StatisticsPeriod.WEEK,
            StatisticsPeriod.MONTH,
            StatisticsPeriod.YEAR,
            -> referenceMillis.value = shiftedRange.startMillis

            StatisticsPeriod.CUSTOM -> customRange.value = shiftedRange
            StatisticsPeriod.ALL -> Unit
        }
    }

    private fun StatisticsSnapshot.toUiState(
        period: StatisticsPeriod,
        range: StatisticsRange,
        canNavigatePrevious: Boolean,
        canNavigateNext: Boolean,
    ): StatisticsUiState {
        val assetTrend = buildAssetTrendItems(trendBreakdown)
        val averageExpenseByBucketFen = if (trendBreakdown.isEmpty()) {
            0L
        } else {
            trendBreakdown.sumOf { it.expenseFen } / trendBreakdown.size
        }

        val displayMode = when (period) {
            StatisticsPeriod.ALL -> StatisticsTimeDisplayMode.ALL_READ_ONLY
            StatisticsPeriod.CUSTOM -> StatisticsTimeDisplayMode.RANGE_LABEL
            else -> StatisticsTimeDisplayMode.SINGLE_LABEL
        }

        return StatisticsUiState(
            isLoading = false,
            selectedPeriod = period,
            rangeLabel = range.label,
            rangeStartLabel = formatDate(range.startMillis),
            rangeEndLabel = formatDate(range.endMillis),
            timeDisplayMode = displayMode,
            rangeStartMillis = range.startMillis,
            rangeEndMillis = range.endMillis,
            minYear = minYear,
            maxYear = maxYear,
            canNavigatePrevious = canNavigatePrevious,
            canNavigateNext = canNavigateNext,
            incomeLabel = AccountingFormatters.formatFen(summary.incomeFen),
            expenseLabel = AccountingFormatters.formatFen(summary.expenseFen),
            surplusLabel = AccountingFormatters.formatFen(summary.surplusFen),
            averageExpenseLabel = AccountingFormatters.formatFen(summary.averageDailyExpenseFen),
            pendingReimburseLabel = AccountingFormatters.formatFen(summary.pendingReimburseFen),
            reimbursedLabel = AccountingFormatters.formatFen(summary.reimbursedInFen),
            repaymentLabel = AccountingFormatters.formatFen(summary.repaymentFen),
            collectionLabel = AccountingFormatters.formatFen(summary.collectionFen),
            expenseChartAverageLabel = "平均值：${AccountingFormatters.formatFen(averageExpenseByBucketFen)}",
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
            assetTrendItems = assetTrend,
            emptyTitle = emptyTitleFor(period),
            emptyBody = emptyBodyFor(period),
            categoryEmptyText = categoryEmptyTextFor(period),
            trendEmptyText = trendEmptyTextFor(period),
            assetTrendEmptyText = assetTrendEmptyTextFor(period),
        )
    }

    private fun resolveRange(
        period: StatisticsPeriod,
        referenceMillis: Long,
        custom: StatisticsRange?,
    ): StatisticsRange = when (period) {
        StatisticsPeriod.CUSTOM -> custom ?: StatisticsCalculator.defaultCustomRange(
            referenceMillis = referenceMillis,
            zoneId = zoneId,
            maxYear = maxYear,
        )

        else -> StatisticsCalculator.rangeFor(
            period = period,
            referenceMillis = referenceMillis,
            zoneId = zoneId,
            maxYear = maxYear,
        )
    }

    private fun canNavigate(period: StatisticsPeriod, range: StatisticsRange, step: Int): Boolean {
        return canNavigateByShift(
            period = period,
            range = range,
            step = step,
            minYear = minYear,
            maxYear = maxYear,
            zoneId = zoneId,
        )
    }

    private fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().format(dateLabelFormatter)

    private fun emptyTitleFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEK -> "本周还没有记账数据"
        StatisticsPeriod.MONTH -> "本月还没有记账数据"
        StatisticsPeriod.YEAR -> "今年还没有记账数据"
        StatisticsPeriod.ALL -> "还没有历史记账数据"
        StatisticsPeriod.CUSTOM -> "该范围内还没有记账数据"
    }

    private fun emptyBodyFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEK -> "先补充本周的几笔记录，这里就会开始展示本周收支概览。"
        StatisticsPeriod.MONTH -> "先新增本月的几笔记录，这里就会自动汇总月度统计结果。"
        StatisticsPeriod.YEAR -> "先补充几个月的交易记录，这里会自动汇总全年趋势。"
        StatisticsPeriod.ALL -> "先新增一笔收入或支出，统计页就会自动开始累计历史数据。"
        StatisticsPeriod.CUSTOM -> "先在该时间范围内新增记录，这里会自动刷新统计结果。"
    }

    private fun categoryEmptyTextFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEK -> "本周还没有可展示的支出分类占比"
        StatisticsPeriod.MONTH -> "本月还没有可展示的支出分类占比"
        StatisticsPeriod.YEAR -> "今年还没有可展示的支出分类占比"
        StatisticsPeriod.ALL -> "当前还没有可展示的支出分类占比"
        StatisticsPeriod.CUSTOM -> "该范围还没有可展示的支出分类占比"
    }

    private fun trendEmptyTextFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEK -> "本周还没有可展示的支出趋势"
        StatisticsPeriod.MONTH -> "本月还没有可展示的支出趋势"
        StatisticsPeriod.YEAR -> "今年还没有可展示的支出趋势"
        StatisticsPeriod.ALL -> "当前还没有可展示的支出趋势"
        StatisticsPeriod.CUSTOM -> "该范围还没有可展示的支出趋势"
    }

    private fun assetTrendEmptyTextFor(period: StatisticsPeriod): String = when (period) {
        StatisticsPeriod.WEEK -> "本周还没有可展示的资产趋势"
        StatisticsPeriod.MONTH -> "本月还没有可展示的资产趋势"
        StatisticsPeriod.YEAR -> "今年还没有可展示的资产趋势"
        StatisticsPeriod.ALL -> "当前还没有可展示的资产趋势"
        StatisticsPeriod.CUSTOM -> "该范围还没有可展示的资产趋势"
    }

    private data class StatisticsQuery(
        val userId: Long?,
        val period: StatisticsPeriod,
        val referenceMillis: Long,
        val customRange: StatisticsRange?,
    )

    companion object {
        internal fun canNavigateByShift(
            period: StatisticsPeriod,
            range: StatisticsRange,
            step: Int,
            minYear: Int,
            maxYear: Int,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Boolean {
            if (period == StatisticsPeriod.ALL) return false
            val shiftedRange = StatisticsCalculator.shiftRange(
                period = period,
                range = range,
                step = step,
                zoneId = zoneId,
                minYear = minYear,
                maxYear = maxYear,
            )
            return shiftedRange != range
        }

        internal fun normalizeCustomRange(
            currentRange: StatisticsRange,
            startMillis: Long?,
            endMillis: Long?,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): StatisticsRange {
            val start = startMillis ?: currentRange.startMillis
            val end = endMillis ?: currentRange.endMillis
            return StatisticsCalculator.customRange(start, end, zoneId)
        }

        internal fun buildAssetTrendItems(
            trendBreakdown: List<com.example.ocrjizhang.data.model.TrendBreakdown>,
        ): List<StatisticsAssetTrendUiModel> {
            var runningAssetFen = 0L
            return trendBreakdown.map { trend ->
                runningAssetFen += trend.incomeFen - trend.expenseFen
                StatisticsAssetTrendUiModel(
                    label = trend.label,
                    amountFen = runningAssetFen,
                )
            }
        }
    }
}

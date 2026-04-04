package com.example.ocrjizhang.ui.statistics

import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsRange
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsViewModelTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `week month year can navigate forward within max year limit`() {
        val maxYear = 2036
        val weekRange = range("2036-12-15", "2036-12-21")
        val monthRange = range("2036-11-01", "2036-11-30")
        val yearRange = range("2035-01-01", "2035-12-31")

        assertTrue(
            StatisticsViewModel.canNavigateByShift(
                period = StatisticsPeriod.WEEK,
                range = weekRange,
                step = 1,
                minYear = 1970,
                maxYear = maxYear,
                zoneId = zoneId,
            ),
        )
        assertTrue(
            StatisticsViewModel.canNavigateByShift(
                period = StatisticsPeriod.MONTH,
                range = monthRange,
                step = 1,
                minYear = 1970,
                maxYear = maxYear,
                zoneId = zoneId,
            ),
        )
        assertTrue(
            StatisticsViewModel.canNavigateByShift(
                period = StatisticsPeriod.YEAR,
                range = yearRange,
                step = 1,
                minYear = 1970,
                maxYear = maxYear,
                zoneId = zoneId,
            ),
        )
    }

    @Test
    fun `all period cannot navigate`() {
        val allRange = range("1970-01-01", "2036-12-31")
        assertFalse(
            StatisticsViewModel.canNavigateByShift(
                period = StatisticsPeriod.ALL,
                range = allRange,
                step = 1,
                minYear = 1970,
                maxYear = 2036,
                zoneId = zoneId,
            ),
        )
        assertFalse(
            StatisticsViewModel.canNavigateByShift(
                period = StatisticsPeriod.ALL,
                range = allRange,
                step = -1,
                minYear = 1970,
                maxYear = 2036,
                zoneId = zoneId,
            ),
        )
    }

    @Test
    fun `custom range start end update keeps normalized query range`() {
        val current = range("2026-04-01", "2026-04-10")
        val updatedStart = date("2026-04-18")
        val normalized = StatisticsViewModel.normalizeCustomRange(
            currentRange = current,
            startMillis = updatedStart,
            endMillis = null,
            zoneId = zoneId,
        )
        val updatedEnd = date("2026-03-22")
        val normalizedTwice = StatisticsViewModel.normalizeCustomRange(
            currentRange = normalized,
            startMillis = null,
            endMillis = updatedEnd,
            zoneId = zoneId,
        )

        assertTrue(normalized.startMillis <= normalized.endMillis)
        assertTrue(normalizedTwice.startMillis <= normalizedTwice.endMillis)
        assertEquals("2026-03-22", formatDate(normalizedTwice.startMillis))
        assertEquals("2026-04-10", formatDate(normalizedTwice.endMillis))
    }

    private fun range(startIso: String, endIso: String): StatisticsRange = StatisticsRange(
        startMillis = date(startIso),
        endMillis = endOfDay(endIso),
        label = "$startIso~$endIso",
    )

    private fun date(isoDate: String): Long =
        LocalDate.parse(isoDate).atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun endOfDay(isoDate: String): Long =
        LocalDate.parse(isoDate).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

    private fun formatDate(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()
}

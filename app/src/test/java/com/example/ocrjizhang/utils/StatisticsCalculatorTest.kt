package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.TransactionSource
import com.example.ocrjizhang.data.model.StatisticsPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsCalculatorTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `weekReferenceFromDate snaps to monday`() {
        val wednesday = localDate("2026-04-08")
        val monday = StatisticsCalculator.weekReferenceFromDate(wednesday, zoneId)
        assertEquals("2026-04-06", formatDate(monday))
    }

    @Test
    fun `rangeFor month and year produces expected labels`() {
        val reference = localDate("2026-04-05")
        val monthRange = StatisticsCalculator.rangeFor(
            period = StatisticsPeriod.MONTH,
            referenceMillis = reference,
            zoneId = zoneId,
            maxYear = 2036,
        )
        val yearRange = StatisticsCalculator.rangeFor(
            period = StatisticsPeriod.YEAR,
            referenceMillis = reference,
            zoneId = zoneId,
            maxYear = 2036,
        )

        assertEquals("2026年04月", monthRange.label)
        assertEquals("2026年", yearRange.label)
    }

    @Test
    fun `customRange normalizes reversed start and end`() {
        val start = localDate("2026-04-30")
        val end = localDate("2026-04-01")
        val range = StatisticsCalculator.customRange(startMillis = start, endMillis = end, zoneId = zoneId)
        assertTrue(range.startMillis <= range.endMillis)
        assertEquals("2026-04-01", formatDate(range.startMillis))
        assertEquals("2026-04-30", formatDate(range.endMillis))
    }

    @Test
    fun `shiftRange blocks next navigation beyond maxYear end`() {
        val maxYear = 2036
        val decemberRef = StatisticsCalculator.monthReference(maxYear, 12, zoneId)
        val decemberRange = StatisticsCalculator.rangeFor(
            period = StatisticsPeriod.MONTH,
            referenceMillis = decemberRef,
            zoneId = zoneId,
            maxYear = maxYear,
        )

        val shifted = StatisticsCalculator.shiftRange(
            period = StatisticsPeriod.MONTH,
            range = decemberRange,
            step = 1,
            zoneId = zoneId,
            minYear = StatisticsCalculator.MIN_SUPPORTED_YEAR,
            maxYear = maxYear,
        )

        assertEquals(decemberRange.startMillis, shifted.startMillis)
        assertEquals(decemberRange.endMillis, shifted.endMillis)
    }

    @Test
    fun `buildSnapshot aggregates summary category and weekly trend`() {
        val monday = localDate("2026-04-06")
        val tuesday = localDate("2026-04-07")
        val thursday = localDate("2026-04-09")
        val reference = localDate("2026-04-09")
        val range = StatisticsCalculator.rangeFor(
            period = StatisticsPeriod.WEEK,
            referenceMillis = reference,
            zoneId = zoneId,
            maxYear = 2036,
        )

        val snapshot = StatisticsCalculator.buildSnapshot(
            period = StatisticsPeriod.WEEK,
            range = range,
            transactions = listOf(
                transaction(
                    id = 1L,
                    type = RecordType.INCOME,
                    amountFen = 500_00L,
                    categoryName = "工资",
                    transactionTime = monday,
                ),
                transaction(
                    id = 2L,
                    type = RecordType.EXPENSE,
                    amountFen = 120_00L,
                    categoryName = "餐饮",
                    transactionTime = tuesday,
                ),
                transaction(
                    id = 3L,
                    type = RecordType.EXPENSE,
                    amountFen = 80_00L,
                    categoryName = "交通",
                    transactionTime = thursday,
                ),
            ),
            zoneId = zoneId,
        )

        assertEquals(500_00L, snapshot.summary.incomeFen)
        assertEquals(200_00L, snapshot.summary.expenseFen)
        assertEquals(300_00L, snapshot.summary.surplusFen)
        assertEquals(2, snapshot.categoryBreakdown.size)
        assertEquals(7, snapshot.trendBreakdown.size)
        assertTrue(snapshot.hasTransactions)
    }

    private fun transaction(
        id: Long,
        type: RecordType,
        amountFen: Long,
        categoryName: String,
        transactionTime: Long,
    ) = TransactionEntity(
        id = id,
        userId = 1L,
        type = type,
        amountFen = amountFen,
        accountId = null,
        accountName = null,
        categoryId = 10L,
        categoryName = categoryName,
        remark = null,
        merchantName = null,
        transactionTime = transactionTime,
        source = TransactionSource.MANUAL,
        createdAt = transactionTime,
        updatedAt = transactionTime,
        syncStatus = SyncStatus.SYNCED,
    )

    private fun localDate(isoDate: String): Long =
        LocalDate.parse(isoDate).atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()
}

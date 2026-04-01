package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.TransactionSource
import com.example.ocrjizhang.data.model.StatisticsPeriod
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsCalculatorTest {

    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `rangeFor week starts on monday and ends on sunday`() {
        val now = localDate("2024-03-31")

        val range = StatisticsCalculator.rangeFor(
            period = StatisticsPeriod.WEEK,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals("03.25 - 03.31", range.label)
    }

    @Test
    fun `buildSnapshot aggregates summary category and week trend`() {
        val now = localDate("2024-03-31")
        val monday = localDate("2024-03-25")
        val tuesday = localDate("2024-03-26")
        val thursday = localDate("2024-03-28")

        val snapshot = StatisticsCalculator.buildSnapshot(
            period = StatisticsPeriod.WEEK,
            transactions = listOf(
                transaction(id = 1L, type = RecordType.INCOME, amountFen = 500_00L, categoryName = "salary", transactionTime = monday),
                transaction(id = 2L, type = RecordType.EXPENSE, amountFen = 120_00L, categoryName = "food", transactionTime = tuesday),
                transaction(id = 3L, type = RecordType.EXPENSE, amountFen = 80_00L, categoryName = "transport", transactionTime = thursday),
            ),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(500_00L, snapshot.summary.incomeFen)
        assertEquals(200_00L, snapshot.summary.expenseFen)
        assertEquals(300_00L, snapshot.summary.surplusFen)
        assertEquals(2, snapshot.categoryBreakdown.size)
        assertEquals("food", snapshot.categoryBreakdown.first().categoryName)
        assertEquals(7, snapshot.trendBreakdown.size)
        assertEquals(500_00L, snapshot.trendBreakdown.first().incomeFen)
        assertTrue(snapshot.hasTransactions)
    }

    @Test
    fun `buildSnapshot creates monthly buckets`() {
        val now = localDate("2024-03-31")

        val snapshot = StatisticsCalculator.buildSnapshot(
            period = StatisticsPeriod.MONTH,
            transactions = emptyList(),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(5, snapshot.trendBreakdown.size)
        assertEquals("\u7b2c1\u5468", snapshot.trendBreakdown.first().label)
    }

    @Test
    fun `buildSnapshot excludes income transactions from category breakdown`() {
        val now = localDate("2024-03-31")
        val monday = localDate("2024-03-25")
        val tuesday = localDate("2024-03-26")

        val snapshot = StatisticsCalculator.buildSnapshot(
            period = StatisticsPeriod.WEEK,
            transactions = listOf(
                transaction(id = 1L, type = RecordType.INCOME, amountFen = 300_00L, categoryName = "salary", transactionTime = monday),
                transaction(id = 2L, type = RecordType.EXPENSE, amountFen = 66_00L, categoryName = "food", transactionTime = tuesday),
            ),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(1, snapshot.categoryBreakdown.size)
        assertEquals("food", snapshot.categoryBreakdown.single().categoryName)
        assertEquals(1f, snapshot.categoryBreakdown.single().share)
    }

    @Test
    fun `buildSnapshot day period creates a single today bucket`() {
        val now = localDate("2024-03-31")

        val snapshot = StatisticsCalculator.buildSnapshot(
            period = StatisticsPeriod.DAY,
            transactions = listOf(
                transaction(id = 1L, type = RecordType.EXPENSE, amountFen = 88_00L, categoryName = "coffee", transactionTime = now),
            ),
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(1, snapshot.trendBreakdown.size)
        assertEquals("今天", snapshot.trendBreakdown.single().label)
        assertEquals(88_00L, snapshot.trendBreakdown.single().expenseFen)
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
        LocalDate.parse(isoDate)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
}

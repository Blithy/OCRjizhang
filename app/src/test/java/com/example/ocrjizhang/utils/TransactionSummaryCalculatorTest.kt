package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.local.entity.TransactionSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSummaryCalculatorTest {

    @Test
    fun `calculateMonthlySummary only counts current month transactions`() {
        val now = 1_711_929_600_000L
        val startOfMonth = AccountingFormatters.startOfCurrentMonth(now)
        val lastMonth = startOfMonth - 86_400_000L

        val transactions = listOf(
            transaction(id = 1L, type = RecordType.INCOME, amountFen = 200_000L, transactionTime = startOfMonth),
            transaction(id = 2L, type = RecordType.EXPENSE, amountFen = 50_000L, transactionTime = now),
            transaction(id = 3L, type = RecordType.EXPENSE, amountFen = 99_999L, transactionTime = lastMonth),
        )

        val summary = TransactionSummaryCalculator.calculateMonthlySummary(
            transactions = transactions,
            nowMillis = now,
        )

        assertEquals(200_000L, summary.incomeFen)
        assertEquals(50_000L, summary.expenseFen)
        assertEquals(150_000L, summary.surplusFen)
    }

    private fun transaction(
        id: Long,
        type: RecordType,
        amountFen: Long,
        transactionTime: Long,
    ) = TransactionEntity(
        id = id,
        userId = 1L,
        type = type,
        amountFen = amountFen,
        accountId = null,
        accountName = null,
        categoryId = 10L,
        categoryName = "测试分类",
        remark = null,
        merchantName = null,
        transactionTime = transactionTime,
        source = TransactionSource.MANUAL,
        createdAt = transactionTime,
        updatedAt = transactionTime,
        syncStatus = SyncStatus.SYNCED,
    )
}

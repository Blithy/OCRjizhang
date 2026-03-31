package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.TransactionEntity

data class MonthlySummary(
    val incomeFen: Long,
    val expenseFen: Long,
) {
    val surplusFen: Long get() = incomeFen - expenseFen
}

object TransactionSummaryCalculator {
    fun calculateMonthlySummary(
        transactions: List<TransactionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
    ): MonthlySummary {
        val start = AccountingFormatters.startOfCurrentMonth(nowMillis)
        val end = AccountingFormatters.endOfCurrentMonth(nowMillis)
        val currentMonthTransactions = transactions.filter {
            it.transactionTime in start..end
        }
        val incomeFen = currentMonthTransactions
            .filter { it.type == RecordType.INCOME }
            .sumOf { it.amountFen }
        val expenseFen = currentMonthTransactions
            .filter { it.type == RecordType.EXPENSE }
            .sumOf { it.amountFen }
        return MonthlySummary(
            incomeFen = incomeFen,
            expenseFen = expenseFen,
        )
    }
}

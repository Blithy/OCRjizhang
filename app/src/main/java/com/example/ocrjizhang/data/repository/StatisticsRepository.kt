package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsSnapshot
import com.example.ocrjizhang.utils.StatisticsCalculator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StatisticsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    fun observeStatistics(
        userId: Long,
        period: StatisticsPeriod,
        nowMillis: Long = System.currentTimeMillis(),
    ): Flow<StatisticsSnapshot> {
        val range = StatisticsCalculator.rangeFor(period, nowMillis)
        return transactionDao.observeTransactionsBetween(
            userId = userId,
            startTime = range.startMillis,
            endTime = range.endMillis,
        ).map { transactions ->
            StatisticsCalculator.buildSnapshot(
                period = period,
                transactions = transactions,
                nowMillis = nowMillis,
            )
        }
    }
}

package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.dao.TransactionDao
import com.example.ocrjizhang.data.model.StatisticsPeriod
import com.example.ocrjizhang.data.model.StatisticsRange
import com.example.ocrjizhang.data.model.StatisticsSnapshot
import com.example.ocrjizhang.utils.StatisticsCalculator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 统计仓库。
 * 负责按时间范围读取交易数据，并交给统计计算器生成图表需要的聚合结果。
 */
@Singleton
class StatisticsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    fun observeStatistics(
        userId: Long,
        period: StatisticsPeriod,
        range: StatisticsRange,
    ): Flow<StatisticsSnapshot> {
        return transactionDao.observeTransactionsBetween(
            userId = userId,
            startTime = range.startMillis,
            endTime = range.endMillis,
        ).map { transactions ->
            StatisticsCalculator.buildSnapshot(
                period = period,
                range = range,
                transactions = transactions,
            )
        }
    }
}

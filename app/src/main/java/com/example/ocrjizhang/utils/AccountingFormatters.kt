package com.example.ocrjizhang.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

object AccountingFormatters {
    private val amountFormatSymbols = DecimalFormatSymbols(Locale.CHINA).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }

    private val amountFormatter = DecimalFormat("#,##0.00", amountFormatSymbols)

    fun formatFen(amountFen: Long): String {
        val amountYuan = amountFen.absoluteValue / 100.0
        val prefix = if (amountFen < 0) "-" else ""
        return prefix + "￥" + amountFormatter.format(amountYuan)
    }

    fun formatFenForInput(amountFen: Long): String =
        amountFormatter.format(amountFen / 100.0)

    fun parseToFen(rawInput: String): Long? {
        val normalized = rawInput.trim()
            .replace("￥", "")
            .replace(",", "")
        if (normalized.isBlank()) return null
        val amount = normalized.toBigDecimalOrNull() ?: return null
        if (amount <= java.math.BigDecimal.ZERO) return null
        return try {
            amount.movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(epochMillis))

    fun formatDateTime(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochMillis))

    fun startOfCurrentMonth(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        return zonedDateTime
            .withDayOfMonth(1)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfCurrentMonth(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val zonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val firstMomentOfNextMonth = zonedDateTime
            .withDayOfMonth(1)
            .plusMonths(1)
            .toLocalDate()
            .atStartOfDay(zoneId)
        return firstMomentOfNextMonth.toInstant().toEpochMilli() - 1
    }
}

package com.example.ocrjizhang.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ParsedReceiptData(
    val amountText: String? = null,
    val amountFen: Long? = null,
    val dateText: String? = null,
    val dateMillis: Long? = null,
    val merchantName: String? = null,
)

object OcrReceiptParser {

    private val amountKeywords = listOf(
        "合计",
        "应付",
        "实付",
        "实收",
        "金额",
        "消费",
        "支付",
        "total",
        "amt",
    )

    private val merchantStopWords = listOf(
        "欢迎",
        "谢谢",
        "合计",
        "应付",
        "实付",
        "实收",
        "金额",
        "日期",
        "时间",
        "收银",
        "交易",
        "单号",
        "订单",
        "票据",
        "发票",
        "支付宝",
        "微信",
        "银行",
        "电话",
        "tel",
    )

    private val amountRegex = Regex("""(?<!\d)(\d{1,7}(?:[.,]\d{1,2})?)(?!\d)""")
    private val dateRegex = Regex(
        """(20\d{2}[./-年]\d{1,2}[./-月]\d{1,2}(?:日)?(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?)""",
    )

    fun parse(rawText: String): ParsedReceiptData {
        val lines = rawText.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()

        val amount = extractBestAmount(lines)
        val date = extractBestDate(lines)
        val merchant = extractMerchant(lines)

        return ParsedReceiptData(
            amountText = amount?.normalizedText,
            amountFen = amount?.amountFen,
            dateText = date?.first,
            dateMillis = date?.second,
            merchantName = merchant,
        )
    }

    fun parseDateToMillis(rawDate: String?): Long? {
        val normalized = rawDate
            ?.trim()
            ?.replace("年", "-")
            ?.replace("月", "-")
            ?.replace("日", "")
            ?.replace("/", "-")
            ?.replace(".", "-")
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val zoneId = ZoneId.systemDefault()
        val dateTimeFormats = listOf(
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss", Locale.CHINA),
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm", Locale.CHINA),
        )
        dateTimeFormats.forEach { formatter ->
            try {
                return LocalDateTime.parse(normalized, formatter)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Try next formatter.
            }
        }

        val dateFormats = listOf(
            DateTimeFormatter.ofPattern("yyyy-M-d", Locale.CHINA),
        )
        dateFormats.forEach { formatter ->
            try {
                return LocalDate.parse(normalized, formatter)
                    .atTime(12, 0)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
                // Try next formatter.
            }
        }

        return null
    }

    private fun extractBestAmount(lines: List<String>): AmountCandidate? {
        val candidates = buildList {
            lines.forEach { line ->
                val hasKeyword = amountKeywords.any { keyword ->
                    line.contains(keyword, ignoreCase = true)
                }
                val likelyDateLine = line.contains('-') || line.contains('/') || line.contains('年')
                amountRegex.findAll(line).forEach { match ->
                    val normalizedText = match.value.replace(",", "")
                    val amountFen = AccountingFormatters.parseToFen(normalizedText)
                        ?: return@forEach
                    if (likelyDateLine && !hasKeyword && !normalizedText.contains('.')) {
                        return@forEach
                    }
                    add(
                        AmountCandidate(
                            normalizedText = normalizedText,
                            amountFen = amountFen,
                            hasKeyword = hasKeyword,
                            hasDecimal = normalizedText.contains('.'),
                        ),
                    )
                }
            }
        }

        return candidates.maxWithOrNull(
            compareBy<AmountCandidate>({ it.hasKeyword }, { it.hasDecimal }, { it.amountFen }),
        )
    }

    private fun extractBestDate(lines: List<String>): Pair<String, Long>? {
        lines.forEach { line ->
            val candidate = dateRegex.find(line)?.value ?: return@forEach
            val millis = parseDateToMillis(candidate) ?: return@forEach
            return candidate to millis
        }
        return null
    }

    private fun extractMerchant(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            if (merchantStopWords.any { keyword -> line.contains(keyword, ignoreCase = true) }) {
                return@firstOrNull false
            }
            val digitCount = line.count(Char::isDigit)
            val letterLikeCount = line.count { char -> char.isLetter() }
            line.length in 2..24 && digitCount <= 3 && letterLikeCount >= 2
        }
    }

    private data class AmountCandidate(
        val normalizedText: String,
        val amountFen: Long,
        val hasKeyword: Boolean,
        val hasDecimal: Boolean,
    )
}

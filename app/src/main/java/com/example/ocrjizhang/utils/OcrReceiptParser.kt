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
        "总计",
        "小计",
        "应付",
        "实付",
        "实收",
        "支付",
        "金额",
        "消费",
        "total",
        "amount",
        "amt",
    )

    private val amountNegativeKeywords = listOf(
        "找零",
        "优惠",
        "折扣",
        "抹零",
        "减免",
        "退款",
        "返现",
        "coupon",
        "discount",
        "change",
    )

    private val merchantStopWords = listOf(
        "欢迎",
        "谢谢",
        "合计",
        "总计",
        "小计",
        "应付",
        "实付",
        "金额",
        "日期",
        "时间",
        "收银",
        "交易",
        "订单",
        "票据",
        "发票",
        "支付宝",
        "微信",
        "银行",
        "电话",
        "服务热线",
        "tel",
        "url",
        "http",
        "税号",
        "机器号",
        "流水号",
    )

    private val merchantKeywords = listOf(
        "店",
        "超市",
        "便利店",
        "餐厅",
        "饭店",
        "咖啡",
        "药房",
        "生活",
        "百货",
        "商行",
        "有限公司",
        "公司",
    )

    private val dateKeywordRegex = Regex("日期|时间|交易时间|消费时间")
    private val noisyMerchantRegex = Regex("""[0-9A-Za-z]{8,}|^\d+$""")
    private val amountRegex = Regex("""(?<!\d)(\d{1,7}(?:[.,]\d{1,2})?)(?!\d)""")
    private val dateRegex = Regex(
        """(20\d{2}\s*[./年-]\s*\d{1,2}\s*[./月-]\s*\d{1,2}\s*(?:日)?(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?)""",
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
            lines.forEachIndexed { index, line ->
                val lowerCaseLine = line.lowercase(Locale.ROOT)
                val hasKeyword = amountKeywords.any { keyword ->
                    line.contains(keyword, ignoreCase = true)
                }
                val hasDateKeyword = dateKeywordRegex.containsMatchIn(line)
                val hasNegativeKeyword = amountNegativeKeywords.any { keyword ->
                    line.contains(keyword, ignoreCase = true)
                }
                val likelyDateLine = dateRegex.containsMatchIn(line)
                val dateSeparatorCount = line.count { char ->
                    char == '-' || char == '/' || char == '.' || char == '年' || char == '月' || char == ':'
                }
                val digitCount = line.count(Char::isDigit)
                val looksLikeDateLine = likelyDateLine ||
                    hasDateKeyword ||
                    (digitCount >= 6 && dateSeparatorCount >= 2)
                amountRegex.findAll(line).forEach { match ->
                    val normalizedText = match.value.replace(",", "")
                    val amountFen = AccountingFormatters.parseToFen(normalizedText) ?: return@forEach
                    if (amountFen <= 0) {
                        return@forEach
                    }
                    if (looksLikeDateLine && !hasKeyword) {
                        return@forEach
                    }
                    if (normalizedText.length == 4 && normalizedText.startsWith("20") && hasDateKeyword) {
                        return@forEach
                    }

                    var score = amountFen.coerceAtMost(999_999L).toInt()
                    if (hasKeyword) score += 30_000
                    if (normalizedText.contains('.')) score += 10_000
                    if (!hasNegativeKeyword) score += 5_000
                    if (index >= lines.lastIndex - 3) score += 1_500
                    if (line.endsWith(match.value)) score += 1_000
                    if (lowerCaseLine.contains("total")) score += 3_000
                    if (hasNegativeKeyword) score -= 20_000

                    add(
                        AmountCandidate(
                            normalizedText = normalizedText,
                            amountFen = amountFen,
                            score = score,
                        ),
                    )
                }
            }
        }

        return candidates.maxByOrNull(AmountCandidate::score)
    }

    private fun extractBestDate(lines: List<String>): Pair<String, Long>? {
        val candidates = buildList {
            lines.forEachIndexed { index, line ->
                dateRegex.findAll(line).forEach { match ->
                    val value = match.value.trim()
                    val millis = parseDateToMillis(value) ?: return@forEach
                    var score = 100 - index
                    if (dateKeywordRegex.containsMatchIn(line)) {
                        score += 50
                    }
                    if (value.contains(':')) {
                        score += 10
                    }
                    add(DateCandidate(value, millis, score))
                }
            }
        }
        if (candidates.isNotEmpty()) {
            return candidates.maxByOrNull(DateCandidate::score)?.let { it.rawText to it.millis }
        }

        val fullText = lines.joinToString(" ")
        return dateRegex.findAll(fullText)
            .mapNotNull { match ->
                val value = match.value.trim()
                val millis = parseDateToMillis(value) ?: return@mapNotNull null
                DateCandidate(value, millis, 0)
            }
            .maxByOrNull(DateCandidate::score)
            ?.let { it.rawText to it.millis }
    }

    private fun extractMerchant(lines: List<String>): String? {
        val candidates = buildList {
            lines.take(8).forEachIndexed { index, line ->
                val normalizedLine = line
                    .replace(Regex("""^[*#\s]+|[*#\s]+$"""), "")
                    .replace(Regex("""\s{2,}"""), " ")
                if (normalizedLine.length !in 2..32) {
                    return@forEachIndexed
                }
                if (merchantStopWords.any { keyword -> normalizedLine.contains(keyword, ignoreCase = true) }) {
                    return@forEachIndexed
                }
                if (noisyMerchantRegex.containsMatchIn(normalizedLine)) {
                    return@forEachIndexed
                }

                val digitCount = normalizedLine.count(Char::isDigit)
                val chineseOrLetterCount = normalizedLine.count { char ->
                    char.isLetter() || char.code in 0x4E00..0x9FFF
                }
                if (digitCount > 4 || chineseOrLetterCount < 2) {
                    return@forEachIndexed
                }

                var score = 100 - index * 10
                if (merchantKeywords.any { keyword -> normalizedLine.contains(keyword, ignoreCase = true) }) {
                    score += 80
                }
                if (normalizedLine.length in 4..18) {
                    score += 20
                }
                if (digitCount == 0) {
                    score += 10
                }

                add(MerchantCandidate(normalizedLine, score))
            }
        }
        return candidates.maxByOrNull(MerchantCandidate::score)?.name
    }

    private data class AmountCandidate(
        val normalizedText: String,
        val amountFen: Long,
        val score: Int,
    )

    private data class DateCandidate(
        val rawText: String,
        val millis: Long,
        val score: Int,
    )

    private data class MerchantCandidate(
        val name: String,
        val score: Int,
    )
}

package com.example.ocrjizhang.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs
import java.util.Locale

data class ParsedReceiptData(
    val amountText: String? = null,
    val amountFen: Long? = null,
    val dateText: String? = null,
    val dateMillis: Long? = null,
    val merchantName: String? = null,
)

object OcrReceiptParser {

    private val amountRegex = Regex(
        pattern = """(?<!\d)([-+]?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|[-+]?\d{1,7}(?:\.\d{1,2})?)(?!\d)""",
    )

    private val dateRegex = Regex(
        pattern = """20\d{2}\s*[./\-\u5e74]\s*\d{1,2}\s*[./\-\u6708]\s*\d{1,2}(?:\s*\u65e5)?(?:\s*\d{1,2}:\d{2}(?::\d{2})?)?""",
    )

    private val timeOnlyRegex = Regex(
        pattern = """^\d{1,2}:\d{2}(?::\d{2})?$""",
    )

    private val decimalAmountRegex = Regex(
        pattern = """[-+]?\d+\.\d{1,2}""",
    )

    private val amountKeywords = listOf(
        "amount",
        "amt",
        "total",
        "\u5408\u8ba1",
        "\u603b\u8ba1",
        "\u5c0f\u8ba1",
        "\u5e94\u4ed8",
        "\u5b9e\u4ed8",
        "\u5b9e\u6536",
        "\u91d1\u989d",
        "\u4ea4\u6613\u91d1\u989d",
        "\u652f\u4ed8\u91d1\u989d",
        "\u652f\u51fa",
        "\u6536\u5165",
        "\u6263\u6b3e",
    )

    private val amountPenaltyKeywords = listOf(
        "coupon",
        "discount",
        "change",
        "\u4f18\u60e0",
        "\u627e\u96f6",
        "\u6298\u6263",
        "\u51cf\u514d",
        "\u8fd4\u73b0",
    )

    private val dateKeywords = listOf(
        "date",
        "time",
        "\u65e5\u671f",
        "\u65f6\u95f4",
        "\u4ea4\u6613\u65f6\u95f4",
    )

    private val merchantStopWords = listOf(
        "\u4ea4\u6613\u8be6\u60c5",
        "\u5f53\u524d\u72b6\u6001",
        "\u652f\u4ed8\u6210\u529f",
        "\u5c0f\u7a0b\u5e8f",
        "\u89c6\u9891\u53f7",
        "\u559c\u6b22",
        "\u670d\u52a1",
        "\u4ea4\u6613\u670d\u52a1",
        "\u5546\u54c1",
        "\u6536\u5355\u673a\u6784",
        "\u652f\u4ed8\u65b9\u5f0f",
        "\u4ea4\u6613\u5355\u53f7",
        "\u5546\u6237\u5355\u53f7",
        "\u72b6\u6001",
        "\u65f6\u95f4",
        "\u96f6\u94b1",
        "\u5bf9\u65b9",
        "\u901a\u77e5",
        "\u632f\u52a8",
        "\u7279\u4ef7\u5916\u5356\u56e2\u8d2d",
        "\u7f8e\u597d\u751f\u6d3b\u5e2e\u624b",
        "http",
        "www",
    )

    private val merchantKeywords = listOf(
        "\u516c\u53f8",
        "\u6709\u9650\u516c\u53f8",
        "\u8d85\u5e02",
        "\u996d\u5e97",
        "\u9910\u5385",
        "\u5e97",
        "\u7f8e\u56e2",
        "\u5fae\u4fe1",
        "\u652f\u4ed8\u5b9d",
    )

    private val merchantPlatformNames = setOf(
        "\u7f8e\u56e2",
        "\u997f\u4e86\u4e48",
        "\u5fae\u4fe1",
        "\u652f\u4ed8\u5b9d",
        "\u6296\u97f3",
        "\u4eac\u4e1c",
        "\u6dd8\u5b9d",
        "\u62fc\u591a\u591a",
    )

    fun parse(rawText: String): ParsedReceiptData {
        val lines = rawText.lineSequence()
            .map(::normalizeLine)
            .filter(String::isNotBlank)
            .toList()

        val amount = extractBestAmount(lines)
        val date = extractBestDate(lines)
        val merchant = extractMerchant(lines, amount?.lineIndex)

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
            ?.replace("\u5e74", "-")
            ?.replace("\u6708", "-")
            ?.replace("\u65e5", "")
            ?.replace("/", "-")
            ?.replace(".", "-")
            ?.replace(
                Regex("""^(\d{4}-\d{1,2}-\d{1,2})(\d{1,2}:\d{2}(?::\d{2})?)$"""),
                "$1 $2",
            )
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

        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d", Locale.CHINA))
                .atTime(12, 0)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            return null
        }
    }

    private fun normalizeLine(line: String): String = line
        .replace('\u00a0', ' ')
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun extractBestAmount(lines: List<String>): AmountCandidate? {
        val candidates = buildList {
            lines.forEachIndexed { index, line ->
                val lowerCaseLine = line.lowercase(Locale.ROOT)
                val hasKeyword = amountKeywords.any { keyword ->
                    lowerCaseLine.contains(keyword.lowercase(Locale.ROOT))
                }
                val hasPenaltyKeyword = amountPenaltyKeywords.any { keyword ->
                    lowerCaseLine.contains(keyword.lowercase(Locale.ROOT))
                }
                val hasDateKeyword = dateKeywords.any { keyword ->
                    lowerCaseLine.contains(keyword.lowercase(Locale.ROOT))
                }
                val looksLikeDateLine = hasDateKeyword || dateRegex.containsMatchIn(line)
                val isTimeOnlyLine = timeOnlyRegex.matches(line)
                val digitCount = line.count(Char::isDigit)
                val isStandaloneNumericLine = line.trim()
                    .matches(Regex("""^[-+]?\d+(?:\.\d{1,2})?$"""))

                if (isTimeOnlyLine && !hasKeyword) {
                    return@forEachIndexed
                }

                amountRegex.findAll(line).forEach { match ->
                    val rawAmount = match.value
                    val normalizedText = rawAmount
                        .removePrefix("+")
                        .removePrefix("-")
                        .replace(",", "")

                    val amountFen = AccountingFormatters.parseToFen(normalizedText) ?: return@forEach
                    if (normalizedText.length == 4 && normalizedText.startsWith("20") && !normalizedText.contains('.')) {
                        return@forEach
                    }
                    if (looksLikeDateLine && !hasKeyword && !rawAmount.contains('.')) {
                        return@forEach
                    }
                    if (digitCount >= 10 && !rawAmount.contains('.') && !hasKeyword) {
                        return@forEach
                    }

                    var score = 0
                    if (hasKeyword) score += 120
                    if (rawAmount.startsWith("-")) score += 60
                    if (normalizedText.contains('.')) score += 80
                    if (isStandaloneNumericLine) score += 60
                    if (line.length <= 12) score += 30
                    if (index in 0..8) score += 20
                    if (!hasPenaltyKeyword) score += 15
                    if (hasPenaltyKeyword) score -= 80
                    if (hasDateKeyword && !hasKeyword) score -= 50
                    if (line.contains(':') && !rawAmount.contains('.') && !hasKeyword) score -= 60
                    if (!hasKeyword && !rawAmount.contains('.') && !rawAmount.startsWith("-")) score -= 55
                    if (!hasKeyword && normalizedText.length >= 4 && !rawAmount.contains('.')) score -= 35
                    score += amountFen.coerceAtMost(99_999L).toInt() / 1_000

                    add(
                        AmountCandidate(
                            normalizedText = normalizedText,
                            amountFen = amountFen,
                            lineIndex = index,
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
                    val value = normalizeDateText(match.value)
                    val millis = parseDateToMillis(value) ?: return@forEach
                    var score = 100 - index
                    if (dateKeywords.any { keyword ->
                            line.lowercase(Locale.ROOT).contains(keyword.lowercase(Locale.ROOT))
                        }
                    ) {
                        score += 30
                    }
                    if (value.contains(':')) {
                        score += 10
                    }
                    add(DateCandidate(value, millis, score))
                }
            }
        }

        return candidates.maxByOrNull(DateCandidate::score)?.let { it.rawText to it.millis }
    }

    private fun extractMerchant(lines: List<String>, amountLineIndex: Int?): String? {
        val candidates = buildList {
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine
                    .trimStart('`', '.', '\u00b7', '\u3002', '-', '_', ':')
                    .trim()
                val normalizedMerchant = normalizeMerchantName(line)
                val shouldInspect = index < 18 || amountLineIndex?.let { abs(index - it) <= 3 } == true
                if (!shouldInspect) {
                    return@forEachIndexed
                }
                if (normalizedMerchant.length !in 2..32) {
                    return@forEachIndexed
                }
                if (merchantStopWords.any { stopWord ->
                        line.contains(stopWord, ignoreCase = true)
                    }
                ) {
                    return@forEachIndexed
                }
                if (decimalAmountRegex.containsMatchIn(line) || timeOnlyRegex.matches(line) || dateRegex.containsMatchIn(line)) {
                    return@forEachIndexed
                }
                if (merchantPlatformNames.contains(normalizedMerchant)) {
                    return@forEachIndexed
                }

                val digitCount = normalizedMerchant.count(Char::isDigit)
                val letterOrChineseCount = normalizedMerchant.count { character ->
                    character.isLetter() || character.code in 0x4E00..0x9FFF
                }
                if (digitCount > 6 || letterOrChineseCount < 2) {
                    return@forEachIndexed
                }

                var score = 100 - index * 6
                if (merchantKeywords.any { keyword -> normalizedMerchant.contains(keyword, ignoreCase = true) }) {
                    score += 40
                }
                if (digitCount == 0) {
                    score += 20
                }
                if (normalizedMerchant.length in 2..10) {
                    score += 20
                }
                if (normalizedMerchant.contains('(') || normalizedMerchant.contains('\uff08')) {
                    score += 35
                }
                if (normalizedMerchant.length >= 8) {
                    score += 15
                }
                if (amountLineIndex != null && index < amountLineIndex) {
                    score += 10
                }
                if (amountLineIndex != null && abs(index - amountLineIndex) <= 2) {
                    score += 45
                }
                if (amountLineIndex != null && abs(index - amountLineIndex) == 1) {
                    score += 30
                }

                add(MerchantCandidate(normalizedMerchant, score))
            }
        }

        return candidates.maxByOrNull(MerchantCandidate::score)?.name
    }

    private fun normalizeDateText(rawDate: String): String = rawDate
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun normalizeMerchantName(rawMerchant: String): String = rawMerchant
        .replace(
            Regex("""[-\s]*(美团|饿了么|微信|支付宝|京东|淘宝|拼多多)\s*(App|app|小程序)?[-\s]*$"""),
            "",
        )
        .trim()
        .trim('-', '_', '.', '\u00b7', '\u3002', ':')
        .trim()

    private data class AmountCandidate(
        val normalizedText: String,
        val amountFen: Long,
        val lineIndex: Int,
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

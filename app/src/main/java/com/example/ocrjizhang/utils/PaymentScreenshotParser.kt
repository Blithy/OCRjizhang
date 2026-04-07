package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.ocr.OcrLine
import kotlin.math.abs

object PaymentScreenshotParser {

    private val amountValueRegex = Regex(
        pattern = """[-+]?(?:[¥￥]\s*)?\d{1,3}(?:,\d{3})*(?:\.\d{1,2})?|[-+]?(?:[¥￥]\s*)?\d{1,7}(?:\.\d{1,2})?""",
    )

    private val dateValueRegex = Regex(
        pattern = """20\d{2}\s*[./\-\u5e74]\s*\d{1,2}\s*[./\-\u6708]\s*\d{1,2}(?:\s*\u65e5)?(?:\s*\d{1,2}:\d{2}(?::\d{2})?)?""",
    )

    private val platformNameSet = setOf(
        "\u7f8e\u56e2",
        "\u997f\u4e86\u4e48",
        "\u5fae\u4fe1",
        "\u652f\u4ed8\u5b9d",
        "\u4eac\u4e1c",
        "\u6dd8\u5b9d",
        "\u62fc\u591a\u591a",
    )

    private val promoKeywords = listOf(
        "\u5916\u5356",
        "\u56e2\u8d2d",
        "\u7279\u4ef7",
        "\u559c\u6b22",
        "\u597d\u8bc4",
        "\u70ed\u5356",
        "\u7206\u6b3e",
        "\u5e2e\u624b",
        "\u89c6\u9891\u53f7",
        "\u5c0f\u7a0b\u5e8f",
    )

    private val knownFieldLabels = listOf(
        "\u5f53\u524d\u72b6\u6001",
        "\u652f\u4ed8\u65f6\u95f4",
        "\u4ea4\u6613\u65f6\u95f4",
        "\u5546\u54c1",
        "\u6536\u5355\u673a\u6784",
        "\u652f\u4ed8\u65b9\u5f0f",
        "\u4ea4\u6613\u5355\u53f7",
        "\u5546\u6237\u5355\u53f7",
        "\u65e5\u671f",
        "\u65f6\u95f4",
    )

    private val summaryAmountLabels = listOf(
        "\u5408\u8ba1",
        "\u603b\u8ba1",
        "\u5b9e\u4ed8",
        "\u5e94\u4ed8",
        "\u4ed8\u6b3e\u91d1\u989d",
        "\u652f\u4ed8\u91d1\u989d",
        "\u8ba2\u5355\u91d1\u989d",
    )

    fun parse(
        lines: List<OcrLine>,
        fallback: ParsedReceiptData,
    ): ParsedReceiptData? {
        val layoutLines = lines
            .map(::toLayoutLine)
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy<LayoutLine> { it.top }.thenBy { it.left })

        if (!looksLikePaymentDetail(layoutLines)) {
            return null
        }

        val dateText = extractDateText(layoutLines) ?: fallback.dateText
        val merchantName = extractMerchantName(layoutLines) ?: fallback.merchantName
        val amountCandidate = extractAmount(layoutLines)

        if (dateText == null && merchantName == null && amountCandidate == null) {
            return null
        }

        return ParsedReceiptData(
            amountText = amountCandidate?.normalizedText ?: fallback.amountText,
            amountFen = amountCandidate?.amountFen ?: fallback.amountFen,
            dateText = dateText,
            dateMillis = OcrReceiptParser.parseDateToMillis(dateText) ?: fallback.dateMillis,
            merchantName = merchantName,
        )
    }

    private fun looksLikePaymentDetail(lines: List<LayoutLine>): Boolean {
        val strongSignalCount = buildList {
            if (lines.any(::isPaymentTimeLabel)) add("time")
            if (lines.any { it.compactText == "\u4ea4\u6613\u5355\u53f7" }) add("transaction_id")
            if (lines.any { it.compactText == "\u5546\u6237\u5355\u53f7" }) add("merchant_order_id")
            if (lines.any { it.compactText == "\u6536\u5355\u673a\u6784" }) add("acquirer")
        }.size

        val assistSignalCount = buildList {
            if (lines.any(::isProductLabel)) add("product")
            if (lines.any { it.compactText == "\u652f\u4ed8\u65b9\u5f0f" }) add("payment_method")
        }.size

        return strongSignalCount >= 1 && strongSignalCount + assistSignalCount >= 3
    }

    private fun extractDateText(lines: List<LayoutLine>): String? {
        val label = lines
            .filter(::isPaymentTimeLabel)
            .maxByOrNull { line -> line.text.length * 10 - line.top / 10 }
            ?: return null

        val sameRowValue = selectBestValue(
            label = label,
            candidates = lines,
            valueFilter = { line ->
                dateValueRegex.containsMatchIn(line.text) ||
                    OcrReceiptParser.parseDateToMillis(line.text) != null
            },
        )
        if (sameRowValue != null) {
            return normalizeDateValue(sameRowValue.text)
        }

        return lines
            .filter { line ->
                line.top > label.top &&
                    line.left >= label.left &&
                    line.top - label.bottom <= 140 &&
                    (dateValueRegex.containsMatchIn(line.text) ||
                        OcrReceiptParser.parseDateToMillis(line.text) != null)
            }
            .minByOrNull { line -> line.top - label.bottom }
            ?.text
            ?.let(::normalizeDateValue)
    }

    private fun extractMerchantName(lines: List<LayoutLine>): String? {
        val label = lines
            .filter(::isProductLabel)
            .maxByOrNull { line -> line.text.length * 10 - line.top / 10 }
            ?: return null

        val value = selectBestValue(
            label = label,
            candidates = lines,
            valueFilter = ::isMerchantValueCandidate,
        ) ?: lines
            .filter { line ->
                line.top > label.top &&
                    line.left >= label.left &&
                    line.top - label.bottom <= 180 &&
                    isMerchantValueCandidate(line)
            }
            .maxByOrNull(::scoreMerchantLine)

        return value?.text?.let(::cleanMerchantName)?.takeIf { it.isNotBlank() }
    }

    private fun extractAmount(lines: List<LayoutLine>): AmountCandidate? {
        val label = lines
            .filter(::isSummaryAmountLabel)
            .maxByOrNull(::scoreSummaryLabel)
            ?: return null

        val bestCandidate = lines
            .asSequence()
            .filter { candidateLine ->
                candidateLine !== label &&
                    candidateLine.centerY >= label.centerY - maxOf(label.height, 28) &&
                    candidateLine.top <= label.bottom + 120 &&
                    candidateLine.right >= label.right - 24
            }
            .mapNotNull { candidateLine ->
                extractAmountCandidate(candidateLine.text)?.let { amount ->
                    CandidateWithAmount(candidate = candidateLine, amount = amount)
                }
            }
            .maxByOrNull { candidateWithAmount ->
                val line = candidateWithAmount.candidate
                var score = 0
                score += 180 - abs(line.centerY - label.centerY)
                score += (line.left - label.left).coerceAtLeast(0) / 3
                if (line.left >= label.right - 24) score += 120
                if (line.top >= label.top) score += 20
                if (abs(line.centerY - label.centerY) <= maxOf(label.height, 28)) score += 120
                if (line.text.contains('¥') || line.text.contains('\uffe5')) score += 35
                if (line.text.contains("\u539f\u4ef7")) score -= 100
                if (line.text.contains("\u7acb\u51cf")) score -= 70
                if (line.text.contains("\u4f18\u60e0") && candidateWithAmount.amount.amountIndex == 0) score -= 80
                score + candidateWithAmount.amount.score
            }

        return bestCandidate?.amount
    }

    private fun selectBestValue(
        label: LayoutLine,
        candidates: List<LayoutLine>,
        valueFilter: (LayoutLine) -> Boolean,
    ): LayoutLine? {
        return candidates
            .asSequence()
            .filter { candidate ->
                candidate !== label &&
                    candidate.left >= label.right - 24 &&
                    abs(candidate.centerY - label.centerY) <= maxOf(label.height, candidate.height) * 2
            }
            .filter(valueFilter)
            .maxByOrNull { candidate ->
                val verticalScore = 120 - abs(candidate.centerY - label.centerY)
                val horizontalScore = 80 - ((candidate.left - label.right).coerceAtLeast(0) / 6)
                verticalScore + horizontalScore + scoreMerchantLine(candidate)
            }
    }

    private fun isPaymentTimeLabel(line: LayoutLine): Boolean {
        val compact = line.compactText
        return compact == "\u652f\u4ed8\u65f6\u95f4" ||
            compact == "\u4ea4\u6613\u65f6\u95f4" ||
            compact.startsWith("\u652f\u4ed8\u65f6") ||
            compact.startsWith("\u4ea4\u6613\u65f6") ||
            compact == "\u65e5\u671f"
    }

    private fun isProductLabel(line: LayoutLine): Boolean {
        val compact = line.compactText
        return compact == "\u5546\u54c1" ||
            compact.startsWith("\u5546\u54c1") ||
            compact == "\u5546\u5bb6" ||
            compact == "\u5546\u6237\u540d\u79f0"
    }

    private fun isSummaryAmountLabel(line: LayoutLine): Boolean {
        val compact = line.compactText
        return summaryAmountLabels.any { label ->
            compact == label || compact.startsWith(label)
        }
    }

    private fun isMerchantValueCandidate(line: LayoutLine): Boolean {
        if (line.compactText.isBlank()) {
            return false
        }
        if (isKnownLabel(line)) {
            return false
        }
        if (dateValueRegex.containsMatchIn(line.text)) {
            return false
        }
        if (line.compactText.all(Char::isDigit)) {
            return false
        }
        if (platformNameSet.contains(line.compactText)) {
            return false
        }
        if (promoKeywords.any { keyword -> line.compactText.contains(keyword) }) {
            return false
        }

        val digitCount = line.text.count(Char::isDigit)
        val chineseOrLetterCount = line.text.count { character ->
            character.isLetter() || character.code in 0x4E00..0x9FFF
        }
        if (digitCount > 10) {
            return false
        }
        return chineseOrLetterCount >= 2
    }

    private fun scoreMerchantLine(line: LayoutLine): Int {
        var score = 0
        if (line.text.contains('(') || line.text.contains('\uff08')) score += 60
        if (line.text.contains("\u5e97")) score += 40
        if (line.text.contains("\u9910")) score += 30
        if (line.text.contains("\u8336")) score += 20
        if (line.text.length in 4..24) score += 30
        if (line.left >= 180) score += 25
        if (line.top >= 260) score += 20
        if (line.text.contains("\u516c\u53f8")) score -= 40
        return score
    }

    private fun scoreSummaryLabel(line: LayoutLine): Int {
        var score = line.top
        if (line.compactText == "\u5408\u8ba1" || line.compactText.startsWith("\u5408\u8ba1")) score += 240
        if (line.compactText == "\u5b9e\u4ed8" || line.compactText.startsWith("\u5b9e\u4ed8")) score += 180
        if (line.compactText == "\u5e94\u4ed8" || line.compactText.startsWith("\u5e94\u4ed8")) score += 140
        return score
    }

    private fun extractAmountCandidate(text: String): AmountCandidate? {
        return amountValueRegex.findAll(text)
            .mapNotNull { match ->
                val rawValue = match.value.trim()
                val normalizedText = rawValue
                    .replace("¥", "")
                    .replace("\uffe5", "")
                    .replace(",", "")
                    .removePrefix("+")
                    .removePrefix("-")
                    .trim()
                val amountFen = AccountingFormatters.parseToFen(normalizedText) ?: return@mapNotNull null
                if (amountFen <= 0L) {
                    return@mapNotNull null
                }

                val start = match.range.first
                val end = match.range.last + 1
                val prefix = text.substring((start - 6).coerceAtLeast(0), start)
                val suffix = text.substring(end, (end + 6).coerceAtMost(text.length))
                val isLastAmountInLine = amountValueRegex.findAll(text).lastOrNull()?.range == match.range

                var score = 0
                if (normalizedText.contains('.')) score += 100
                if (rawValue.contains('¥') || rawValue.contains('\uffe5')) score += 70
                if (start >= text.length / 2) score += 55
                if (end >= text.length - 2) score += 45
                if (isLastAmountInLine) score += 80
                if (prefix.contains("\u539f\u4ef7")) score -= 110
                if (prefix.contains("\u4f18\u60e0") && !isLastAmountInLine) score -= 90
                if (prefix.contains("\u7acb\u51cf")) score -= 90
                if (suffix.contains("\u4f18\u60e0")) score -= 50
                if (rawValue.startsWith("-")) score -= 160

                AmountCandidate(
                    normalizedText = normalizedText,
                    amountFen = amountFen,
                    score = score,
                    sourceText = text.trim(),
                    amountIndex = amountValueRegex.findAll(text).indexOfFirst { it.range == match.range },
                )
            }
            .maxByOrNull(AmountCandidate::score)
    }

    private fun isKnownLabel(line: LayoutLine): Boolean {
        return knownFieldLabels.any { label ->
            line.compactText == label || line.compactText.startsWith(label)
        } || isPaymentTimeLabel(line) || isProductLabel(line)
    }

    private fun normalizeDateValue(rawText: String): String {
        return rawText
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cleanMerchantName(rawText: String): String {
        return rawText
            .replace(
                Regex("""[-\s]*(\u7f8e\u56e2|\u997f\u4e86\u4e48|\u5fae\u4fe1|\u652f\u4ed8\u5b9d|\u4eac\u4e1c|\u6dd8\u5b9d|\u62fc\u591a\u591a)\s*(App|app|\u5c0f\u7a0b\u5e8f)?[-\s]*$"""),
                "",
            )
            .trim()
            .trim('-', '_', '.', '\u00b7', '\u3002', ':')
            .trim()
    }

    private fun toLayoutLine(line: OcrLine): LayoutLine {
        return LayoutLine(
            text = line.text.trim(),
            compactText = compactText(line.text),
            left = line.left,
            top = line.top,
            right = line.right,
            bottom = line.bottom,
        )
    }

    private fun compactText(text: String): String {
        return text
            .replace(Regex("""[^\p{L}\p{Nd}\u4E00-\u9FFF]+"""), "")
            .trim()
    }

    private data class LayoutLine(
        val text: String,
        val compactText: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val centerY: Int
            get() = (top + bottom) / 2

        val height: Int
            get() = (bottom - top).coerceAtLeast(1)
    }

    private data class AmountCandidate(
        val normalizedText: String,
        val amountFen: Long,
        val score: Int,
        val sourceText: String,
        val amountIndex: Int,
    )

    private data class CandidateWithAmount(
        val candidate: LayoutLine,
        val amount: AmountCandidate,
    )
}

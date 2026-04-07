package com.example.ocrjizhang.utils

import com.example.ocrjizhang.data.ocr.OcrLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentScreenshotParserTest {

    @Test
    fun `parse prefers payment field values from structured screenshot lines`() {
        val lines = listOf(
            OcrLine(text = "15:35", left = 48, top = 56, right = 116, bottom = 84),
            OcrLine(text = "\u7f8e\u56e2", left = 90, top = 118, right = 164, bottom = 146),
            OcrLine(text = "\u7279\u4ef7\u5916\u5356\u56e2\u8d2d", left = 82, top = 154, right = 262, bottom = 182),
            OcrLine(text = "\u652f\u4ed8\u6210\u529f", left = 270, top = 334, right = 372, bottom = 362),
            OcrLine(text = "-13.40", left = 258, top = 278, right = 378, bottom = 324),
            OcrLine(text = "\u652f\u4ed8\u65f6\u95fb", left = 52, top = 428, right = 146, bottom = 456),
            OcrLine(text = "2026\u5e7404\u670801\u65e514:26:15", left = 226, top = 426, right = 492, bottom = 458),
            OcrLine(text = "\u5546\u54c1", left = 52, top = 472, right = 92, bottom = 500),
            OcrLine(text = "\u9e7f\u89d2\u5df7(\u4e07\u8c61\u5b9c\u5bbe\u5929\u5730\u5e97)-\u7f8e\u56e2App-", left = 224, top = 470, right = 720, bottom = 504),
            OcrLine(text = "\u6536\u5355\u673a\u6784", left = 52, top = 514, right = 128, bottom = 542),
            OcrLine(text = "\u5317\u4eac\u94b1\u888b\u5b9d\u652f\u4ed8\u6280\u672f\u6709\u9650\u516c\u53f8", left = 224, top = 512, right = 588, bottom = 546),
        )

        val fallback = ParsedReceiptData(
            amountText = "13.40",
            amountFen = 1_340L,
            dateText = "2026\u5e7404\u670801\u65e514:26:15",
            dateMillis = OcrReceiptParser.parseDateToMillis("2026\u5e7404\u670801\u65e514:26:15"),
            merchantName = "\u7279\u4ef7\u5916\u5356\u56e2\u8d2d",
        )

        val result = PaymentScreenshotParser.parse(lines = lines, fallback = fallback)

        assertNotNull(result)
        assertEquals("13.40", result?.amountText)
        assertEquals(1_340L, result?.amountFen)
        assertEquals("2026\u5e7404\u670801\u65e514:26:15", result?.dateText)
        assertEquals("\u9e7f\u89d2\u5df7(\u4e07\u8c61\u5b9c\u5bbe\u5929\u5730\u5e97)", result?.merchantName)
    }

    @Test
    fun `parse returns null for non payment screenshot lines`() {
        val lines = listOf(
            OcrLine(text = "LAWSON", left = 40, top = 60, right = 180, bottom = 88),
            OcrLine(text = "2026-04-01 18:23:15", left = 40, top = 104, right = 280, bottom = 132),
            OcrLine(text = "Total 18.50", left = 40, top = 148, right = 180, bottom = 176),
        )

        val result = PaymentScreenshotParser.parse(
            lines = lines,
            fallback = ParsedReceiptData(amountText = "18.50", amountFen = 1_850L),
        )

        assertNull(result)
    }

    @Test
    fun `parse returns null for order detail screenshot and lets fallback parser handle totals`() {
        val lines = listOf(
            OcrLine(text = "\u8ba2\u5355\u53f7", left = 32, top = 188, right = 108, bottom = 218),
            OcrLine(text = "3902 0629 1364 3048 466", left = 168, top = 188, right = 488, bottom = 220),
            OcrLine(text = "\u4e0b\u5355\u65f6\u95f4", left = 32, top = 242, right = 124, bottom = 272),
            OcrLine(text = "2026-04-02 13:55:56", left = 164, top = 242, right = 414, bottom = 274),
            OcrLine(text = "\u652f\u4ed8\u65b9\u5f0f", left = 32, top = 296, right = 124, bottom = 326),
            OcrLine(text = "\u5728\u7ebf\u652f\u4ed8", left = 164, top = 296, right = 286, bottom = 328),
            OcrLine(text = "\u5546\u54c1\u8d39\u7528", left = 32, top = 438, right = 124, bottom = 470),
            OcrLine(text = "\u67e0\u6c14\u4e91\u5357\u83dc(\u5b9c\u5bbe\u4e07\u8c61\u6c47\u5e97)", left = 32, top = 500, right = 344, bottom = 532),
            OcrLine(text = "\u3010\u9996\u521b\u3011\u7cef\u7c73\u9999\u67e0\u6aac\u8336", left = 120, top = 558, right = 400, bottom = 590),
            OcrLine(text = "\u00a519", left = 876, top = 560, right = 944, bottom = 592),
            OcrLine(text = "\u6253\u5305\u8d39", left = 32, top = 650, right = 116, bottom = 680),
            OcrLine(text = "\u00a51", left = 900, top = 650, right = 944, bottom = 682),
            OcrLine(text = "\u7528\u6237\u914d\u9001\u8d39", left = 32, top = 706, right = 152, bottom = 738),
            OcrLine(text = "\u00a51.7", left = 872, top = 706, right = 944, bottom = 738),
            OcrLine(text = "\u5df2\u4eab\u95e8\u5e97\u65b0\u5ba2\u7acb\u51cf\u7b492\u9879\u4f18\u60e0", left = 32, top = 880, right = 318, bottom = 914),
            OcrLine(text = "-\u00a57", left = 880, top = 880, right = 944, bottom = 914),
            OcrLine(text = "\u5408\u8ba1", left = 32, top = 958, right = 78, bottom = 996),
            OcrLine(text = "\u5df2\u4f18\u60e012\u5143 \u00a514.7", left = 620, top = 958, right = 944, bottom = 996),
        )

        val fallback = ParsedReceiptData(
            amountText = "19",
            amountFen = 1_900L,
            dateText = "2026-04-02 13:55:56",
            dateMillis = OcrReceiptParser.parseDateToMillis("2026-04-02 13:55:56"),
            merchantName = "\u67e0\u6c14\u4e91\u5357\u83dc(\u5b9c\u5bbe\u4e07\u8c61\u6c47\u5e97)",
        )

        val result = PaymentScreenshotParser.parse(lines = lines, fallback = fallback)

        assertNull(result)
    }

    @Test
    fun `parse returns null when only order summary labels are present`() {
        val lines = listOf(
            OcrLine(text = "\u8ba2\u5355\u53f7", left = 32, top = 188, right = 108, bottom = 218),
            OcrLine(text = "3902 0629 1364 3048 466", left = 168, top = 188, right = 488, bottom = 220),
            OcrLine(text = "\u4e0b\u5355\u65f6\u95f4", left = 32, top = 242, right = 124, bottom = 272),
            OcrLine(text = "2026-04-02 13:55:56", left = 164, top = 242, right = 414, bottom = 274),
            OcrLine(text = "\u652f\u4ed8\u65b9\u5f0f", left = 32, top = 296, right = 124, bottom = 326),
            OcrLine(text = "\u5728\u7ebf\u652f\u4ed8", left = 164, top = 296, right = 286, bottom = 328),
            OcrLine(text = "\u5546\u54c1\u8d39\u7528", left = 32, top = 840, right = 124, bottom = 872),
            OcrLine(text = "\u7528\u6237\u914d\u9001\u8d39", left = 32, top = 892, right = 152, bottom = 924),
            OcrLine(text = "\u00a51.7", left = 872, top = 894, right = 944, bottom = 926),
            OcrLine(text = "\u5408\u8ba1", left = 32, top = 946, right = 78, bottom = 986),
            OcrLine(text = "\u5df2\u4f18\u60e012\u5143 \u00a514.7", left = 620, top = 946, right = 944, bottom = 986),
        )

        val fallback = ParsedReceiptData(
            amountText = "1.7",
            amountFen = 170L,
            dateText = "2026-04-02 13:55:56",
            dateMillis = OcrReceiptParser.parseDateToMillis("2026-04-02 13:55:56"),
            merchantName = "\u67e0\u6c14\u4e91\u5357\u83dc(\u5b9c\u5bbe\u4e07\u8c61\u6c47\u5e97)",
        )

        val result = PaymentScreenshotParser.parse(lines = lines, fallback = fallback)

        assertNull(result)
    }
}

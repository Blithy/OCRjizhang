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
}

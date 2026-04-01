package com.example.ocrjizhang.utils

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OcrReceiptParserTest {

    @Test
    fun `parse prefers total amount date and merchant from common receipt text`() {
        val rawText = """
            LAWSON
            2026-04-01 18:23:15
            Americano 12.00
            Rice Ball 8.50
            Discount 2.00
            Total 18.50
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("18.50", result.amountText)
        assertEquals(1_850L, result.amountFen)
        assertEquals("2026-04-01 18:23:15", result.dateText)
        assertEquals("LAWSON", result.merchantName)
        assertNotNull(result.dateMillis)
    }

    @Test
    fun `parseDateToMillis supports chinese date format`() {
        val millis = OcrReceiptParser.parseDateToMillis("2026年04月01日09:08:07")
        val localDateTime = Instant.ofEpochMilli(millis ?: error("millis should not be null"))
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        assertEquals(2026, localDateTime.year)
        assertEquals(4, localDateTime.monthValue)
        assertEquals(1, localDateTime.dayOfMonth)
        assertEquals(9, localDateTime.hour)
        assertEquals(8, localDateTime.minute)
        assertEquals(7, localDateTime.second)
    }

    @Test
    fun `parse ignores discount style amounts when final total exists`() {
        val rawText = """
            盒马鲜生
            日期 2026/04/01 12:30
            原价 32.00
            会员优惠 5.00
            合计 27.00
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("27.00", result.amountText)
        assertEquals(2_700L, result.amountFen)
        assertEquals("盒马鲜生", result.merchantName)
    }

    @Test
    fun `parse prefers amount brand and payment datetime from payment detail screenshot text`() {
        val rawText = """
            15:35
            美团
            特价外卖团购
            服务
            当前状态
            支付时间
            商品
            收单机构
            支付方式
            交易单号
            商户单号
            支付成功
            -13.40
            美团
            2026年04月01日14:26:15
            零钱
            鹿角巷(万象宜宾天地店)-美团App-
            26040111100300001308341521990
            北京钱袋宝支付技术有限公司
            4200003095202604010844725303
            20260401142607U7777756450963
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("13.40", result.amountText)
        assertEquals(1_340L, result.amountFen)
        assertEquals("2026年04月01日14:26:15", result.dateText)
        assertEquals("美团", result.merchantName)
        assertNotNull(result.dateMillis)
    }
}

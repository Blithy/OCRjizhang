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
        assertEquals("鹿角巷(万象宜宾天地店)", result.merchantName)
        assertNotNull(result.dateMillis)
    }

    @Test
    fun `parse prefers final total over delivery fee in order detail raw text`() {
        val rawText = """
            商品费用
            柠气云南菜(宜宾万象汇店)
            打包费
            叮H5T但,D以力M
            准时宝
            用户配送费
            放心吃
            合计
            更多推荐
            外卖
            点击收起^
            已享门店新客立减等2项优惠
            【首创】糯米香柠檬茶
            超级杯,正常冰,半糖
            x1
            点击收起、
            .0l令
            [外卖性
            复制
            合收藏
            19
            1
            ¥6:7￥1.7
            黑金会员免费
            商家赠送
            外卖
            -¥7
            已优惠12元¥14.7
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("14.7", result.amountText)
        assertEquals(1_470L, result.amountFen)
        assertEquals("柠气云南菜(宜宾万象汇店)", result.merchantName)
    }

    @Test
    fun `parse prefers actual payment over product prices in ecommerce order detail`() {
        val rawText = """
            已发货
            还剩58天17小时自动确认
            运输中【上海市】预计【4月8日】到达
            南岸龙湾一号
            预计明天送达
            DSPIAE迪斯派
            88VIP好评率99%，平均16小时退款
            DSPIAE迪斯派碳纤维打磨笔
            CFB-S02
            ¥14.4
            ¥15
            x1
            DSPIAE迪斯派碳纤维打磨笔
            CB-S
            ¥11.33
            ¥11.8
            x3
            实付款
            ¥48.4
            订单信息
            2701814880074018250
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("48.4", result.amountText)
        assertEquals(4_840L, result.amountFen)
        assertEquals("DSPIAE迪斯派", result.merchantName)
    }

    @Test
    fun `parse prefers actual payment from exact ocr raw text capture`() {
        val rawText = """
            <
            16:21
            运输中【上海市】预计【4月8日】到达【自...
            >
            @南岸龙湾一号
            犹正扬 86-189****0588 号码保护中取件出示
            虚拟号>
            ①预计明天送达
            还剩58天17小时自动确认
            实付款
            DSPIAE迪斯派
            订单信息v
            已发货
            88VIP好评率99%,平均16小时退款
            客服 更多
            DSPIAE迪斯派碳纤维打磨¥14.4>
            CFB-S02
            退货宝极速退款7天无理由退货>
            CB-S
            服务保障 88VIP
            DSPIAE迪斯派碳纤维打磨
            加入购物车
            退货宝极速退款7天无理由退货>
            催物流
            进店逛逛》
            415
            查看物流
            申请售后
            X1
            ¥11.33 >
            ¥11.8
            X3
            加入购物车 申请售后
            48.4 v
            2701814880074018250复制
            查看更多>
            确认收货
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("48.4", result.amountText)
        assertEquals(4_840L, result.amountFen)
        assertEquals("DSPIAE迪斯派", result.merchantName)
    }

    @Test
    fun `parse prefers final total from exact food delivery ocr raw text capture`() {
        val rawText = """
            18:26
            く尊敬的黑金会员,已己堤前 16分钟送达
            订单号码39020629 1364 3048 466
            下单时间 2026-04-0213:55:56
            支付方式在线支付
            商品費用
            柠气云南茶(宜宾万象汇店)>
            打包费
            吓I H5T但,D以力M
            准时宝
            用戶配送费
            放心吃
            合计
            更多堆荐
            外卖
            |点击收起へ
            已享门店新客立减等2项优惠
            【首创】糯米香拧檬茶
            超級杯,正常冰,半糖
            x1
            点击收起、
            .0l令
            [外妻性
            复制
            合收藏
            19
            1
            ¥6:7 ¥ 1.7
            |黑金会员兔费
            商家赠送
            外卖
            -¥7
            已优惠12元¥14.7
        """.trimIndent()

        val result = OcrReceiptParser.parse(rawText)

        assertEquals("14.7", result.amountText)
        assertEquals(1_470L, result.amountFen)
        assertEquals("柠气云南茶(宜宾万象汇店)", result.merchantName)
    }
}

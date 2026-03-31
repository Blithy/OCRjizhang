package com.example.ocrjizhang.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountingFormattersTest {

    @Test
    fun `parseToFen converts valid decimal text`() {
        assertEquals(3250L, AccountingFormatters.parseToFen("32.50"))
        assertEquals(120000L, AccountingFormatters.parseToFen("1,200"))
        assertEquals(990L, AccountingFormatters.parseToFen("￥9.90"))
    }

    @Test
    fun `parseToFen rejects invalid or non-positive values`() {
        assertNull(AccountingFormatters.parseToFen(""))
        assertNull(AccountingFormatters.parseToFen("abc"))
        assertNull(AccountingFormatters.parseToFen("0"))
        assertNull(AccountingFormatters.parseToFen("-12.00"))
        assertNull(AccountingFormatters.parseToFen("12.345"))
    }

    @Test
    fun `formatFen renders yuan with currency prefix`() {
        assertEquals("￥32.50", AccountingFormatters.formatFen(3250L))
        assertEquals("￥1,200.00", AccountingFormatters.formatFen(120000L))
    }
}

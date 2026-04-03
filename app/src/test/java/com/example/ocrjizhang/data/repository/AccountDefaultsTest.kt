package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.AccountEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDefaultsTest {

    @Test
    fun `buildMissingDefaults returns all defaults when no account exists`() {
        val result = AccountDefaults.buildMissingDefaults(
            userId = 101L,
            existingAccounts = emptyList(),
        )

        assertEquals(4, result.size)
        assertTrue(result.all { it.isDefault })
        assertTrue(result.map { it.name }.containsAll(listOf("现金", "微信", "支付宝", "银行卡")))
    }

    @Test
    fun `buildMissingDefaults fills missing defaults even when custom account exists`() {
        val existing = listOf(
            AccountEntity(
                id = 1L,
                userId = 101L,
                name = "测试账户",
                symbol = "测",
                balanceFen = 1000L,
                isDefault = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val result = AccountDefaults.buildMissingDefaults(
            userId = 101L,
            existingAccounts = existing,
        )

        assertEquals(4, result.size)
        assertTrue(result.map { it.name }.containsAll(listOf("现金", "微信", "支付宝", "银行卡")))
    }

    @Test
    fun `buildMissingDefaults skips default names that already exist`() {
        val existing = listOf(
            AccountEntity(
                id = 1L,
                userId = 101L,
                name = "微信",
                symbol = "微",
                balanceFen = 0L,
                isDefault = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            AccountEntity(
                id = 2L,
                userId = 101L,
                name = "测试账户",
                symbol = "测",
                balanceFen = 0L,
                isDefault = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val result = AccountDefaults.buildMissingDefaults(
            userId = 101L,
            existingAccounts = existing,
        )

        assertEquals(3, result.size)
        assertTrue(result.none { it.name == "微信" })
    }

    @Test
    fun `buildDefaultUpgrades marks default account as default when same name exists`() {
        val existing = listOf(
            AccountEntity(
                id = 1L,
                userId = 101L,
                name = "微信",
                symbol = "W",
                balanceFen = 1200L,
                isDefault = false,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

        val result = AccountDefaults.buildDefaultUpgrades(existing)

        assertEquals(1, result.size)
        assertEquals("微信", result.first().name)
        assertTrue(result.first().isDefault)
        assertEquals("微", result.first().symbol)
    }
}

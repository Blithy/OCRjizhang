package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.AccountEntity
import com.example.ocrjizhang.utils.LocalIdGenerator

object AccountDefaults {
    private val defaultAccounts = listOf(
        "现金" to "现",
        "微信" to "微",
        "支付宝" to "支",
        "银行卡" to "卡",
    )

    fun buildMissingDefaults(
        userId: Long,
        existingAccounts: List<AccountEntity>,
    ): List<AccountEntity> {
        val existingNames = existingAccounts.map { it.name.lowercase() }.toSet()
        val now = System.currentTimeMillis()
        return defaultAccounts.mapNotNull { (name, symbol) ->
            if (existingNames.contains(name.lowercase())) {
                null
            } else {
                AccountEntity(
                    id = LocalIdGenerator.nextId(),
                    userId = userId,
                    name = name,
                    symbol = symbol,
                    balanceFen = 0L,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        }
    }

    fun buildSymbol(name: String): String =
        name.trim().take(1).ifBlank { "账" }
}

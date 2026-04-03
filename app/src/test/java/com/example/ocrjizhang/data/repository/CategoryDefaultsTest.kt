package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryDefaultsTest {

    @Test
    fun `buildMissingDefaults returns both income and expense defaults for new user`() {
        val result = CategoryDefaults.buildMissingDefaults(
            userId = 7L,
            existingCategories = emptyList(),
            now = 1_000L,
        )

        assertEquals(33, result.size)
        assertTrue(result.any { it.type == RecordType.EXPENSE && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
        assertTrue(result.any { it.type == RecordType.INCOME && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
        assertTrue(result.all { it.isDefault })
        assertTrue(result.all { it.syncStatus == SyncStatus.PENDING_CREATE })
        assertEquals(result.map { it.id }.distinct().size, result.size)
    }

    @Test
    fun `buildMissingDefaults skips categories that already exist`() {
        val existing = listOf(
            CategoryEntity(
                id = 1L,
                userId = 9L,
                name = "餐饮",
                type = RecordType.EXPENSE,
                icon = null,
                color = null,
                isDefault = true,
                createdAt = 1L,
                updatedAt = 1L,
                syncStatus = SyncStatus.SYNCED,
            ),
            CategoryEntity(
                id = 2L,
                userId = 9L,
                name = CategoryDefaults.UNCATEGORIZED_NAME,
                type = RecordType.INCOME,
                icon = null,
                color = null,
                isDefault = true,
                createdAt = 1L,
                updatedAt = 1L,
                syncStatus = SyncStatus.SYNCED,
            ),
        )

        val result = CategoryDefaults.buildMissingDefaults(
            userId = 9L,
            existingCategories = existing,
            now = 2_000L,
        )

        assertEquals(31, result.size)
        assertTrue(result.none { it.type == RecordType.EXPENSE && it.name == "餐饮" })
        assertTrue(result.none { it.type == RecordType.INCOME && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
    }

    @Test
    fun `buildMissingDefaults skips reseeding when legacy default count is already complete`() {
        val legacyExpenseDefaults = listOf(
            "餐饮",
            "饮品",
            "零食",
            "买菜",
            "交通",
            "打车",
            "购物",
            "服饰",
            "美妆",
            "日用",
            "娱乐",
            "运动",
            "医疗",
            "学习",
            "住房",
            "水电",
            "通讯",
            "旅行",
            "社交",
            "礼物",
            "宠物",
            "未分类",
        ).map(CategoryDefaults::legacyAliasOf)
        val legacyIncomeDefaults = listOf(
            "工资",
            "奖金",
            "兼职",
            "收款",
            "报销",
            "退款",
            "理财",
            "利息",
            "红包",
            "其他",
            "未分类",
        ).map(CategoryDefaults::legacyAliasOf)
        val existing = buildList {
            legacyExpenseDefaults.forEachIndexed { index, name ->
                add(
                    CategoryEntity(
                        id = index + 1L,
                        userId = 11L,
                        name = name,
                        type = RecordType.EXPENSE,
                        icon = null,
                        color = null,
                        isDefault = true,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncStatus = SyncStatus.SYNCED,
                    ),
                )
            }
            legacyIncomeDefaults.forEachIndexed { index, name ->
                add(
                    CategoryEntity(
                        id = index + 101L,
                        userId = 11L,
                        name = name,
                        type = RecordType.INCOME,
                        icon = null,
                        color = null,
                        isDefault = true,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncStatus = SyncStatus.SYNCED,
                    ),
                )
            }
        }

        val result = CategoryDefaults.buildMissingDefaults(
            userId = 11L,
            existingCategories = existing,
            now = 3_000L,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildMissingDefaults uses stable ids for same user`() {
        val first = CategoryDefaults.buildMissingDefaults(
            userId = 21L,
            existingCategories = emptyList(),
            now = 1_000L,
        )
        val second = CategoryDefaults.buildMissingDefaults(
            userId = 21L,
            existingCategories = emptyList(),
            now = 9_000L,
        )
        val third = CategoryDefaults.buildMissingDefaults(
            userId = 22L,
            existingCategories = emptyList(),
            now = 9_000L,
        )

        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first.map { it.id } != third.map { it.id })
    }
}

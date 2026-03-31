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

        assertEquals(14, result.size)
        assertTrue(result.any { it.type == RecordType.EXPENSE && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
        assertTrue(result.any { it.type == RecordType.INCOME && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
        assertTrue(result.all { it.isDefault })
        assertTrue(result.all { it.syncStatus == SyncStatus.PENDING_CREATE })
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

        assertEquals(12, result.size)
        assertTrue(result.none { it.type == RecordType.EXPENSE && it.name == "餐饮" })
        assertTrue(result.none { it.type == RecordType.INCOME && it.name == CategoryDefaults.UNCATEGORIZED_NAME })
    }
}

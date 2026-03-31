package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.utils.LocalIdGenerator

data class CategorySeedTemplate(
    val name: String,
    val type: RecordType,
)

object CategoryDefaults {
    const val UNCATEGORIZED_NAME = "未分类"

    private val templates = listOf(
        CategorySeedTemplate("餐饮", RecordType.EXPENSE),
        CategorySeedTemplate("交通", RecordType.EXPENSE),
        CategorySeedTemplate("购物", RecordType.EXPENSE),
        CategorySeedTemplate("日用", RecordType.EXPENSE),
        CategorySeedTemplate("娱乐", RecordType.EXPENSE),
        CategorySeedTemplate("医疗", RecordType.EXPENSE),
        CategorySeedTemplate("住房", RecordType.EXPENSE),
        CategorySeedTemplate(UNCATEGORIZED_NAME, RecordType.EXPENSE),
        CategorySeedTemplate("工资", RecordType.INCOME),
        CategorySeedTemplate("奖金", RecordType.INCOME),
        CategorySeedTemplate("兼职", RecordType.INCOME),
        CategorySeedTemplate("理财", RecordType.INCOME),
        CategorySeedTemplate("其他", RecordType.INCOME),
        CategorySeedTemplate(UNCATEGORIZED_NAME, RecordType.INCOME),
    )

    fun buildMissingDefaults(
        userId: Long,
        existingCategories: List<CategoryEntity>,
        now: Long = System.currentTimeMillis(),
    ): List<CategoryEntity> {
        val existingKeys = existingCategories.map { it.type to it.name }.toSet()
        return templates
            .filterNot { (it.type to it.name) in existingKeys }
            .mapIndexed { index, template ->
                CategoryEntity(
                    id = LocalIdGenerator.nextId(),
                    userId = userId,
                    name = template.name,
                    type = template.type,
                    icon = null,
                    color = null,
                    isDefault = true,
                    createdAt = now + index,
                    updatedAt = now + index,
                    syncStatus = SyncStatus.PENDING_CREATE,
                )
            }
    }
}

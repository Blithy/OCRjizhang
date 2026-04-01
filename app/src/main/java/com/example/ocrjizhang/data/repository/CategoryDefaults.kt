package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.utils.LocalIdGenerator
import java.nio.charset.Charset

data class CategorySeedTemplate(
    val name: String,
    val type: RecordType,
)

object CategoryDefaults {
    const val UNCATEGORIZED_NAME = "未分类"
    private val legacyGbkCharset = Charset.forName("GBK")

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
    private val templateCountByType = templates.groupingBy(CategorySeedTemplate::type).eachCount()

    fun buildMissingDefaults(
        userId: Long,
        existingCategories: List<CategoryEntity>,
        now: Long = System.currentTimeMillis(),
    ): List<CategoryEntity> {
        val existingDefaultCountByType = existingCategories
            .asSequence()
            .filter(CategoryEntity::isDefault)
            .groupingBy(CategoryEntity::type)
            .eachCount()

        return templates
            .filterNot { template ->
                val existingCount = existingDefaultCountByType[template.type] ?: 0
                val requiredCount = templateCountByType.getValue(template.type)
                if (existingCount >= requiredCount) {
                    true
                } else {
                    existingCategories.any { category ->
                        category.type == template.type && category.name == template.name
                    }
                }
            }
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

    fun legacyAliasOf(name: String): String =
        String(name.toByteArray(Charsets.UTF_8), legacyGbkCharset)

    fun uncategorizedAliases(): List<String> =
        listOf(UNCATEGORIZED_NAME, legacyAliasOf(UNCATEGORIZED_NAME)).distinct()
}

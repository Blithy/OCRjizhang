package com.example.ocrjizhang.data.repository

import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.ui.category.CategoryIconRegistry
import com.example.ocrjizhang.utils.LocalIdGenerator
import java.nio.charset.Charset
import kotlin.math.absoluteValue

data class CategorySeedTemplate(
    val name: String,
    val type: RecordType,
    val icon: String,
)

object CategoryDefaults {
    const val UNCATEGORIZED_NAME = "未分类"
    private val legacyGbkCharset = Charset.forName("GBK")

    private val templates = listOf(
        CategorySeedTemplate("餐饮", RecordType.EXPENSE, "food"),
        CategorySeedTemplate("饮品", RecordType.EXPENSE, "coffee"),
        CategorySeedTemplate("零食", RecordType.EXPENSE, "food"),
        CategorySeedTemplate("买菜", RecordType.EXPENSE, "food"),
        CategorySeedTemplate("交通", RecordType.EXPENSE, "transport"),
        CategorySeedTemplate("打车", RecordType.EXPENSE, "transport"),
        CategorySeedTemplate("购物", RecordType.EXPENSE, "bag"),
        CategorySeedTemplate("服饰", RecordType.EXPENSE, "bag"),
        CategorySeedTemplate("美妆", RecordType.EXPENSE, "bag"),
        CategorySeedTemplate("日用", RecordType.EXPENSE, "bag"),
        CategorySeedTemplate("娱乐", RecordType.EXPENSE, "fun"),
        CategorySeedTemplate("运动", RecordType.EXPENSE, "fun"),
        CategorySeedTemplate("医疗", RecordType.EXPENSE, "medical"),
        CategorySeedTemplate("学习", RecordType.EXPENSE, "book"),
        CategorySeedTemplate("住房", RecordType.EXPENSE, "home"),
        CategorySeedTemplate("水电", RecordType.EXPENSE, "home"),
        CategorySeedTemplate("通讯", RecordType.EXPENSE, "phone"),
        CategorySeedTemplate("旅行", RecordType.EXPENSE, "transport"),
        CategorySeedTemplate("社交", RecordType.EXPENSE, "heart"),
        CategorySeedTemplate("礼物", RecordType.EXPENSE, "heart"),
        CategorySeedTemplate("宠物", RecordType.EXPENSE, "heart"),
        CategorySeedTemplate(UNCATEGORIZED_NAME, RecordType.EXPENSE, "more"),
        CategorySeedTemplate("工资", RecordType.INCOME, "wallet"),
        CategorySeedTemplate("奖金", RecordType.INCOME, "wallet"),
        CategorySeedTemplate("兼职", RecordType.INCOME, "wallet"),
        CategorySeedTemplate("收款", RecordType.INCOME, "wallet"),
        CategorySeedTemplate("报销", RecordType.INCOME, "receipt"),
        CategorySeedTemplate("退款", RecordType.INCOME, "receipt"),
        CategorySeedTemplate("理财", RecordType.INCOME, "chart"),
        CategorySeedTemplate("利息", RecordType.INCOME, "chart"),
        CategorySeedTemplate("红包", RecordType.INCOME, "wallet"),
        CategorySeedTemplate("其他", RecordType.INCOME, "more"),
        CategorySeedTemplate(UNCATEGORIZED_NAME, RecordType.INCOME, "more"),
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
                    id = stableDefaultCategoryId(userId, template),
                    userId = userId,
                    name = template.name,
                    type = template.type,
                    icon = template.icon,
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

    fun iconKeyFor(name: String, type: RecordType, storedKey: String?): String =
        CategoryIconRegistry.iconKeyFor(name, type, storedKey)

    private fun stableDefaultCategoryId(userId: Long, template: CategorySeedTemplate): Long {
        val seed = "${userId}|${template.type.name}|${template.name}"
        var hash = 1469598103934665603L
        seed.forEach { char ->
            hash = hash xor char.code.toLong()
            hash *= 1099511628211L
        }
        val normalized = (hash and Long.MAX_VALUE).absoluteValue
        return if (normalized == 0L) LocalIdGenerator.nextId() else normalized
    }
}

package com.example.ocrjizhang.ui.category

import androidx.annotation.DrawableRes
import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType

data class CategoryIconOption(
    val key: String,
    val label: String,
    @DrawableRes val iconRes: Int,
)

object CategoryIconRegistry {
    private const val ICON_FOOD = "food"
    private const val ICON_COFFEE = "coffee"
    private const val ICON_TRANSPORT = "transport"
    private const val ICON_BAG = "bag"
    private const val ICON_FUN = "fun"
    private const val ICON_MEDICAL = "medical"
    private const val ICON_BOOK = "book"
    private const val ICON_HOME = "home"
    private const val ICON_HEART = "heart"
    private const val ICON_PHONE = "phone"
    private const val ICON_WALLET = "wallet"
    private const val ICON_RECEIPT = "receipt"
    private const val ICON_CHART = "chart"
    private const val ICON_MORE = "more"

    private data class IconSpec(
        val key: String,
        val label: String,
        @DrawableRes val iconRes: Int,
    )

    private val iconSpecs = listOf(
        IconSpec(ICON_FOOD, "餐饮", R.drawable.ic_category_food_24),
        IconSpec(ICON_COFFEE, "饮品", R.drawable.ic_category_coffee_24),
        IconSpec(ICON_TRANSPORT, "出行", R.drawable.ic_category_transport_24),
        IconSpec(ICON_BAG, "购物", R.drawable.ic_category_bag_24),
        IconSpec(ICON_FUN, "娱乐", R.drawable.ic_category_fun_24),
        IconSpec(ICON_MEDICAL, "医疗", R.drawable.ic_category_medical_24),
        IconSpec(ICON_BOOK, "学习", R.drawable.ic_category_book_24),
        IconSpec(ICON_HOME, "居家", R.drawable.ic_home_24),
        IconSpec(ICON_HEART, "社交", R.drawable.ic_category_heart_24),
        IconSpec(ICON_PHONE, "数码", R.drawable.ic_category_phone_24),
        IconSpec(ICON_WALLET, "钱包", R.drawable.ic_category_wallet_24),
        IconSpec(ICON_RECEIPT, "账单", R.drawable.ic_category_receipt_24),
        IconSpec(ICON_CHART, "理财", R.drawable.ic_chart_24),
        IconSpec(ICON_MORE, "更多", R.drawable.ic_category_more_24),
    )
    private val iconSpecByKey = iconSpecs.associateBy(IconSpec::key)

    private val nameToIcon = mapOf(
        "餐饮" to ICON_FOOD,
        "饮品" to ICON_COFFEE,
        "咖啡" to ICON_COFFEE,
        "奶茶" to ICON_COFFEE,
        "买菜" to ICON_FOOD,
        "交通" to ICON_TRANSPORT,
        "打车" to ICON_TRANSPORT,
        "旅行" to ICON_TRANSPORT,
        "购物" to ICON_BAG,
        "服饰" to ICON_BAG,
        "日用" to ICON_BAG,
        "娱乐" to ICON_FUN,
        "医疗" to ICON_MEDICAL,
        "学习" to ICON_BOOK,
        "住房" to ICON_HOME,
        "水电" to ICON_HOME,
        "社交" to ICON_HEART,
        "礼物" to ICON_HEART,
        "恋爱" to ICON_HEART,
        "数码" to ICON_PHONE,
        "通讯" to ICON_PHONE,
        "工资" to ICON_WALLET,
        "奖金" to ICON_WALLET,
        "兼职" to ICON_WALLET,
        "报销" to ICON_RECEIPT,
        "理财" to ICON_CHART,
        "红包" to ICON_WALLET,
        "其他" to ICON_MORE,
        "未分类" to ICON_MORE,
    )

    fun defaultKeyForType(type: RecordType): String =
        if (type == RecordType.INCOME) ICON_WALLET else ICON_MORE

    fun selectableOptions(type: RecordType): List<CategoryIconOption> {
        val orderedKeys = if (type == RecordType.EXPENSE) {
            listOf(
                ICON_FOOD,
                ICON_COFFEE,
                ICON_TRANSPORT,
                ICON_BAG,
                ICON_FUN,
                ICON_MEDICAL,
                ICON_BOOK,
                ICON_HOME,
                ICON_HEART,
                ICON_PHONE,
                ICON_RECEIPT,
                ICON_CHART,
                ICON_WALLET,
                ICON_MORE,
            )
        } else {
            listOf(
                ICON_WALLET,
                ICON_RECEIPT,
                ICON_CHART,
                ICON_HEART,
                ICON_PHONE,
                ICON_FOOD,
                ICON_COFFEE,
                ICON_HOME,
                ICON_MORE,
            )
        }
        return orderedKeys
            .distinct()
            .mapNotNull(iconSpecByKey::get)
            .map { spec ->
                CategoryIconOption(
                    key = spec.key,
                    label = spec.label,
                    iconRes = spec.iconRes,
                )
            }
    }

    fun iconKeyFor(name: String, type: RecordType, storedKey: String?): String =
        storedKey
            ?.takeIf(iconSpecByKey::containsKey)
            ?: nameToIcon[name]
            ?: defaultKeyForType(type)

    fun iconResFor(iconKey: String): Int =
        iconSpecByKey[iconKey]?.iconRes ?: R.drawable.ic_category_more_24
}

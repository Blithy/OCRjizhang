package com.example.ocrjizhang.ui.category

import com.example.ocrjizhang.R
import com.example.ocrjizhang.data.local.entity.RecordType

object CategoryIconRegistry {
    private const val ICON_FOOD = "food"
    private const val ICON_TRANSPORT = "transport"
    private const val ICON_BAG = "bag"
    private const val ICON_FUN = "fun"
    private const val ICON_MEDICAL = "medical"
    private const val ICON_BOOK = "book"
    private const val ICON_HOME = "home"
    private const val ICON_WALLET = "wallet"
    private const val ICON_RECEIPT = "receipt"
    private const val ICON_CHART = "chart"
    private const val ICON_MORE = "more"

    private val nameToIcon = mapOf(
        "餐饮" to ICON_FOOD,
        "饮品" to ICON_FOOD,
        "买菜" to ICON_FOOD,
        "交通" to ICON_TRANSPORT,
        "打车" to ICON_TRANSPORT,
        "购物" to ICON_BAG,
        "服饰" to ICON_BAG,
        "日用" to ICON_BAG,
        "娱乐" to ICON_FUN,
        "医疗" to ICON_MEDICAL,
        "学习" to ICON_BOOK,
        "住房" to ICON_HOME,
        "水电" to ICON_HOME,
        "旅行" to ICON_TRANSPORT,
        "社交" to ICON_FUN,
        "工资" to ICON_WALLET,
        "奖金" to ICON_WALLET,
        "兼职" to ICON_WALLET,
        "报销" to ICON_RECEIPT,
        "理财" to ICON_CHART,
        "红包" to ICON_WALLET,
        "其他" to ICON_MORE,
        "未分类" to ICON_MORE,
    )

    fun iconKeyFor(name: String, type: RecordType, storedKey: String?): String =
        storedKey
            ?: nameToIcon[name]
            ?: if (type == RecordType.INCOME) ICON_WALLET else ICON_MORE

    fun iconResFor(iconKey: String): Int =
        when (iconKey) {
            ICON_FOOD -> R.drawable.ic_category_food_24
            ICON_TRANSPORT -> R.drawable.ic_category_transport_24
            ICON_BAG -> R.drawable.ic_category_bag_24
            ICON_FUN -> R.drawable.ic_category_fun_24
            ICON_MEDICAL -> R.drawable.ic_category_medical_24
            ICON_BOOK -> R.drawable.ic_category_book_24
            ICON_HOME -> R.drawable.ic_home_24
            ICON_WALLET -> R.drawable.ic_category_wallet_24
            ICON_RECEIPT -> R.drawable.ic_category_receipt_24
            ICON_CHART -> R.drawable.ic_chart_24
            else -> R.drawable.ic_category_more_24
        }
}

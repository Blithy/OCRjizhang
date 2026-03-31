package com.example.ocrjizhang.ui.ocr

data class OcrNavigationPayload(
    val amount: String = "",
    val merchant: String = "",
    val remark: String = "",
    val dateMillis: Long? = null,
)

data class OcrHistoryUiModel(
    val id: Long,
    val title: String,
    val subtitle: String,
    val meta: String,
    val navigationPayload: OcrNavigationPayload,
)

data class OcrUiState(
    val isProcessing: Boolean = false,
    val isImagePreparing: Boolean = false,
    val selectedImagePath: String? = null,
    val selectedImageHint: String = "先从相册选择一张票据图片",
    val parsedAmount: String = "未识别",
    val parsedDate: String = "未识别",
    val parsedMerchant: String = "未识别",
    val rawText: String = "",
    val history: List<OcrHistoryUiModel> = emptyList(),
    val historyEmptyTitle: String = "还没有 OCR 识别记录",
    val historyEmptyBody: String = "识别成功后，最近记录会保留在这里，方便你重复带入记账。",
    val canRecognize: Boolean = false,
    val canFillTransaction: Boolean = false,
)

sealed interface OcrEvent {
    data class Message(val message: String) : OcrEvent
    data class NavigateToTransactionEditor(val payload: OcrNavigationPayload) : OcrEvent
}

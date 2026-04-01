package com.example.ocrjizhang.data.ocr

data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerX: Int
        get() = (left + right) / 2

    val centerY: Int
        get() = (top + bottom) / 2

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top
}

data class OcrStructuredResult(
    val rawText: String,
    val lines: List<OcrLine>,
)

package com.example.ocrjizhang.data.ocr

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit 文字识别引擎封装。
 * 这里把图片识别成结构化文本行，供后面的金额、日期和商户解析使用。
 */
@Singleton
class MlKitOcrEngine @Inject constructor() {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 对单张图片做 OCR，并返回“原始文本 + 每行文字坐标”。
     * 后面的支付截图解析器会强依赖这些坐标来判断：
     * 哪一行更像金额、哪一行更像商户、哪些只是标签字段。
     */
    suspend fun recognize(imagePath: String): OcrStructuredResult {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: error("Unable to decode image for ML Kit OCR")
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) {
                            continuation.resume(text.toStructuredResult())
                        }
                    }
                    .addOnFailureListener { throwable ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(throwable)
                        }
                    }
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 把 ML Kit 的识别结果转换成项目内部统一的数据结构。
     * 这里会把每一行文字保留成 OcrLine，后续规则解析只认这个结构，不直接依赖 ML Kit 原始对象。
     */
    private fun Text.toStructuredResult(): OcrStructuredResult {
        val lines = textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val normalizedText = line.text.trim()
                if (normalizedText.isBlank()) {
                    null
                } else {
                    line.toOcrLine(normalizedText)
                }
            }

        val rawText = when {
            lines.isNotEmpty() -> lines.joinToString(separator = "\n") { it.text }
            text.isNotBlank() -> text.trim()
            else -> ""
        }

        return OcrStructuredResult(
            rawText = rawText,
            lines = lines,
        )
    }

    /**
     * 把单行文字转换成“文本 + 包围盒坐标”的轻量对象。
     * 这样后续逻辑就可以只关注 left / top / right / bottom，而不用关心识别引擎本身。
     */
    private fun Text.Line.toOcrLine(normalizedText: String): OcrLine {
        val box = boundingBox ?: Rect()
        return OcrLine(
            text = normalizedText,
            left = box.left,
            top = box.top,
            right = box.right,
            bottom = box.bottom,
        )
    }

    suspend fun recognizeRawText(imagePath: String): String {
        return recognize(imagePath).rawText
    }

    suspend fun recognizeLines(imagePath: String): List<OcrLine> {
        return recognize(imagePath).lines
    }
}

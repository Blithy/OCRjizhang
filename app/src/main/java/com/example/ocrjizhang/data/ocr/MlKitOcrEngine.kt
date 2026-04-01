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

@Singleton
class MlKitOcrEngine @Inject constructor() {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

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

package com.example.ocrjizhang.data.ocr

import android.graphics.BitmapFactory
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

    suspend fun recognize(imagePath: String): String {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: error("Unable to decode image for ML Kit OCR")
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return try {
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { text ->
                        if (continuation.isActive) {
                            continuation.resume(text.toStructuredText())
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

    private fun Text.toStructuredText(): String {
        val lines = textBlocks
            .flatMap { block -> block.lines }
            .map { line -> line.text.trim() }
            .filter { line -> line.isNotBlank() }

        return when {
            lines.isNotEmpty() -> lines.joinToString(separator = "\n")
            text.isNotBlank() -> text.trim()
            else -> ""
        }
    }
}

package com.example.ocrjizhang.data.ocr

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Paddle OCR 原生桥接层。
 * 当前文件只负责把 Kotlin 调用转发给底层 JNI so 库，作为 ML Kit 失败时的本地回退方案。
 */
@Singleton
class PaddleOcrNative @Inject constructor() {

    fun recognize(imagePath: String, runtimeDir: String): String =
        recognizeNative(imagePath, runtimeDir)

    private external fun recognizeNative(imagePath: String, runtimeDir: String): String

    companion object {
        init {
            System.loadLibrary("ocr_native")
        }
    }
}

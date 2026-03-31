package com.example.ocrjizhang.data.ocr

import javax.inject.Inject
import javax.inject.Singleton

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

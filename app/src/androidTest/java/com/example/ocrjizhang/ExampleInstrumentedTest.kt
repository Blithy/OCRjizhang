package com.example.ocrjizhang

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocrjizhang.data.ocr.PaddleOcrNative
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.ocrjizhang", appContext.packageName)
    }

    @Test
    fun recognizeExternalTestImage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val imageFile = File(appContext.filesDir, "OcrTestPicture.PNG").takeIf { it.exists() }
            ?: File(appContext.getExternalFilesDir(null), "OcrTestPicture.PNG")
        assertTrue(
            "Expected test image at ${imageFile.absolutePath}",
            imageFile.exists(),
        )

        val runtimeDir = File(appContext.filesDir, "ocr_runtime_instrumented").apply {
            deleteRecursively()
            mkdirs()
        }

        appContext.assets.list("ocr").orEmpty().forEach { assetName ->
            appContext.assets.open("ocr/$assetName").use { input ->
                File(runtimeDir, assetName).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val recognizedText = PaddleOcrNative().recognize(
            imagePath = imageFile.absolutePath,
            runtimeDir = runtimeDir.absolutePath,
        )

        Log.d("ExampleInstrumentedTest", "OCR result:\n$recognizedText")
        assertTrue("OCR result should not be blank", recognizedText.isNotBlank())
    }
}

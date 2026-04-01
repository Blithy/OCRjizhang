package com.example.ocrjizhang

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocrjizhang.data.ocr.MlKitOcrEngine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

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

        val recognizedText = runBlocking {
            MlKitOcrEngine().recognize(imageFile.absolutePath)
        }

        Log.d("ExampleInstrumentedTest", "OCR result:\n${recognizedText.rawText}")
        assertTrue("OCR result should not be blank", recognizedText.rawText.isNotBlank())
        assertTrue("OCR structured lines should not be empty", recognizedText.lines.isNotEmpty())
    }
}

package com.example.ocrjizhang.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageFileUtils {

    fun createCameraCaptureUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val outputFile = File(
            cameraDir,
            "ocr-camera-${System.currentTimeMillis()}.jpg",
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile,
        )
    }

    suspend fun copyUriToCache(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        if (uri.scheme == "file") {
            return@withContext uri.toFile()
        }

        val extension = context.contentResolver.getType(uri)
            ?.substringAfterLast('/', "jpg")
            ?.ifBlank { "jpg" }
            ?: "jpg"
        val outputFile = File(
            context.cacheDir,
            "ocr-${System.currentTimeMillis()}.$extension",
        )

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: error("Unable to read selected image")

        outputFile
    }
}

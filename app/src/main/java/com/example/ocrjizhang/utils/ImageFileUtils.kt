package com.example.ocrjizhang.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageFileUtils {

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
        } ?: error("无法读取所选图片")

        outputFile
    }
}

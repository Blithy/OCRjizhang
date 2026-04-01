package com.example.ocrjizhang.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageFileUtils {

    private const val NormalizedJpegQuality = 92
    private const val MaxDecodeDimension = 2_048

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
        val outputFile = File(
            context.cacheDir,
            "ocr-${System.currentTimeMillis()}.jpg",
        )

        val normalizedBitmap = when (uri.scheme) {
            "file" -> {
                val sourceFile = uri.toFile()
                val bitmap = decodeBitmapFromFile(sourceFile)
                    ?: error("Unable to decode selected image")
                val orientation = ExifInterface(sourceFile.absolutePath)
                    .getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                rotateBitmapIfRequired(bitmap, orientation)
            }

            else -> {
                val imageBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                } ?: error("Unable to read selected image")
                val bitmap = decodeBitmapFromBytes(imageBytes)
                    ?: error("Unable to decode selected image")
                val orientation = ExifInterface(ByteArrayInputStream(imageBytes))
                    .getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                rotateBitmapIfRequired(bitmap, orientation)
            }
        }

        FileOutputStream(outputFile).use { outputStream ->
            normalizedBitmap.compress(Bitmap.CompressFormat.JPEG, NormalizedJpegQuality, outputStream)
        }
        normalizedBitmap.recycle()

        outputFile
    }

    private fun decodeBitmapFromFile(file: File): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions)
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun decodeBitmapFromBytes(bytes: ByteArray): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var sampleSize = 1
        while (height / sampleSize > MaxDecodeDimension || width / sampleSize > MaxDecodeDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
            }
        }

        if (matrix.isIdentity) {
            return bitmap
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
        if (rotatedBitmap != bitmap) {
            bitmap.recycle()
        }
        return rotatedBitmap
    }
}

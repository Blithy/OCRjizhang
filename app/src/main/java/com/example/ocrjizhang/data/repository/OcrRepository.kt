package com.example.ocrjizhang.data.repository

import android.content.Context
import com.example.ocrjizhang.data.local.dao.OcrRecordDao
import com.example.ocrjizhang.data.local.entity.OcrRecordEntity
import com.example.ocrjizhang.data.ocr.OcrLine
import com.example.ocrjizhang.data.ocr.MlKitOcrEngine
import com.example.ocrjizhang.data.ocr.OcrStructuredResult
import com.example.ocrjizhang.data.ocr.PaddleOcrNative
import com.example.ocrjizhang.utils.LocalIdGenerator
import com.example.ocrjizhang.utils.OcrReceiptParser
import com.example.ocrjizhang.utils.PaymentScreenshotParser
import com.example.ocrjizhang.utils.ParsedReceiptData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class OcrRecognitionResult(
    val imagePath: String,
    val rawText: String,
    val structuredLines: List<OcrLine>,
    val parsedData: ParsedReceiptData,
)

data class OcrHistoryRecord(
    val id: Long,
    val imagePath: String,
    val amountText: String?,
    val amountFen: Long?,
    val dateText: String?,
    val dateMillis: Long?,
    val merchantName: String?,
    val rawText: String,
    val createdAt: Long,
)

@Singleton
class OcrRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrRecordDao: OcrRecordDao,
    private val sessionManager: SessionManager,
    private val mlKitOcrEngine: MlKitOcrEngine,
    private val paddleOcrNative: PaddleOcrNative,
) {

    fun observeRecentRecords(userId: Long): Flow<List<OcrHistoryRecord>> =
        ocrRecordDao.observeRecentRecords(userId).map { records ->
            records.map { entity ->
                OcrHistoryRecord(
                    id = entity.id,
                    imagePath = entity.imageUri,
                    amountText = entity.amountText,
                    amountFen = entity.amountFen,
                    dateText = entity.dateText,
                    dateMillis = OcrReceiptParser.parseDateToMillis(entity.dateText),
                    merchantName = entity.merchantName,
                    rawText = entity.rawJson.orEmpty(),
                    createdAt = entity.createdAt,
                )
            }
        }

    suspend fun getCurrentUserId(): Long? = sessionManager.sessionFlow.first().userId

    suspend fun recognizeImage(imagePath: String): OcrRecognitionResult = withContext(Dispatchers.IO) {
        val structuredResult = recognizeStructured(imagePath)
        val fallbackParsedData = OcrReceiptParser.parse(structuredResult.rawText)
        val parsedData = PaymentScreenshotParser.parse(
            lines = structuredResult.lines,
            fallback = fallbackParsedData,
        ) ?: fallbackParsedData
        val result = OcrRecognitionResult(
            imagePath = imagePath,
            rawText = structuredResult.rawText,
            structuredLines = structuredResult.lines,
            parsedData = parsedData,
        )

        getCurrentUserId()?.let { userId ->
            saveRecord(userId, result)
        }
        result
    }

    private suspend fun recognizeStructured(imagePath: String): OcrStructuredResult {
        val mlKitResult = runCatching { mlKitOcrEngine.recognize(imagePath) }
            .getOrNull()
        if (mlKitResult != null && mlKitResult.rawText.isNotBlank()) {
            return mlKitResult
        }

        val runtimeDir = ensureRuntimeFiles()
        val rawText = paddleOcrNative.recognize(imagePath, runtimeDir.absolutePath).trim()
        return OcrStructuredResult(
            rawText = rawText,
            lines = emptyList(),
        )
    }

    private suspend fun saveRecord(userId: Long, result: OcrRecognitionResult) {
        ocrRecordDao.upsert(
            OcrRecordEntity(
                id = LocalIdGenerator.nextId(),
                userId = userId,
                imageUri = result.imagePath,
                amountText = result.parsedData.amountText,
                amountFen = result.parsedData.amountFen,
                dateText = result.parsedData.dateText,
                merchantName = result.parsedData.merchantName,
                rawJson = result.rawText,
                createdAt = System.currentTimeMillis(),
            ),
        )
        ocrRecordDao.trimToRecent(userId)
    }

    private fun ensureRuntimeFiles(): File {
        val runtimeDir = File(context.filesDir, "ocr_runtime").apply { mkdirs() }
        val assetNames = context.assets.list("ocr").orEmpty()
        assetNames.forEach { assetName ->
            val targetFile = File(runtimeDir, assetName)
            copyAssetIfNeeded("ocr/$assetName", targetFile)
        }
        return runtimeDir
    }

    private fun copyAssetIfNeeded(assetPath: String, targetFile: File) {
        context.assets.open(assetPath).use { inputStream ->
            if (targetFile.exists() && targetFile.length() == inputStream.available().toLong()) {
                return
            }
            targetFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}

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

/**
 * OCR 仓库。
 * 负责调起识别引擎、解析识别结果、保存 OCR 历史，并把结果回填给记账模块。
 */
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

    /**
     * OCR 主入口。
     * 整体流程分成四步：
     * 1. 先做底层文字识别，尽量拿到“结构化行信息”。
     * 2. 先用通用票据解析器做一轮兜底提取。
     * 3. 如果当前图片更像支付截图，再用支付截图解析器做更强的规则筛选。
     * 4. 把最终结果保存到 OCR 历史，供用户重复带入记账。
     */
    suspend fun recognizeImage(imagePath: String): OcrRecognitionResult = withContext(Dispatchers.IO) {
        val structuredResult = recognizeStructured(imagePath)

        // 通用解析器更偏“无坐标兜底”，只看纯文本内容，适合普通票据和截图都先过一遍。
        val fallbackParsedData = OcrReceiptParser.parse(structuredResult.rawText)

        // 支付截图解析器会额外利用文本块的坐标关系做筛选。
        // 如果它判断这张图不像支付详情页，会直接返回 null，此时继续使用通用解析结果。
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

    /**
     * 结构化识别阶段。
     * 优先使用 ML Kit，因为它能返回每一行文字的坐标信息，这对后续“金额筛选”“商户识别”非常重要。
     * 如果 ML Kit 失败或识别不到有效文本，再回退到本地 Paddle OCR。
     */
    private suspend fun recognizeStructured(imagePath: String): OcrStructuredResult {
        val mlKitResult = runCatching { mlKitOcrEngine.recognize(imagePath) }
            .getOrNull()
        if (mlKitResult != null && mlKitResult.rawText.isNotBlank()) {
            return mlKitResult
        }

        // Paddle 当前主要作为“文字兜底来源”使用。
        // 它返回的是原始文本，所以回退后只能依赖通用文本解析，无法再使用坐标级规则。
        val runtimeDir = ensureRuntimeFiles()
        val rawText = paddleOcrNative.recognize(imagePath, runtimeDir.absolutePath).trim()
        return OcrStructuredResult(
            rawText = rawText,
            lines = emptyList(),
        )
    }

    /**
     * 把 OCR 结果落到本地历史表中。
     * 历史记录的价值在于：用户识别过一次后，可以快速重复带入，不需要每次重新拍照。
     */
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

    /**
     * 确保 Paddle 运行时模型已经复制到应用私有目录。
     * 这样 JNI 层就可以直接读取模型文件完成本地 OCR。
     */
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

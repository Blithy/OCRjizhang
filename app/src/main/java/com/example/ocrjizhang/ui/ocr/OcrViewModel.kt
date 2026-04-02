package com.example.ocrjizhang.ui.ocr

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.repository.OcrHistoryRecord
import com.example.ocrjizhang.data.repository.OcrRecognitionResult
import com.example.ocrjizhang.data.repository.OcrRepository
import com.example.ocrjizhang.utils.AccountingFormatters
import com.example.ocrjizhang.utils.ImageFileUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class OcrViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrRepository: OcrRepository,
) : ViewModel() {

    private val currentUserId = MutableStateFlow<Long?>(null)
    private val isProcessing = MutableStateFlow(false)
    private val isImagePreparing = MutableStateFlow(false)
    private val selectedImagePath = MutableStateFlow<String?>(null)
    private val currentResult = MutableStateFlow<OcrRecognitionResult?>(null)

    private val _eventFlow = MutableSharedFlow<OcrEvent>()
    val eventFlow: SharedFlow<OcrEvent> = _eventFlow.asSharedFlow()

    private val historyState = currentUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            ocrRepository.observeRecentRecords(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val uiState: StateFlow<OcrUiState> = combine(
        isProcessing,
        isImagePreparing,
        selectedImagePath,
        currentResult,
        historyState,
    ) { processing, preparingImage, imagePath, result, history ->
        val navigationPayload = result?.toNavigationPayload()
        val selectedFileName = imagePath
            ?.substringAfterLast('\\')
            ?.substringAfterLast('/')

        OcrUiState(
            isProcessing = processing,
            isImagePreparing = preparingImage,
            selectedImagePath = imagePath,
            selectedImageHint = selectedFileName?.let { "已选择图片：$it，点击“开始识别”后即可提取金额、日期和商户。" }
                ?: "先拍照或从相册选择一张票据图片，再开始识别。",
            parsedAmount = result?.parsedData?.amountText ?: "未识别",
            parsedDate = result?.parsedData?.dateText ?: "未识别",
            parsedMerchant = result?.parsedData?.merchantName ?: "未识别",
            rawText = result?.rawText.orEmpty(),
            history = history.map(::toHistoryUiModel),
            canRecognize = imagePath != null && !processing && !preparingImage,
            canFillTransaction = navigationPayload != null && navigationPayload.hasAnyValue(),
            canClearSelection = imagePath != null && !processing && !preparingImage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OcrUiState(),
    )

    init {
        viewModelScope.launch {
            currentUserId.value = ocrRepository.getCurrentUserId()
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            isImagePreparing.value = true
            runCatching {
                ImageFileUtils.copyUriToCache(context, uri)
            }.onSuccess { file ->
                selectedImagePath.value = file.absolutePath
                currentResult.value = null
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "图片准备失败，请重新选择")
            }
            isImagePreparing.value = false
        }
    }

    fun recognizeSelectedImage() {
        val imagePath = selectedImagePath.value
        if (imagePath == null) {
            emitMessage("请先选择一张票据图片")
            return
        }

        viewModelScope.launch {
            isProcessing.value = true
            runCatching {
                ocrRepository.recognizeImage(imagePath)
            }.onSuccess { result ->
                currentResult.value = result
                if (result.rawText.isBlank()) {
                    emitMessage("未识别到清晰文字，请换一张更清晰的票据")
                } else {
                    emitMessage("识别完成，先核对金额、日期和商户，再带入记账表单。")
                }
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "OCR 识别失败，请稍后重试")
            }
            isProcessing.value = false
        }
    }

    fun clearSelectedImage() {
        selectedImagePath.value = null
        currentResult.value = null
    }

    fun fillCurrentResult() {
        val payload = currentResult.value?.toNavigationPayload()
        if (payload == null || !payload.hasAnyValue()) {
            emitMessage("当前结果还没有可带入的字段")
            return
        }
        viewModelScope.launch {
            _eventFlow.emit(OcrEvent.NavigateToTransactionEditor(payload))
        }
    }

    fun fillHistoryRecord(item: OcrHistoryUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(OcrEvent.NavigateToTransactionEditor(item.navigationPayload))
        }
    }

    private fun toHistoryUiModel(record: OcrHistoryRecord): OcrHistoryUiModel {
        val title = record.merchantName
            ?.takeIf { it.isNotBlank() }
            ?: "OCR 识别记录"
        val subtitle = buildList {
            record.amountText?.takeIf { it.isNotBlank() }?.let { add("金额 $it") }
            record.dateText?.takeIf { it.isNotBlank() }?.let { add("日期 $it") }
            if (isEmpty()) {
                add(
                    record.rawText.lineSequence()
                        .map(String::trim)
                        .firstOrNull { it.isNotBlank() }
                        ?.take(28)
                        ?: "识别结果待手动确认",
                )
            }
        }.joinToString(" · ")

        return OcrHistoryUiModel(
            id = record.id,
            title = title,
            subtitle = subtitle,
            meta = AccountingFormatters.formatDateTime(record.createdAt),
            navigationPayload = OcrNavigationPayload(
                amount = record.amountText.orEmpty(),
                merchant = record.merchantName.orEmpty(),
                dateMillis = record.dateMillis,
            ),
        )
    }

    private fun OcrRecognitionResult.toNavigationPayload(): OcrNavigationPayload =
        OcrNavigationPayload(
            amount = parsedData.amountText.orEmpty(),
            merchant = parsedData.merchantName.orEmpty(),
            dateMillis = parsedData.dateMillis,
        )

    private fun OcrNavigationPayload.hasAnyValue(): Boolean =
        amount.isNotBlank() || merchant.isNotBlank() || !remark.isNullOrBlank() || dateMillis != null

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(OcrEvent.Message(message))
        }
    }
}

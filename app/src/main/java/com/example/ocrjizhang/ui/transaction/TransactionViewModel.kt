package com.example.ocrjizhang.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.TransactionEntity
import com.example.ocrjizhang.data.repository.CategoryRepository
import com.example.ocrjizhang.data.repository.SessionManager
import com.example.ocrjizhang.data.repository.TransactionRepository
import com.example.ocrjizhang.utils.AccountingFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val currentUserId = MutableStateFlow<Long?>(null)
    private val selectedType = MutableStateFlow(RecordType.EXPENSE)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val amountInput = MutableStateFlow("")
    private val dateMillis = MutableStateFlow(System.currentTimeMillis())
    private val merchantInput = MutableStateFlow("")
    private val remarkInput = MutableStateFlow("")
    private val editingTransactionId = MutableStateFlow<Long?>(null)
    private val pendingEditRequestId = MutableStateFlow<Long?>(null)
    private val fromOcrPrefill = MutableStateFlow(false)
    private var prefillApplied = false

    private val _eventFlow = MutableSharedFlow<TransactionEvent>()
    val eventFlow: SharedFlow<TransactionEvent> = _eventFlow.asSharedFlow()

    private val transactionsState = currentUserId
        .flatMapLatest(::observeTransactions)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val categoriesState = combine(currentUserId, selectedType) { userId, type ->
        userId to type
    }.flatMapLatest { (userId, type) ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            categoryRepository.observeCategories(userId, type)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val uiState: StateFlow<TransactionUiState> = combine(
        currentUserId,
        selectedType,
        selectedCategoryId,
        amountInput,
        dateMillis,
        merchantInput,
        remarkInput,
        editingTransactionId,
        fromOcrPrefill,
        categoriesState,
    ) { values: Array<Any?> ->
        val userId = values[0] as Long?
        val type = values[1] as RecordType
        val manualCategoryId = values[2] as Long?
        val amount = values[3] as String
        val selectedDate = values[4] as Long
        val merchant = values[5] as String
        val remark = values[6] as String
        val editingId = values[7] as Long?
        val showOcrPrefill = values[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val categories = values[9] as List<CategoryEntity>

        val effectiveCategoryId = categories.firstOrNull { it.id == manualCategoryId }?.id
            ?: categories.firstOrNull()?.id

        TransactionUiState(
            isLoading = userId == null,
            selectedType = type,
            categories = categories.map { category ->
                CategoryOption(
                    id = category.id,
                    name = category.name,
                    symbol = category.name.take(1),
                    isSelected = category.id == effectiveCategoryId,
                )
            },
            selectedCategoryId = effectiveCategoryId,
            amountInput = amount,
            amountDisplay = formatAmountDisplay(amount),
            dateLabel = AccountingFormatters.formatDateTime(selectedDate),
            dateMillis = selectedDate,
            merchantInput = merchant,
            remarkInput = remark,
            detailLabel = buildDetailLabel(merchant, remark),
            isEditing = editingId != null,
            submitLabel = if (editingId == null) {
                "完成"
            } else {
                "更新完成"
            },
            secondaryLabel = if (editingId == null) {
                "再记一笔"
            } else {
                "更新后继续"
            },
            showDeleteButton = editingId != null,
            showOcrPrefillHint = showOcrPrefill,
            ocrPrefillTitle = if (showOcrPrefill) {
                "已带入 OCR 识别结果"
            } else {
                ""
            },
            ocrPrefillBody = if (showOcrPrefill) {
                "金额、商户和时间已经预填好，你可以补充分类或备注后直接保存。"
            } else {
                ""
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionUiState(),
    )

    init {
        viewModelScope.launch {
            sessionManager.sessionFlow
                .map { it.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    currentUserId.value = userId
                }
        }

        viewModelScope.launch {
            combine(pendingEditRequestId, transactionsState) { requestedId, transactions ->
                requestedId?.let { id -> transactions.firstOrNull { it.id == id } }
            }.collect { transaction ->
                if (transaction != null) {
                    populateEditor(transaction)
                    pendingEditRequestId.value = null
                }
            }
        }
    }

    fun onTypeSelected(type: RecordType) {
        selectedType.value = type
        if (editingTransactionId.value == null) {
            selectedCategoryId.value = null
        }
    }

    fun onCategorySelected(categoryId: Long) {
        selectedCategoryId.value = categoryId
    }

    fun onMerchantChanged(value: String) {
        merchantInput.value = value
    }

    fun onRemarkChanged(value: String) {
        remarkInput.value = value
    }

    fun onDateSelected(selectedMillis: Long) {
        dateMillis.value = selectedMillis
    }

    fun appendAmountDigit(token: String) {
        val current = amountInput.value
        val updated = if (current.contains(".")) {
            val fraction = current.substringAfter('.', "")
            if (fraction.length >= 2) {
                current
            } else {
                current + token.take(2 - fraction.length)
            }
        } else {
            current + token
        }
        amountInput.value = normalizeAmount(updated)
    }

    fun appendDecimalPoint() {
        val current = amountInput.value
        if (current.contains('.')) return
        amountInput.value = if (current.isBlank()) {
            "0."
        } else {
            "$current."
        }
    }

    fun removeLastAmountChar() {
        val current = amountInput.value
        if (current.isBlank()) return
        amountInput.value = current.dropLast(1)
    }

    fun clearAmount() {
        amountInput.value = ""
    }

    fun applyPrefill(
        amount: String,
        merchant: String,
        remark: String,
        dateMillis: Long?,
    ) {
        if (prefillApplied) return
        prefillApplied = true

        var hasAnyPrefill = false
        if (amount.isNotBlank()) {
            amountInput.value = normalizeAmount(amount)
            hasAnyPrefill = true
        }
        if (merchant.isNotBlank()) {
            merchantInput.value = merchant
            hasAnyPrefill = true
        }
        if (remark.isNotBlank()) {
            remarkInput.value = remark
            hasAnyPrefill = true
        }
        if (dateMillis != null) {
            this.dateMillis.value = dateMillis
            hasAnyPrefill = true
        }
        if (hasAnyPrefill) {
            selectedType.value = RecordType.EXPENSE
            fromOcrPrefill.value = true
            emitMessage("已带入 OCR 识别结果，可手动调整后再保存")
        }
    }

    fun requestEdit(transactionId: Long?) {
        pendingEditRequestId.value = transactionId
    }

    fun saveAndClose() {
        persistTransaction(closeAfterSave = true)
    }

    fun saveAndContinue() {
        persistTransaction(closeAfterSave = false)
    }

    fun startEditing(transactionId: Long) {
        pendingEditRequestId.value = transactionId
    }

    fun clearForm() {
        editingTransactionId.value = null
        fromOcrPrefill.value = false
        amountInput.value = ""
        dateMillis.value = System.currentTimeMillis()
        merchantInput.value = ""
        remarkInput.value = ""
        selectedCategoryId.value = null
    }

    fun deleteCurrentTransaction() {
        val userId = currentUserId.value
        val transactionId = editingTransactionId.value
        if (userId == null) {
            emitMessage("登录状态已失效，请重新登录")
            return
        }
        if (transactionId == null) {
            emitMessage("当前没有可删除的记录")
            return
        }

        viewModelScope.launch {
            runCatching {
                transactionRepository.deleteTransaction(userId, transactionId)
            }.onSuccess {
                clearForm()
                emitMessage("交易已删除")
                emitCloseEvent()
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "删除失败，请稍后重试")
            }
        }
    }

    private fun persistTransaction(closeAfterSave: Boolean) {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("登录状态已失效，请重新登录")
            return
        }

        val amountFen = AccountingFormatters.parseToFen(amountInput.value)
        if (amountFen == null) {
            emitMessage("请先输入正确金额，最多保留两位小数")
            return
        }

        val categoryId = uiState.value.selectedCategoryId
        if (categoryId == null) {
            emitMessage("请先选择分类")
            return
        }

        viewModelScope.launch {
            runCatching {
                val editingId = editingTransactionId.value
                if (editingId == null) {
                    transactionRepository.createTransaction(
                        userId = userId,
                        type = selectedType.value,
                        amountFen = amountFen,
                        categoryId = categoryId,
                        transactionTime = dateMillis.value,
                        merchantName = merchantInput.value,
                        remark = remarkInput.value,
                    )
                } else {
                    transactionRepository.updateTransaction(
                        userId = userId,
                        transactionId = editingId,
                        type = selectedType.value,
                        amountFen = amountFen,
                        categoryId = categoryId,
                        transactionTime = dateMillis.value,
                        merchantName = merchantInput.value,
                        remark = remarkInput.value,
                    )
                }
            }.onSuccess {
                val editing = editingTransactionId.value != null
                if (closeAfterSave) {
                    emitMessage(if (editing) "交易已更新" else "交易已保存")
                    emitCloseEvent()
                } else {
                    emitMessage(if (editing) "交易已更新，继续记下一笔" else "交易已保存，继续记下一笔")
                    clearForm()
                }
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "保存失败，请稍后重试")
            }
        }
    }

    private fun observeTransactions(userId: Long?): Flow<List<TransactionEntity>> {
        if (userId == null) return flowOf(emptyList())
        return transactionRepository.observeTransactions(userId)
    }

    private fun populateEditor(transaction: TransactionEntity) {
        editingTransactionId.value = transaction.id
        fromOcrPrefill.value = false
        selectedType.value = transaction.type
        selectedCategoryId.value = transaction.categoryId
        amountInput.value = AccountingFormatters.formatFenForInput(transaction.amountFen)
        dateMillis.value = transaction.transactionTime
        merchantInput.value = transaction.merchantName.orEmpty()
        remarkInput.value = transaction.remark.orEmpty()
    }

    private fun normalizeAmount(raw: String): String {
        if (raw.isBlank()) return ""
        if (!raw.contains('.')) {
            return raw.trimStart('0').ifBlank { "0" }
        }

        val hasTrailingDot = raw.endsWith(".")
        val integerPart = raw.substringBefore('.').trimStart('0').ifBlank { "0" }
        val fractionPart = raw.substringAfter('.', "").take(2)
        return if (hasTrailingDot && fractionPart.isEmpty()) {
            "$integerPart."
        } else {
            "$integerPart.$fractionPart"
        }
    }

    private fun formatAmountDisplay(raw: String): String =
        if (raw.isBlank()) {
            "¥0.00"
        } else {
            "¥$raw"
        }

    private fun buildDetailLabel(merchant: String, remark: String): String =
        buildList {
            merchant.takeIf { it.isNotBlank() }?.let(::add)
            remark.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(" · ").ifBlank {
            "点击填写备注"
        }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(TransactionEvent.Message(message))
        }
    }

    private fun emitCloseEvent() {
        viewModelScope.launch {
            _eventFlow.emit(TransactionEvent.SavedAndClose)
        }
    }
}

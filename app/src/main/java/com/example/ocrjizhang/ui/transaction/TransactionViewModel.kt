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
        transactionsState,
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
        @Suppress("UNCHECKED_CAST")
        val transactions = values[10] as List<TransactionEntity>

        val effectiveCategoryId = categories
            .firstOrNull { it.id == manualCategoryId }
            ?.id
            ?: categories.firstOrNull()?.id

        TransactionUiState(
            isLoading = userId == null,
            selectedType = type,
            categories = categories.map { CategoryOption(id = it.id, name = it.name) },
            selectedCategoryId = effectiveCategoryId,
            amountInput = amount,
            dateLabel = AccountingFormatters.formatDate(selectedDate),
            dateMillis = selectedDate,
            merchantInput = merchant,
            remarkInput = remark,
            isEditing = editingId != null,
            submitLabel = if (editingId == null) {
                "\u4fdd\u5b58\u8fd9\u7b14\u8bb0\u8d26"
            } else {
                "\u66f4\u65b0\u8fd9\u7b14\u8bb0\u8d26"
            },
            secondaryLabel = if (editingId == null) {
                "\u6e05\u7a7a\u8868\u5355"
            } else {
                "\u53d6\u6d88\u7f16\u8f91"
            },
            showOcrPrefillHint = showOcrPrefill,
            ocrPrefillTitle = if (showOcrPrefill) {
                "\u5df2\u5e26\u5165 OCR \u8bc6\u522b\u7ed3\u679c"
            } else {
                ""
            },
            ocrPrefillBody = if (showOcrPrefill) {
                "\u8fd9\u7b14\u652f\u51fa\u5df2\u9884\u586b\u91d1\u989d\u3001\u5546\u6237\u548c\u65e5\u671f\uff0c\u518d\u68c0\u67e5\u5206\u7c7b\u6216\u8865\u5145\u5907\u6ce8\u540e\u5c31\u53ef\u4ee5\u76f4\u63a5\u4fdd\u5b58\u3002"
            } else {
                ""
            },
            transactions = transactions.map(::toListItem),
            emptyTitle = if (userId == null) {
                "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u8bb0\u8d26"
            } else {
                "\u8fd8\u6ca1\u6709\u4ea4\u6613\u8bb0\u5f55"
            },
            emptyBody = if (userId == null) {
                "\u5f53\u524d\u6ca1\u6709\u53ef\u7528\u4f1a\u8bdd\uff0c\u91cd\u65b0\u767b\u5f55\u540e\u5c31\u80fd\u7ee7\u7eed\u8bb0\u8d26\u3002"
            } else {
                "\u5148\u4fdd\u5b58\u4e00\u7b14\u6536\u5165\u6216\u652f\u51fa\uff0c\u8fd9\u91cc\u5c31\u4f1a\u5f00\u59cb\u51fa\u73b0\u771f\u5b9e\u8bb0\u5f55\u3002"
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

    fun onAmountChanged(value: String) {
        amountInput.value = value
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
            amountInput.value = amount
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
            emitMessage("\u5df2\u5e26\u5165 OCR \u8bc6\u522b\u7ed3\u679c\uff0c\u53ef\u624b\u52a8\u8c03\u6574\u540e\u518d\u4fdd\u5b58")
        }
    }

    fun submit() {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55")
            return
        }

        val amountFen = AccountingFormatters.parseToFen(amountInput.value)
        if (amountFen == null) {
            emitMessage("\u8bf7\u8f93\u5165\u6b63\u786e\u7684\u91d1\u989d\uff0c\u6700\u591a\u4fdd\u7559\u4e24\u4f4d\u5c0f\u6570")
            return
        }

        val categoryId = uiState.value.selectedCategoryId
        if (categoryId == null) {
            emitMessage("\u8bf7\u5148\u9009\u62e9\u5206\u7c7b")
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
                emitMessage(
                    if (editingTransactionId.value == null) {
                        "\u4ea4\u6613\u5df2\u4fdd\u5b58"
                    } else {
                        "\u4ea4\u6613\u5df2\u66f4\u65b0"
                    },
                )
                clearForm()
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
            }
        }
    }

    fun startEditing(transactionId: Long) {
        val transaction = transactionsState.value.firstOrNull { it.id == transactionId } ?: return
        editingTransactionId.value = transaction.id
        fromOcrPrefill.value = false
        selectedType.value = transaction.type
        selectedCategoryId.value = transaction.categoryId
        amountInput.value = AccountingFormatters.formatFenForInput(transaction.amountFen)
        dateMillis.value = transaction.transactionTime
        merchantInput.value = transaction.merchantName.orEmpty()
        remarkInput.value = transaction.remark.orEmpty()
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

    fun deleteTransaction(transactionId: Long) {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("\u767b\u5f55\u72b6\u6001\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55")
            return
        }
        viewModelScope.launch {
            runCatching {
                transactionRepository.deleteTransaction(userId, transactionId)
            }.onSuccess {
                if (editingTransactionId.value == transactionId) {
                    clearForm()
                }
                emitMessage("\u4ea4\u6613\u5df2\u5220\u9664")
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "\u5220\u9664\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
            }
        }
    }

    private fun observeTransactions(userId: Long?): Flow<List<TransactionEntity>> {
        if (userId == null) return flowOf(emptyList())
        return transactionRepository.observeTransactions(userId)
    }

    private fun toListItem(entity: TransactionEntity): TransactionListItem {
        val subtitleParts = buildList {
            entity.merchantName?.takeIf { it.isNotBlank() }?.let(::add)
            entity.remark?.takeIf { it.isNotBlank() }?.let(::add)
        }
        return TransactionListItem(
            id = entity.id,
            title = entity.categoryName,
            subtitle = subtitleParts.joinToString(" \u00b7 ").ifBlank {
                "\u65e0\u5907\u6ce8\u4fe1\u606f"
            },
            meta = AccountingFormatters.formatDateTime(entity.transactionTime),
            amountLabel = AccountingFormatters.formatFen(entity.amountFen),
            type = entity.type,
        )
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(TransactionEvent.Message(message))
        }
    }
}

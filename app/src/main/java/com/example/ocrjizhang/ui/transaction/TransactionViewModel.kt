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
        @Suppress("UNCHECKED_CAST")
        val categories = values[8] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[9] as List<TransactionEntity>

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
            submitLabel = if (editingId == null) "保存这笔记账" else "更新这笔记账",
            secondaryLabel = if (editingId == null) "清空表单" else "取消编辑",
            transactions = transactions.map(::toListItem),
            emptyTitle = if (userId == null) "请先登录后再记账" else "还没有交易记录",
            emptyBody = if (userId == null) {
                "当前没有可用会话，重新登录后就能继续记账。"
            } else {
                "先保存一笔收入或支出，这里就会开始出现真实记录。"
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

    fun submit() {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("登录状态已失效，请重新登录")
            return
        }

        val amountFen = AccountingFormatters.parseToFen(amountInput.value)
        if (amountFen == null) {
            emitMessage("请输入正确的金额，最多保留两位小数")
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
                emitMessage(if (editingTransactionId.value == null) "交易已保存" else "交易已更新")
                clearForm()
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "保存失败，请稍后重试")
            }
        }
    }

    fun startEditing(transactionId: Long) {
        val transaction = transactionsState.value.firstOrNull { it.id == transactionId } ?: return
        editingTransactionId.value = transaction.id
        selectedType.value = transaction.type
        selectedCategoryId.value = transaction.categoryId
        amountInput.value = AccountingFormatters.formatFenForInput(transaction.amountFen)
        dateMillis.value = transaction.transactionTime
        merchantInput.value = transaction.merchantName.orEmpty()
        remarkInput.value = transaction.remark.orEmpty()
    }

    fun clearForm() {
        editingTransactionId.value = null
        amountInput.value = ""
        dateMillis.value = System.currentTimeMillis()
        merchantInput.value = ""
        remarkInput.value = ""
        selectedCategoryId.value = null
    }

    fun deleteTransaction(transactionId: Long) {
        val userId = currentUserId.value
        if (userId == null) {
            emitMessage("登录状态已失效，请重新登录")
            return
        }
        viewModelScope.launch {
            runCatching {
                transactionRepository.deleteTransaction(userId, transactionId)
            }.onSuccess {
                if (editingTransactionId.value == transactionId) {
                    clearForm()
                }
                emitMessage("交易已删除")
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "删除失败，请稍后重试")
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
            subtitle = subtitleParts.joinToString(" · ").ifBlank { "无备注信息" },
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

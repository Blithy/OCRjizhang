package com.example.ocrjizhang.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.repository.CategoryDefaults
import com.example.ocrjizhang.data.repository.CategoryRepository
import com.example.ocrjizhang.data.repository.SessionManager
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

private data class CategoryBuckets(
    val expense: List<CategoryEntity> = emptyList(),
    val income: List<CategoryEntity> = emptyList(),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val selectedType = MutableStateFlow(RecordType.EXPENSE)
    private val currentUserId = MutableStateFlow<Long?>(null)

    private val _eventFlow = MutableSharedFlow<CategoryEvent>()
    val eventFlow: SharedFlow<CategoryEvent> = _eventFlow.asSharedFlow()

    val uiState: StateFlow<CategoryUiState> = combine(
        selectedType,
        currentUserId,
        currentUserId.flatMapLatest(::observeBuckets),
    ) { type, userId, buckets ->
        val visibleCategories = if (type == RecordType.EXPENSE) {
            buckets.expense
        } else {
            buckets.income
        }
        val listItems = visibleCategories.map(::toListItem)
        CategoryUiState(
            isLoading = userId != null && visibleCategories.isEmpty(),
            selectedType = type,
            categories = listItems,
            emptyTitle = if (userId == null) "当前没有可用会话" else "这一组分类还是空的",
            emptyBody = if (userId == null) {
                "请先重新登录，再继续管理分类。"
            } else {
                "可以先创建一个自定义分类，后续交易录入时就能直接选择。"
            },
            actionLabel = if (type == RecordType.EXPENSE) "新增支出分类" else "新增收入分类",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryUiState(),
    )

    init {
        viewModelScope.launch {
            sessionManager.sessionFlow
                .map { it.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    currentUserId.value = userId
                    if (userId != null) {
                        categoryRepository.ensureDefaultCategories(userId)
                    }
                }
        }
    }

    fun onTypeSelected(type: RecordType) {
        selectedType.value = type
    }

    fun addCategory(name: String, iconKey: String) {
        launchMutation(successMessage = "分类已添加") { userId ->
            categoryRepository.createCategory(
                userId = userId,
                type = selectedType.value,
                rawName = name,
                iconKey = iconKey,
            )
        }
    }

    fun updateCategory(categoryId: Long, name: String, iconKey: String) {
        launchMutation(successMessage = "分类已更新") { userId ->
            categoryRepository.updateCategory(
                userId = userId,
                categoryId = categoryId,
                rawName = name,
                iconKey = iconKey,
            )
        }
    }

    fun deleteCategory(categoryId: Long) {
        launchMutation(successMessage = "分类已删除，历史交易已迁移到未分类") { userId ->
            categoryRepository.deleteCategory(
                userId = userId,
                categoryId = categoryId,
            )
        }
    }

    private fun observeBuckets(userId: Long?): Flow<CategoryBuckets> {
        if (userId == null) return flowOf(CategoryBuckets())
        return combine(
            categoryRepository.observeCategories(userId, RecordType.EXPENSE),
            categoryRepository.observeCategories(userId, RecordType.INCOME),
        ) { expense, income ->
            CategoryBuckets(expense = expense, income = income)
        }
    }

    private fun launchMutation(
        successMessage: String,
        mutation: suspend (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == null) {
                _eventFlow.emit(CategoryEvent.Message("登录状态已失效，请重新登录"))
                return@launch
            }

            runCatching { mutation(userId) }
                .onSuccess {
                    _eventFlow.emit(CategoryEvent.Message(successMessage))
                }
                .onFailure { throwable ->
                    _eventFlow.emit(
                        CategoryEvent.Message(
                            throwable.message ?: "操作失败，请稍后重试",
                        ),
                    )
                }
        }
    }

    private fun toListItem(category: CategoryEntity): CategoryListItem =
        CategoryListItem(
            id = category.id,
            name = category.name,
            iconKey = CategoryDefaults.iconKeyFor(
                name = category.name,
                type = category.type,
                storedKey = category.icon,
            ),
            detail = if (category.isDefault) "系统默认分类" else "自定义分类",
            isDefault = category.isDefault,
            canEdit = !category.isDefault,
            canDelete = !category.isDefault,
        )
}

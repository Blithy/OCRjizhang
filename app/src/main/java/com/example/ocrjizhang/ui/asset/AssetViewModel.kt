package com.example.ocrjizhang.ui.asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocrjizhang.data.local.entity.AccountEntity
import com.example.ocrjizhang.data.repository.AccountDefaults
import com.example.ocrjizhang.data.repository.AccountRepository
import com.example.ocrjizhang.data.repository.SessionManager
import com.example.ocrjizhang.utils.AccountingFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AssetViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val currentUserId = MutableStateFlow<Long?>(null)

    private val _eventFlow = MutableSharedFlow<AssetEvent>()
    val eventFlow: SharedFlow<AssetEvent> = _eventFlow.asSharedFlow()

    private val accountsState = currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                accountRepository.observeAccounts(userId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<AssetUiState> = combine(currentUserId, accountsState) { userId, accounts ->
            val totalAssets = accounts.sumOf { it.balanceFen }
            AssetUiState(
                isLoading = false,
                totalAssetLabel = AccountingFormatters.formatFen(totalAssets),
                accountCountLabel = "${accounts.size} 个账户",
                statusLabel = buildStatusLabel(accounts),
                defaultAccountsLabel = "已预设 ${AccountDefaults.defaultAccountNames().size} 个常用账户",
                accounts = accounts.map(::toAccountItem),
                emptyTitle = if (userId == null) "当前没有可用会话" else "还没有资金账户",
                emptyBody = if (userId == null) {
                    "请先重新登录，再继续查看资产。"
                } else {
                    "可以先新增一个钱包、银行卡或支付账户，方便演示总资产和资金分布。"
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AssetUiState(),
        )

    init {
        viewModelScope.launch {
            sessionManager.sessionFlow
                .map { it.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    currentUserId.value = userId
                    if (userId != null) {
                        accountRepository.ensureDefaultAccounts(userId)
                    }
                }
        }
    }

    fun addAccount(name: String, balanceInput: String) {
        mutate("账户已添加") { userId ->
            accountRepository.createAccount(
                userId = userId,
                rawName = name,
                balanceFen = parseBalance(balanceInput),
            )
        }
    }

    fun updateAccount(accountId: Long, name: String, balanceInput: String) {
        mutate("账户已更新") { userId ->
            accountRepository.updateAccount(
                userId = userId,
                accountId = accountId,
                rawName = name,
                balanceFen = parseBalance(balanceInput),
            )
        }
    }

    fun deleteAccount(accountId: Long) {
        mutate("账户已删除") { userId ->
            accountRepository.deleteAccount(userId, accountId)
        }
    }

    private fun mutate(
        successMessage: String,
        action: suspend (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == null) {
                _eventFlow.emit(AssetEvent.Message("登录状态已失效，请重新登录"))
                return@launch
            }

            runCatching { action(userId) }
                .onSuccess {
                    _eventFlow.emit(AssetEvent.Message(successMessage))
                }
                .onFailure { throwable ->
                    _eventFlow.emit(AssetEvent.Message(throwable.message ?: "操作失败，请稍后重试"))
                }
        }
    }

    private fun parseBalance(rawInput: String): Long =
        AccountingFormatters.parseToFen(rawInput)
            ?: error("请输入正确余额，最多保留两位小数")

    private fun buildStatusLabel(accounts: List<AccountEntity>): String {
        if (accounts.isEmpty()) return "等待首次添加"

        val latestUpdatedAt = accounts.maxOf { it.updatedAt }
        val diffMillis = System.currentTimeMillis() - latestUpdatedAt
        val minuteMillis = 60_000L
        val hourMillis = 60 * minuteMillis
        val dayMillis = 24 * hourMillis

        return when {
            diffMillis < 10 * minuteMillis -> "刚刚更新"
            diffMillis < hourMillis -> "${(diffMillis / minuteMillis).coerceAtLeast(1)} 分钟前"
            diffMillis < dayMillis -> "${(diffMillis / hourMillis).coerceAtLeast(1)} 小时前"
            else -> AccountingFormatters.formatDate(latestUpdatedAt)
        }
    }

    private fun toAccountItem(account: AccountEntity): AssetAccountItem =
        AssetAccountItem(
            id = account.id,
            name = account.name,
            symbol = account.symbol,
            balanceLabel = AccountingFormatters.formatFen(account.balanceFen),
        )
}

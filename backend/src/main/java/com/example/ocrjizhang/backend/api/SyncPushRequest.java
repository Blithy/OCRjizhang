package com.example.ocrjizhang.backend.api;

import java.util.List;

public record SyncPushRequest(
    List<AccountDto> createAccounts,
    List<AccountDto> updateAccounts,
    List<Long> deleteAccountIds,
    List<CategoryDto> createCategories,
    List<CategoryDto> updateCategories,
    List<Long> deleteCategoryIds,
    List<TransactionDto> createTransactions,
    List<TransactionDto> updateTransactions,
    List<Long> deleteTransactionIds
) {
}

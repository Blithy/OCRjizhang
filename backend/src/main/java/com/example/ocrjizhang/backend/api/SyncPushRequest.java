package com.example.ocrjizhang.backend.api;

import java.util.List;

public record SyncPushRequest(
    List<CategoryDto> createCategories,
    List<CategoryDto> updateCategories,
    List<Long> deleteCategoryIds,
    List<TransactionDto> createTransactions,
    List<TransactionDto> updateTransactions,
    List<Long> deleteTransactionIds
) {
}

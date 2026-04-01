package com.example.ocrjizhang.backend.api;

public record TransactionDto(
    long id,
    long userId,
    String type,
    long amountFen,
    long categoryId,
    String categoryName,
    String remark,
    String merchantName,
    long transactionTime,
    String source,
    long createdAt,
    long updatedAt
) {
}

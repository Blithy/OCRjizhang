package com.example.ocrjizhang.backend.api;

import java.util.List;

public record SyncPullPayloadDto(
    List<CategoryDto> categories,
    List<TransactionDto> transactions,
    long serverTime
) {
}

package com.example.ocrjizhang.data.local.entity

enum class RecordType {
    INCOME,
    EXPENSE,
}

enum class TransactionSource {
    MANUAL,
    OCR,
}

enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}

enum class SyncEntityType {
    TRANSACTION,
    CATEGORY,
    ACCOUNT,
}

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
}

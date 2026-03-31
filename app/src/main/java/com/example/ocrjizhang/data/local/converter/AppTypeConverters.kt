package com.example.ocrjizhang.data.local.converter

import androidx.room.TypeConverter
import com.example.ocrjizhang.data.local.entity.RecordType
import com.example.ocrjizhang.data.local.entity.SyncEntityType
import com.example.ocrjizhang.data.local.entity.SyncOperationType
import com.example.ocrjizhang.data.local.entity.SyncStatus
import com.example.ocrjizhang.data.local.entity.TransactionSource

class AppTypeConverters {

    @TypeConverter
    fun toRecordType(value: String): RecordType = RecordType.valueOf(value)

    @TypeConverter
    fun fromRecordType(value: RecordType): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType = SyncEntityType.valueOf(value)

    @TypeConverter
    fun fromSyncEntityType(value: SyncEntityType): String = value.name

    @TypeConverter
    fun toSyncOperationType(value: String): SyncOperationType = SyncOperationType.valueOf(value)

    @TypeConverter
    fun fromSyncOperationType(value: SyncOperationType): String = value.name
}

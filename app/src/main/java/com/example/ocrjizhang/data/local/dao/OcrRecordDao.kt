package com.example.ocrjizhang.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocrjizhang.data.local.entity.OcrRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrRecordDao {

    @Query("SELECT * FROM ocr_records WHERE userId = :userId ORDER BY createdAt DESC LIMIT 50")
    fun observeRecentRecords(userId: Long): Flow<List<OcrRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: OcrRecordEntity)

    @Query("DELETE FROM ocr_records WHERE id NOT IN (SELECT id FROM ocr_records WHERE userId = :userId ORDER BY createdAt DESC LIMIT 50)")
    suspend fun trimToRecent(userId: Long)
}

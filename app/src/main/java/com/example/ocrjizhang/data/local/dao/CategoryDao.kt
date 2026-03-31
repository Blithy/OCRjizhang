package com.example.ocrjizhang.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ocrjizhang.data.local.entity.CategoryEntity
import com.example.ocrjizhang.data.local.entity.RecordType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY isDefault DESC, name ASC")
    fun observeCategories(userId: Long, type: RecordType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategories(userId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query(
        """
        SELECT COUNT(*) FROM categories
        WHERE userId = :userId AND type = :type AND LOWER(name) = LOWER(:name) AND id != :excludedId
        """
    )
    suspend fun countByName(userId: Long, type: RecordType, name: String, excludedId: Long): Int

    @Query(
        """
        SELECT * FROM categories
        WHERE userId = :userId AND type = :type AND name = :name
        LIMIT 1
        """
    )
    suspend fun findByExactName(userId: Long, type: RecordType, name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)
}

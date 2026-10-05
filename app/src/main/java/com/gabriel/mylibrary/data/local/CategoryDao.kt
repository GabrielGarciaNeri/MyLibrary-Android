package com.gabriel.mylibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gabriel.mylibrary.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortPosition ASC, id ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): Category?

    @Query("SELECT * FROM categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByName(normalizedName: String): Category?

    @Query("SELECT COALESCE(MAX(sortPosition), -1) FROM categories")
    suspend fun lastSortPosition(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category): Int

    @Query("DELETE FROM categories WHERE id = :id AND id != 6")
    suspend fun delete(id: Long): Int
}

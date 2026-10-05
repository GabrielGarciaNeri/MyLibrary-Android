package com.gabriel.mylibrary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryItemDao {
    @Query("SELECT * FROM library_items ORDER BY id ASC")
    fun observeAll(): Flow<List<LibraryItem>>

    @Query("SELECT * FROM library_items WHERE id = :id")
    fun observeItem(id: Long): Flow<LibraryItem?>

    @Query("SELECT * FROM library_items WHERE id = :id")
    suspend fun getItem(id: Long): LibraryItem?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: LibraryItem): Long

    @Update
    suspend fun update(item: LibraryItem): Int

    @Query("DELETE FROM library_items WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("UPDATE library_items SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleFavorite(id: Long, updatedAt: Long): Int

    @Query("UPDATE library_items SET currentProgress = MIN(MAX(currentProgress + :delta, 0), totalCount), updatedAt = CASE WHEN currentProgress != MIN(MAX(currentProgress + :delta, 0), totalCount) THEN :updatedAt ELSE updatedAt END WHERE id = :id")
    suspend fun adjustProgress(id: Long, delta: Int, updatedAt: Long): Int

    @Query("SELECT COUNT(*) FROM library_items WHERE coverFileName = :filename")
    suspend fun coverReferenceCount(filename: String): Int

    @Query("UPDATE library_items SET categoryId = :replacementId, updatedAt = :updatedAt WHERE categoryId = :categoryId")
    suspend fun reassignCategory(categoryId: Long, replacementId: Long, updatedAt: Long): Int

    @Query("UPDATE library_items SET currentProgress = :current, totalCount = :total, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, current: Int, total: Int, updatedAt: Long): Int
}

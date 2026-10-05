package com.gabriel.mylibrary.data.repository

import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.model.Category
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    val items: Flow<List<LibraryItem>>
    val categories: Flow<List<Category>>
    fun observeItem(id: Long): Flow<LibraryItem?>
    suspend fun getItem(id: Long): LibraryItem?
    suspend fun save(item: LibraryItem): Long
    suspend fun delete(id: Long)
    suspend fun toggleFavorite(id: Long)
    suspend fun adjustProgress(id: Long, delta: Int): Boolean
    suspend fun updateProgress(id: Long, current: Int, total: Int)
    suspend fun saveCategory(category: Category): Long
    suspend fun deleteCategory(id: Long, replacementId: Long = Category.UNCATEGORIZED_ID)
    suspend fun deleteUnusedCover(filename: String)
}

package com.gabriel.mylibrary.data.repository

import androidx.room.withTransaction
import com.gabriel.mylibrary.data.covers.CoverStore
import com.gabriel.mylibrary.data.local.LibraryDatabase
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.model.Category
import com.gabriel.mylibrary.model.CoverType
import com.gabriel.mylibrary.util.validateItem
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomLibraryRepository(
    private val database: LibraryDatabase,
    private val coverStore: CoverStore,
) : LibraryRepository {
    private val dao = database.itemDao()
    private val categoryDao = database.categoryDao()
    private val coverMutex = Mutex()

    override val items: Flow<List<LibraryItem>> = dao.observeAll()
    override val categories: Flow<List<Category>> = categoryDao.observeAll()
    override fun observeItem(id: Long): Flow<LibraryItem?> = dao.observeItem(id)
    override suspend fun getItem(id: Long): LibraryItem? = dao.getItem(id)

    override suspend fun save(item: LibraryItem): Long = coverMutex.withLock {
        require(item.id >= 0) { "Invalid item identifier." }
        val validation = validateItem(item.title, item.currentProgress.toString(), item.totalCount.toString())
        require(validation.isValid) {
            validation.titleError ?: validation.currentError ?: validation.totalError ?: "Invalid library item."
        }
        when (item.coverType) {
            CoverType.NONE -> Unit
            CoverType.BUILT_IN -> require(!item.coverPresetId.isNullOrBlank()) { "Choose a built-in cover." }
            CoverType.CUSTOM_FILE -> require(coverStore.resolveFile(item.coverFileName)?.isFile == true) {
                "The selected cover is unavailable. Choose the image again."
            }
        }
        var previousCover: String? = null
        val savedId = database.withTransaction {
            require(categoryDao.getCategory(item.categoryId) != null) { "This category is no longer available. Choose another category." }
            val now = System.currentTimeMillis()
            val existing = if (item.id == 0L) {
                null
            } else {
                dao.getItem(item.id) ?: throw NoSuchElementException("This item is no longer in your library.")
            }
            previousCover = existing?.coverFileName
            val saved = item.copy(
                title = item.title.trim(),
                author = item.author.trim(),
                description = item.description.trim(),
                subcategory = item.subcategory.trim().ifBlank { "General" },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                coverPresetId = item.coverPresetId.takeIf { item.coverType == CoverType.BUILT_IN },
                coverFileName = item.coverFileName.takeIf { item.coverType == CoverType.CUSTOM_FILE },
            )
            if (item.id == 0L) {
                dao.insert(saved)
            } else {
                checkChanged(dao.update(saved))
                saved.id
            }
        }
        previousCover?.let { removeCoverIfUnused(it) }
        savedId
    }

    override suspend fun delete(id: Long) = coverMutex.withLock {
        val previousCover = database.withTransaction {
            val item = dao.getItem(id)
            dao.delete(id)
            item?.coverFileName
        }
        previousCover?.let { removeCoverIfUnused(it) }
        Unit
    }

    override suspend fun toggleFavorite(id: Long) {
        checkChanged(dao.toggleFavorite(id, System.currentTimeMillis()))
    }

    override suspend fun adjustProgress(id: Long, delta: Int): Boolean = database.withTransaction {
        val before = dao.getItem(id) ?: throw NoSuchElementException("This item is no longer in your library.")
        checkChanged(dao.adjustProgress(id, delta, System.currentTimeMillis()))
        val after = dao.getItem(id) ?: throw NoSuchElementException("This item is no longer in your library.")
        after.totalCount > 0 && before.currentProgress < before.totalCount && after.currentProgress == after.totalCount
    }

    override suspend fun updateProgress(id: Long, current: Int, total: Int) {
        require(current >= 0 && total >= 0 && current <= total) { "Progress must be between zero and the total." }
        checkChanged(dao.updateProgress(id, current, total, System.currentTimeMillis()))
    }

    override suspend fun saveCategory(category: Category): Long = database.withTransaction {
        require(category.id >= 0) { "Invalid category identifier." }
        val name = category.name.trim()
        require(name.isNotBlank()) { "Enter a category name." }
        require(name.length <= Category.MAX_NAME_LENGTH) { "Use 40 characters or fewer." }
        require(category.colorArgb in 0xFF000000L..0xFFFFFFFFL) { "Choose an opaque category color." }
        val normalized = name.lowercase(Locale.ROOT)
        val duplicate = categoryDao.findByName(normalized)
        require(duplicate == null || duplicate.id == category.id) { "A category with this name already exists." }
        val existing = if (category.id == 0L) {
            null
        } else {
            categoryDao.getCategory(category.id) ?: throw NoSuchElementException("This category is no longer available.")
        }
        if (category.id == Category.UNCATEGORIZED_ID) {
            require(name == existing?.name) { "Uncategorized cannot be renamed." }
        }
        val saved = category.copy(
            name = name,
            normalizedName = normalized,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            sortPosition = existing?.sortPosition ?: (categoryDao.lastSortPosition() + 1),
            defaultKey = existing?.defaultKey,
        )
        if (category.id == 0L) {
            categoryDao.insert(saved)
        } else {
            check(categoryDao.update(saved) != 0) { "This category is no longer available." }
            saved.id
        }
    }

    override suspend fun deleteCategory(id: Long, replacementId: Long) {
        require(id != Category.UNCATEGORIZED_ID) { "Uncategorized cannot be deleted." }
        require(id != replacementId) { "Choose a different replacement category." }
        database.withTransaction {
            require(categoryDao.getCategory(id) != null) { "This category is no longer available." }
            require(categoryDao.getCategory(replacementId) != null) { "The replacement category is no longer available." }
            dao.reassignCategory(id, replacementId, System.currentTimeMillis())
            check(categoryDao.delete(id) != 0) { "This category is no longer available." }
        }
    }

    override suspend fun deleteUnusedCover(filename: String) = coverMutex.withLock {
        removeCoverIfUnused(filename)
    }

    private suspend fun removeCoverIfUnused(filename: String) {
        if (dao.coverReferenceCount(filename) == 0) {
            runCatching { coverStore.delete(filename) }
        }
    }

    private fun checkChanged(rows: Int) {
        if (rows == 0) throw NoSuchElementException("This item is no longer in your library.")
    }
}

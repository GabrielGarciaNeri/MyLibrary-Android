package com.gabriel.mylibrary.ui.screens

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.mylibrary.data.covers.CoverStore
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.data.repository.LibraryRepository
import com.gabriel.mylibrary.model.Category
import com.gabriel.mylibrary.model.CoverPreset
import com.gabriel.mylibrary.model.CoverType
import com.gabriel.mylibrary.util.validateItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ItemDraft(
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val currentProgress: String = "0",
    val totalCount: String = "0",
    val isFavorite: Boolean = false,
    val categoryId: Long = Category.BOOK_ID,
    val subcategory: String = "General",
    val coverType: CoverType = CoverType.NONE,
    val coverPresetId: String? = null,
    val coverFileName: String? = null,
)

enum class FormLoadError { NOT_FOUND, FAILED }

data class ItemFormState(
    val draft: ItemDraft = ItemDraft(),
    val categories: List<Category> = emptyList(),
    val categoriesLoading: Boolean = true,
    val categoriesFailed: Boolean = false,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isImportingCover: Boolean = false,
    val coverImportFailed: Boolean = false,
    val showValidation: Boolean = false,
    val loadError: FormLoadError? = null,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val discarded: Boolean = false,
    val isDirty: Boolean = false,
)

class ItemFormViewModel(
    private val repository: LibraryRepository,
    private val savedStateHandle: SavedStateHandle,
    private val coverStore: CoverStore,
) : ViewModel() {
    private val itemId = savedStateHandle.get<Long>("itemId") ?: 0L
    private var baseline = readDraft("baseline")
    private var createdAt = savedStateHandle.get<Long>("createdAt") ?: 0L
    private val initialized = savedStateHandle.get<Boolean>("initialized") == true
    private val initialDraft = if (initialized) readDraft("draft") else ItemDraft()
    private val ownedCoverFiles = savedStateHandle.get<ArrayList<String>>("draftCoverFiles")?.toMutableSet() ?: mutableSetOf()
    private var categoryObservation: Job? = null
    private val mutableState = MutableStateFlow(
        ItemFormState(
            draft = initialDraft,
            isEditing = itemId != 0L,
            isLoading = itemId != 0L && !initialized,
            showValidation = savedStateHandle.get<Boolean>("showValidation") ?: false,
            isDirty = initialDraft != baseline,
        ),
    )
    val state = mutableState.asStateFlow()

    init {
        observeCategories()
        if (!initialized) {
            if (itemId == 0L) initialize(ItemDraft(), 0L) else loadItem()
        }
    }

    fun retryLoad() {
        if (!state.value.isLoading) loadItem()
        if (state.value.categoriesFailed) observeCategories()
    }

    fun observeCategories() {
        categoryObservation?.cancel()
        mutableState.value = state.value.copy(categoriesLoading = true, categoriesFailed = false)
        categoryObservation = viewModelScope.launch {
            try {
                repository.categories.collect { categories ->
                    mutableState.value = state.value.copy(categories = categories, categoriesLoading = false)
                    reconcileCategory()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(categoriesLoading = false, categoriesFailed = true)
            }
        }
    }

    private fun reconcileCategory() {
        val current = state.value
        if (!current.isLoading && current.categories.isNotEmpty() && current.categories.none { it.id == current.draft.categoryId }) {
            val fallback = current.categories.firstOrNull { it.id == Category.UNCATEGORIZED_ID } ?: current.categories.first()
            updateDraft(current.draft.copy(categoryId = fallback.id))
        }
    }

    private fun loadItem() {
        mutableState.value = state.value.copy(isLoading = true, loadError = null)
        viewModelScope.launch {
            try {
                val item = repository.getItem(itemId)
                if (item == null) {
                    mutableState.value = state.value.copy(isLoading = false, loadError = FormLoadError.NOT_FOUND)
                } else {
                    initialize(
                        ItemDraft(
                            title = item.title,
                            author = item.author,
                            description = item.description,
                            currentProgress = item.currentProgress.toString(),
                            totalCount = item.totalCount.toString(),
                            isFavorite = item.isFavorite,
                            categoryId = item.categoryId,
                            subcategory = item.subcategory,
                            coverType = item.coverType,
                            coverPresetId = item.coverPresetId,
                            coverFileName = item.coverFileName,
                        ),
                        item.createdAt,
                    )
                    reconcileCategory()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(isLoading = false, loadError = FormLoadError.FAILED)
            }
        }
    }

    private fun initialize(draft: ItemDraft, timestamp: Long) {
        baseline = draft
        createdAt = timestamp
        writeDraft("baseline", draft)
        writeDraft("draft", draft)
        savedStateHandle["createdAt"] = timestamp
        savedStateHandle["initialized"] = true
        mutableState.value = state.value.copy(draft = draft, isLoading = false, loadError = null, isDirty = false)
    }

    fun updateDraft(transform: (ItemDraft) -> ItemDraft) = updateDraft(transform(state.value.draft))

    fun updateDraft(draft: ItemDraft) {
        if (state.value.isSaving || state.value.isLoading || state.value.isImportingCover || state.value.loadError != null) return
        writeDraft("draft", draft)
        mutableState.value = state.value.copy(draft = draft, isDirty = draft != baseline, saveFailed = false)
    }

    fun selectCategory(category: Category) {
        val draft = state.value.draft
        if (draft.categoryId == category.id) return
        updateDraft(draft.copy(categoryId = category.id, subcategory = category.subcategories.first()))
    }

    fun selectBuiltInCover(presetId: String) {
        if (CoverPreset.entries.none { it.id == presetId }) return
        replaceCover(CoverType.BUILT_IN, presetId, null)
    }

    fun randomCover() = selectBuiltInCover(CoverPreset.entries.random().id)

    fun removeCover() = replaceCover(CoverType.NONE, null, null)

    private fun replaceCover(type: CoverType, presetId: String?, filename: String?) {
        if (state.value.isSaving || state.value.isLoading || state.value.isImportingCover) return
        val oldFilename = state.value.draft.coverFileName
        updateDraft(state.value.draft.copy(coverType = type, coverPresetId = presetId, coverFileName = filename))
        mutableState.value = state.value.copy(coverImportFailed = false)
        if (oldFilename != null && oldFilename != filename && oldFilename in ownedCoverFiles) {
            viewModelScope.launch { cleanupCover(oldFilename) }
        }
    }

    fun importCover(uri: Uri) {
        if (state.value.isSaving || state.value.isLoading || state.value.isImportingCover) return
        mutableState.value = state.value.copy(isImportingCover = true, coverImportFailed = false)
        viewModelScope.launch {
            try {
                val filename = coverStore.importImage(uri)
                ownedCoverFiles.add(filename)
                saveOwnedCoverFiles()
                mutableState.value = state.value.copy(isImportingCover = false)
                replaceCover(CoverType.CUSTOM_FILE, null, filename)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(coverImportFailed = true)
            } finally {
                mutableState.value = state.value.copy(isImportingCover = false)
                reconcileCategory()
            }
        }
    }

    fun discard() {
        if (state.value.isSaving || state.value.isImportingCover) return
        mutableState.value = state.value.copy(isSaving = true)
        viewModelScope.launch {
            ownedCoverFiles.toList().forEach { cleanupCover(it) }
            mutableState.value = state.value.copy(isSaving = false, discarded = true)
        }
    }

    private suspend fun cleanupCover(filename: String) {
        try {
            repository.deleteUnusedCover(filename)
            ownedCoverFiles.remove(filename)
            saveOwnedCoverFiles()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
        }
    }

    private fun saveOwnedCoverFiles() {
        savedStateHandle["draftCoverFiles"] = ArrayList(ownedCoverFiles)
    }

    fun save() {
        reconcileCategory()
        val current = state.value
        if (current.isSaving || current.isLoading || current.isImportingCover || current.loadError != null || current.saved ||
            current.categoriesLoading || current.categoriesFailed || current.categories.isEmpty()) return
        savedStateHandle["showValidation"] = true
        val draft = current.draft
        val validation = validateItem(draft.title, draft.currentProgress, draft.totalCount)
        mutableState.value = current.copy(showValidation = true, saveFailed = false)
        if (!validation.isValid) return
        mutableState.value = state.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.save(
                    LibraryItem(
                        id = itemId,
                        title = draft.title.trim(),
                        author = draft.author.trim(),
                        description = draft.description.trim(),
                        categoryId = draft.categoryId,
                        subcategory = draft.subcategory,
                        currentProgress = draft.currentProgress.trim().toInt(),
                        totalCount = draft.totalCount.trim().toInt(),
                        isFavorite = draft.isFavorite,
                        createdAt = if (itemId == 0L) now else createdAt,
                        updatedAt = now,
                        coverType = draft.coverType,
                        coverPresetId = draft.coverPresetId,
                        coverFileName = draft.coverFileName,
                    ),
                )
                ownedCoverFiles.toList().forEach { cleanupCover(it) }
                mutableState.value = state.value.copy(isSaving = false, saved = true, isDirty = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(isSaving = false)
                reconcileCategory()
                mutableState.value = state.value.copy(saveFailed = true)
            }
        }
    }

    private fun readDraft(prefix: String) = ItemDraft(
        title = savedStateHandle["$prefix.title"] ?: "",
        author = savedStateHandle["$prefix.author"] ?: "",
        description = savedStateHandle["$prefix.description"] ?: "",
        currentProgress = savedStateHandle["$prefix.current"] ?: "0",
        totalCount = savedStateHandle["$prefix.total"] ?: "0",
        isFavorite = savedStateHandle["$prefix.favorite"] ?: false,
        categoryId = savedStateHandle["$prefix.categoryId"] ?: Category.BOOK_ID,
        subcategory = savedStateHandle["$prefix.subcategory"] ?: "General",
        coverType = savedStateHandle.get<String>("$prefix.coverType")?.let { saved -> CoverType.entries.find { it.name == saved } } ?: CoverType.NONE,
        coverPresetId = savedStateHandle["$prefix.coverPresetId"],
        coverFileName = savedStateHandle["$prefix.coverFileName"],
    )

    private fun writeDraft(prefix: String, draft: ItemDraft) {
        savedStateHandle["$prefix.title"] = draft.title
        savedStateHandle["$prefix.author"] = draft.author
        savedStateHandle["$prefix.description"] = draft.description
        savedStateHandle["$prefix.current"] = draft.currentProgress
        savedStateHandle["$prefix.total"] = draft.totalCount
        savedStateHandle["$prefix.favorite"] = draft.isFavorite
        savedStateHandle["$prefix.categoryId"] = draft.categoryId
        savedStateHandle["$prefix.subcategory"] = draft.subcategory
        savedStateHandle["$prefix.coverType"] = draft.coverType.name
        savedStateHandle["$prefix.coverPresetId"] = draft.coverPresetId
        savedStateHandle["$prefix.coverFileName"] = draft.coverFileName
    }
}

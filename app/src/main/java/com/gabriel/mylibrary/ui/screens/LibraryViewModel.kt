package com.gabriel.mylibrary.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.data.repository.LibraryRepository
import com.gabriel.mylibrary.data.repository.PreferencesRepository
import com.gabriel.mylibrary.model.Category
import com.gabriel.mylibrary.model.LibraryPreferences
import com.gabriel.mylibrary.model.LayoutMode
import com.gabriel.mylibrary.model.SortOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val preferences: LibraryPreferences = LibraryPreferences(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false
)

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()
    private val _actionFailed = MutableStateFlow(false)
    val actionFailed = _actionFailed.asStateFlow()
    private val _completed = MutableStateFlow<String?>(null)
    val completed = _completed.asStateFlow()
    fun clearCompletion() { _completed.value = null }
    private val retries = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            retries.collectLatest {
                _state.value = _state.value.copy(isLoading = true, loadFailed = false)
                combine(repository.items, repository.categories, preferencesRepository.preferences) { items, categories, preferences ->
                    LibraryUiState(items, categories, preferences, isLoading = false)
                }.catch {
                    if (it is CancellationException) throw it
                    _state.value = _state.value.copy(isLoading = false, loadFailed = true)
                }.collect { _state.value = it }
            }
        }
    }
    fun retry() { retries.value += 1 }
    fun clearError() { _actionFailed.value = false }
    fun toggleFavorite(id: Long) = act { repository.toggleFavorite(id) }
    fun delete(id: Long) = act { repository.delete(id) }
    fun adjustProgress(id: Long, delta: Int) = act {
        if (repository.adjustProgress(id, delta)) _completed.value = repository.getItem(id)?.title
    }
    fun exactProgress(id: Long, current: Int) = act {
        val item = repository.getItem(id) ?: return@act
        repository.updateProgress(id, current, item.totalCount)
        if (current == item.totalCount && current > item.currentProgress) _completed.value = item.title
    }
    fun saveCategory(category: Category, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try { repository.saveCategory(category); onResult(null) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                onResult(error.message ?: "Unable to save category.")
            }
        }
    }
    fun deleteCategory(id: Long, replacementId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try { repository.deleteCategory(id, replacementId); onResult(true) }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                _actionFailed.value = true; onResult(false)
            }
        }
    }
    fun setLayout(layout: LayoutMode) = act { preferencesRepository.setLayout(layout) }
    fun setSortOrder(sortOrder: SortOrder) = act { preferencesRepository.setSortOrder(sortOrder) }
    private fun act(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (error: Exception) {
                if (error is CancellationException) throw error
                _actionFailed.value = true
            }
        }
    }
}

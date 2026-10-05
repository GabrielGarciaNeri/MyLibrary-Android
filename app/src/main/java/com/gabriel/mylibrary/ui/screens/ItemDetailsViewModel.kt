package com.gabriel.mylibrary.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.data.repository.LibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import com.gabriel.mylibrary.model.Category
import kotlinx.coroutines.launch

data class ItemDetailsState(
    val item: LibraryItem? = null,
    val category: Category? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isWorking: Boolean = false,
    val actionFailed: Boolean = false,
    val deleted: Boolean = false,
    val progressUpdated: Int = 0,
    val completionCount: Int = 0,
)

class ItemDetailsViewModel(
    private val repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val itemId = savedStateHandle.get<Long>("itemId") ?: 0L
    private val mutableState = MutableStateFlow(ItemDetailsState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { retryLoad() }

    fun retryLoad() {
        observation?.cancel()
        mutableState.value = state.value.copy(isLoading = true, loadFailed = false)
        observation = viewModelScope.launch {
            try {
                combine(repository.observeItem(itemId), repository.categories) { item, categories ->
                    item to categories.firstOrNull { it.id == item?.categoryId }
                }.collect { (item, category) ->
                    mutableState.value = state.value.copy(item = item, category = category, isLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(isLoading = false, loadFailed = true)
            }
        }
    }

    fun toggleFavorite() = perform { repository.toggleFavorite(itemId) }
    fun adjustProgress(delta: Int) = perform {
        if (repository.adjustProgress(itemId, delta)) {
            mutableState.value = state.value.copy(completionCount = state.value.completionCount + 1)
        }
    }
    fun updateProgress(current: Int, total: Int) = perform {
        val previous = state.value.item
        repository.updateProgress(itemId, current, total)
        val justCompleted = total > 0 && current == total && previous?.let { it.totalCount <= 0 || it.currentProgress < it.totalCount } == true
        mutableState.value = state.value.copy(
            progressUpdated = state.value.progressUpdated + 1,
            completionCount = state.value.completionCount + if (justCompleted) 1 else 0,
        )
    }

    fun delete() = perform {
        repository.delete(itemId)
        mutableState.value = state.value.copy(deleted = true)
    }

    fun clearError() { mutableState.value = state.value.copy(actionFailed = false) }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.isWorking || state.value.item == null || state.value.deleted) return
        mutableState.value = state.value.copy(isWorking = true, actionFailed = false)
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(actionFailed = true)
            } finally {
                mutableState.value = state.value.copy(isWorking = false)
            }
        }
    }
}

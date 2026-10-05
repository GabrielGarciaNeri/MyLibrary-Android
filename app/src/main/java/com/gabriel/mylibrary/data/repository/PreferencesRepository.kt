package com.gabriel.mylibrary.data.repository

import com.gabriel.mylibrary.model.LayoutMode
import com.gabriel.mylibrary.model.LibraryPreferences
import com.gabriel.mylibrary.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<LibraryPreferences>
    suspend fun setLayout(layout: LayoutMode)
    suspend fun setSortOrder(sortOrder: SortOrder)
}

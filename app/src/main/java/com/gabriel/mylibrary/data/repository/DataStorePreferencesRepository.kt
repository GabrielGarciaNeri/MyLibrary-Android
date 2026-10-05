package com.gabriel.mylibrary.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gabriel.mylibrary.model.LayoutMode
import com.gabriel.mylibrary.model.LibraryPreferences
import com.gabriel.mylibrary.model.SortOrder
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.libraryPreferences by preferencesDataStore(name = "library_preferences")

class DataStorePreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {
    constructor(context: Context) : this(context.applicationContext.libraryPreferences)

    override val preferences: Flow<LibraryPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            LibraryPreferences(
                layout = LayoutMode.entries.firstOrNull { it.name == values[LAYOUT] } ?: LayoutMode.GRID,
                sortOrder = SortOrder.entries.firstOrNull { it.name == values[SORT_ORDER] } ?: SortOrder.TITLE_ASC,
            )
        }

    override suspend fun setLayout(layout: LayoutMode) {
        dataStore.edit { it[LAYOUT] = layout.name }
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[SORT_ORDER] = sortOrder.name }
    }

    private companion object {
        val LAYOUT = stringPreferencesKey("layout")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }
}

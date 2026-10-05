package com.gabriel.mylibrary

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.gabriel.mylibrary.data.covers.CoverStore
import com.gabriel.mylibrary.data.local.LibraryDatabase
import com.gabriel.mylibrary.data.repository.DataStorePreferencesRepository
import com.gabriel.mylibrary.data.repository.LibraryRepository
import com.gabriel.mylibrary.data.repository.PreferencesRepository
import com.gabriel.mylibrary.data.repository.RoomLibraryRepository

class LibraryApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val coverStore by lazy { CoverStore(context.applicationContext) }
    private val database by lazy {
        Room.databaseBuilder(context.applicationContext, LibraryDatabase::class.java, "my-library.db")
            .addMigrations(LibraryDatabase.MIGRATION_1_2)
            .addCallback(LibraryDatabase.CREATE_DEFAULTS)
            .build()
    }
    val repository: LibraryRepository by lazy { RoomLibraryRepository(database, coverStore) }
    val preferences: PreferencesRepository by lazy { DataStorePreferencesRepository(context.applicationContext) }
}

package com.gabriel.mylibrary.data.local

import androidx.room.TypeConverter
import com.gabriel.mylibrary.model.CoverType

class LibraryConverters {
    @TypeConverter
    fun coverTypeToString(type: CoverType): String = type.name

    @TypeConverter
    fun stringToCoverType(value: String): CoverType =
        CoverType.entries.firstOrNull { it.name == value } ?: CoverType.NONE
}

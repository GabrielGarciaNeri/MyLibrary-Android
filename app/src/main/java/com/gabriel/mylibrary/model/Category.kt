package com.gabriel.mylibrary.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "categories", indices = [Index(value = ["normalizedName"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Long = 0xFF2F6F9FL,
    val createdAt: Long = System.currentTimeMillis(),
    val sortPosition: Int = 0,
    val defaultKey: String? = null,
    val normalizedName: String = name.trim().lowercase(Locale.ROOT),
) {
    val displayName: String get() = name

    val subcategories: List<String>
        get() = when (defaultKey) {
            "BOOK" -> listOf("General", "Romance", "Fantasy", "Adventure", "Sci-Fi")
            "MANGA" -> listOf("General", "Shonen", "Seinen", "Slice of Life")
            "MANHWA" -> listOf("General", "Action", "Regression", "Tower")
            "MANHUA" -> listOf("General", "Cultivation", "Action")
            "COMIC" -> listOf("General", "Superhero", "Indie")
            else -> listOf("General")
        }

    companion object {
        const val BOOK_ID = 1L
        const val UNCATEGORIZED_ID = 6L
        const val MAX_NAME_LENGTH = 40

        fun defaults(createdAt: Long): List<Category> = listOf(
            Category(1, "Book", 0xFF2F6F9FL, createdAt, 0, "BOOK"),
            Category(2, "Manga", 0xFF7652A4L, createdAt, 1, "MANGA"),
            Category(3, "Manhwa", 0xFF287C78L, createdAt, 2, "MANHWA"),
            Category(4, "Manhua", 0xFFA76024L, createdAt, 3, "MANHUA"),
            Category(5, "Comic", 0xFFAB4863L, createdAt, 4, "COMIC"),
            Category(6, "Uncategorized", 0xFF5C6776L, createdAt, 5, "UNCATEGORIZED"),
        )
    }
}

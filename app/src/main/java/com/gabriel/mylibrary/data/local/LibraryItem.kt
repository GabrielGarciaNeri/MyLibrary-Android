package com.gabriel.mylibrary.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gabriel.mylibrary.model.Category
import com.gabriel.mylibrary.model.CoverType

@Entity(
    tableName = "library_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["categoryId"])],
)
data class LibraryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "",
    val description: String = "",
    val categoryId: Long = Category.BOOK_ID,
    val subcategory: String = "General",
    val currentProgress: Int = 0,
    val totalCount: Int = 0,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val coverType: CoverType = CoverType.NONE,
    val coverPresetId: String? = null,
    val coverFileName: String? = null,
) {
    val progressFraction: Float
        get() = if (totalCount > 0) {
            (currentProgress.toFloat() / totalCount).coerceIn(0f, 1f)
        } else {
            0f
        }
}

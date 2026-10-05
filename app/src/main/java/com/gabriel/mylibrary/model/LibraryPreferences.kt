package com.gabriel.mylibrary.model

enum class SortOrder(val displayName: String) {
    TITLE_ASC("Title A–Z"),
    TITLE_DESC("Title Z–A"),
    AUTHOR_ASC("Author A–Z"),
    AUTHOR_DESC("Author Z–A"),
    PROGRESS_ASC("Progress Low–High"),
    PROGRESS_DESC("Progress High–Low"),
}

enum class LayoutMode { GRID, LIST }

data class LibraryPreferences(
    val layout: LayoutMode = LayoutMode.GRID,
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
)

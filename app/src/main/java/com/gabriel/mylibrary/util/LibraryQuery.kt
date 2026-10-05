package com.gabriel.mylibrary.util

import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.model.SortOrder
import java.util.Locale

fun filterAndSortItems(
    items: List<LibraryItem>,
    query: String = "",
    category: Long? = null,
    favoritesOnly: Boolean = false,
    sortOrder: SortOrder = SortOrder.TITLE_ASC,
): List<LibraryItem> {
    val search = query.trim()
    val filtered = items.filter { item ->
        (!favoritesOnly || item.isFavorite) &&
            (category == null || item.categoryId == category) &&
            (search.isEmpty() || item.title.contains(search, ignoreCase = true) || item.author.contains(search, ignoreCase = true))
    }
    val byTitle = compareBy<LibraryItem> { it.title.lowercase(Locale.ROOT) }
        .thenBy { it.author.lowercase(Locale.ROOT) }
    val byAuthor = compareBy<LibraryItem> { it.author.lowercase(Locale.ROOT) }
        .thenBy { it.title.lowercase(Locale.ROOT) }
    val byProgress = Comparator<LibraryItem> { left, right ->
        val leftCurrent = if (left.totalCount > 0) left.currentProgress.toLong() else 0L
        val rightCurrent = if (right.totalCount > 0) right.currentProgress.toLong() else 0L
        val leftTotal = left.totalCount.coerceAtLeast(1).toLong()
        val rightTotal = right.totalCount.coerceAtLeast(1).toLong()
        (leftCurrent * rightTotal).compareTo(rightCurrent * leftTotal)
    }
    val comparator = when (sortOrder) {
        SortOrder.TITLE_ASC -> byTitle
        SortOrder.TITLE_DESC -> byTitle.reversed()
        SortOrder.AUTHOR_ASC -> byAuthor
        SortOrder.AUTHOR_DESC -> byAuthor.reversed()
        SortOrder.PROGRESS_ASC -> byProgress.then(byTitle)
        SortOrder.PROGRESS_DESC -> byProgress.reversed().then(byTitle)
    }.thenBy { it.id }
    return filtered.sortedWith(comparator)
}

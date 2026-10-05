package com.gabriel.mylibrary.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.model.SortOrder

@StringRes fun SortOrder.labelResource(): Int = when (this) {
    SortOrder.TITLE_ASC -> R.string.sort_title_asc
    SortOrder.TITLE_DESC -> R.string.sort_title_desc
    SortOrder.AUTHOR_ASC -> R.string.sort_author_asc
    SortOrder.AUTHOR_DESC -> R.string.sort_author_desc
    SortOrder.PROGRESS_ASC -> R.string.sort_progress_asc
    SortOrder.PROGRESS_DESC -> R.string.sort_progress_desc
}

@Composable
fun FavoriteButton(item: LibraryItem, onClick: () -> Unit) {
    IconButton(onClick, Modifier.testTag("favorite_${item.id}")) {
        Icon(if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
            stringResource(if (item.isFavorite) R.string.remove_favorite else R.string.add_favorite, item.title),
            tint = if (item.isFavorite) Color(0xFFC38A00) else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProgressDisplay(item: LibraryItem) {
    val description = stringResource(R.string.progress_accessibility, item.currentProgress, item.totalCount)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { item.progressFraction }, modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {}
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.weight(1f, fill = false), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                Text(stringResource(R.string.progress_count, item.currentProgress, item.totalCount),
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp).semantics { contentDescription = description },
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

        }
    }
}

@Composable
fun LibraryCard(item: LibraryItem, onOpen: () -> Unit, onFavorite: () -> Unit,
                onProgress: () -> Unit, onDelete: () -> Unit)  {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("item_${item.id}")
            .combinedClickable(onClick = onOpen, onLongClick = onDelete,
                onLongClickLabel = stringResource(R.string.delete_accessibility, item.title)),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                ItemCover(item, Modifier.height(126.dp).aspectRatio(2f / 3f).align(Alignment.Center))
                Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)) { FavoriteButton(item, onFavorite) }
            }
            Spacer(Modifier.height(12.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(item.author.ifBlank { stringResource(R.string.author_unknown) },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            ProgressDisplay(item)
            TextButton(onProgress, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.quick_progress))
            }
        }
    }
}

@Composable
fun LibraryListRow(item: LibraryItem, onOpen: () -> Unit, onFavorite: () -> Unit,
                   onAdjust: (Int) -> Unit, onProgress: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("item_${item.id}")
            .combinedClickable(onClick = onOpen, onLongClick = onDelete,
                onLongClickLabel = stringResource(R.string.delete_accessibility, item.title)),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemCover(item, Modifier.width(48.dp).aspectRatio(2f / 3f))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.author.ifBlank { stringResource(R.string.author_unknown) },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FavoriteButton(item, onFavorite)
            }
            Spacer(Modifier.height(12.dp))
            ProgressDisplay(item)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.3f) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf(1, 5, 10).forEach { delta ->
                            TextButton({ onAdjust(delta) }, Modifier.weight(1f).heightIn(min = 48.dp),
                                enabled = item.currentProgress < item.totalCount) {
                                Text(stringResource(R.string.progress_plus, delta))
                            }
                        }
                        TextButton(onProgress, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.progress_exact)) }
                    }
                } else {
                    TextButton(onProgress, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.quick_progress)) }
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier,
               favorite: Boolean = false, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(if (favorite) Icons.Default.StarBorder else Icons.AutoMirrored.Filled.MenuBook, null,
                Modifier.padding(22.dp).size(44.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onAction != null && actionLabel != null) Button(onAction) { Text(actionLabel) }
    }
}

@Composable
fun DeleteConfirmation(item: LibraryItem, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message, item.title)) },
        confirmButton = { TextButton(onDelete, Modifier.testTag("delete_confirm")) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("delete_cancel")) { Text(stringResource(R.string.cancel)) } })
}

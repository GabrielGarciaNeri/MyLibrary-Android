package com.gabriel.mylibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.model.Category
import com.gabriel.mylibrary.ui.components.*

@Composable
fun CategoriesScreen(state: LibraryUiState, onCategory: (Category) -> Unit, viewModel: LibraryViewModel) {
    var editorId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.nav_categories), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.categories_subtitle), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton({ editorId = 0 }, enabled = !state.isLoading && !state.loadFailed) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_category))
            }
            Spacer(Modifier.height(6.dp))
        }
        if (state.loadFailed) item {
            EmptyState(stringResource(R.string.load_error_title), stringResource(R.string.load_error_body),
                actionLabel = stringResource(R.string.retry), onAction = viewModel::retry)
        } else if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else items(state.categories, key = { it.id }) { category ->
            var menu by remember { mutableStateOf(false) }
            val count = state.items.count { it.categoryId == category.id }
            Surface(shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().clickable { onCategory(category) }.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(42.dp).background(Color(category.colorArgb), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = categoryForeground(Color(category.colorArgb)))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text(pluralStringResource(R.plurals.title_count, count, count), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.manage_category, category.name)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.edit_category)) }, onClick = { editorId = category.id; menu = false })
                            if (category.id != Category.UNCATEGORIZED_ID) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.delete_category)) }, onClick = { deleteId = category.id; menu = false })
                            }
                        }
                    }
                }
            }
        }
    }
    if (editorId != null) {
        val existing = state.categories.find { it.id == editorId }
        if (editorId == 0L || existing != null) CategoryEditor(existing, state.categories, viewModel::saveCategory) { editorId = null }
    }
    state.categories.find { it.id == deleteId }?.let { category ->
        CategoryDeleteDialog(category, state.items.count { it.categoryId == category.id }, state.categories,
            { replacement, onResult -> viewModel.deleteCategory(category.id, replacement, onResult) }, { deleteId = null })
    }
}

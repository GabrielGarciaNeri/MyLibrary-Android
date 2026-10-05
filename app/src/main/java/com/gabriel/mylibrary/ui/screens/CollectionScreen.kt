package com.gabriel.mylibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.model.*
import com.gabriel.mylibrary.ui.components.*
import com.gabriel.mylibrary.util.filterAndSortItems
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(state: LibraryUiState, favoritesOnly: Boolean, viewModel: LibraryViewModel,
                     onOpen: (Long) -> Unit, onAdd: () -> Unit, fixedCategoryId: Long? = null,
                     onBack: (() -> Unit)? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var progressId by rememberSaveable { mutableStateOf<Long?>(null) }
    val fixedCategory = state.categories.find { it.id == fixedCategoryId }
    val category = fixedCategoryId ?: selectedCategory?.takeIf { id -> state.categories.any { it.id == id } }
    val items = remember(state.items, query, category, favoritesOnly, state.preferences.sortOrder) {
        filterAndSortItems(state.items, query, category, favoritesOnly, state.preferences.sortOrder)
    }
    val title = fixedCategory?.name
        ?: stringResource(if (favoritesOnly) R.string.nav_favorites else R.string.app_name)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp) {
                Row(Modifier.padding(horizontal = 4.dp)) {
                    Box {
                        IconButton({ showSort = true }, Modifier.testTag("sort_menu")) {
                            Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_items))
                        }
                        DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                            SortOrder.entries.forEach { sort ->
                                DropdownMenuItem(text = { Text(stringResource(sort.labelResource())) },
                                    modifier = Modifier.testTag("sort_${sort.name}").semantics { selected = sort == state.preferences.sortOrder },
                                    leadingIcon = { if (sort == state.preferences.sortOrder) Icon(Icons.Default.Check, null)
                                        else Spacer(Modifier.size(24.dp)) },
                                    onClick = { viewModel.setSortOrder(sort); showSort = false })
                            }
                        }
                    }
                    IconButton({ viewModel.setLayout(if (state.preferences.layout == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID) },
                        Modifier.testTag("layout_toggle")) {
                        Icon(if (state.preferences.layout == LayoutMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            stringResource(if (state.preferences.layout == LayoutMode.GRID) R.string.show_list else R.string.show_grid))
                    }
                    if (!favoritesOnly) IconButton(onAdd, Modifier.testTag("add_item")) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_item))
                    }
                    IconButton({ showSearch = !showSearch; if (!showSearch) query = "" }, Modifier.testTag("search_toggle")) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            stringResource(if (showSearch) R.string.close_search else R.string.search_library))
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(if (fixedCategory != null) pluralStringResource(R.plurals.title_count, items.size, items.size)
                else stringResource(if (favoritesOnly) R.string.favorites_subtitle else R.string.library_subtitle),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showSearch) {
            OutlinedTextField(value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("search_field"),
                label = { Text(stringResource(R.string.search_hint)) }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) {
                    Icon(Icons.Default.Clear, stringResource(R.string.clear_search)) } }, shape = RoundedCornerShape(20.dp))
            Spacer(Modifier.height(8.dp))
        }
        if (fixedCategoryId == null) {
            LazyRow(modifier = Modifier.testTag("category_chips"), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = category == null, onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.all_categories)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary), shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("category_ALL")) }
                items(state.categories, key = { it.id }) { option ->
                    FilterChip(selected = category == option.id, onClick = { selectedCategory = option.id },
                        label = { Text(option.name) },
                        colors = categoryChipColors(option), shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("category_${option.id}"))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val loading = stringResource(R.string.loading)
                CircularProgressIndicator(Modifier.semantics { contentDescription = loading })
            }
            state.loadFailed -> EmptyState(stringResource(R.string.load_error_title), stringResource(R.string.load_error_body),
                modifier = Modifier.verticalScroll(rememberScrollState()), actionLabel = stringResource(R.string.retry), onAction = viewModel::retry)
            fixedCategoryId != null && fixedCategory == null -> EmptyState(stringResource(R.string.category_missing),
                stringResource(R.string.category_missing_body), actionLabel = stringResource(R.string.back), onAction = onBack)
            items.isEmpty() -> {
                val filtered = query.isNotBlank() || category != null
                EmptyState(
                    title = stringResource(if (filtered) R.string.empty_results_title else if (favoritesOnly) R.string.empty_favorites_title else R.string.empty_library_title),
                    body = stringResource(if (fixedCategory != null && query.isBlank()) R.string.empty_category_body else if (filtered) R.string.empty_results_body else if (favoritesOnly) R.string.empty_favorites_body else R.string.empty_library_body),
                    modifier = Modifier.testTag("collection_empty").verticalScroll(rememberScrollState()), favorite = favoritesOnly,
                    actionLabel = if (filtered && fixedCategory == null) stringResource(R.string.clear_filters) else if (!favoritesOnly) stringResource(R.string.add_item) else null,
                    onAction = if (filtered && fixedCategory == null) ({ query = ""; selectedCategory = null }) else if (!favoritesOnly) onAdd else null)
            }
            state.preferences.layout == LayoutMode.GRID -> {
                val minWidth = if (LocalDensity.current.fontScale > 1.3f) 210.dp else 156.dp
                LazyVerticalGrid(columns = GridCells.Adaptive(minWidth),
                    modifier = Modifier.weight(1f).testTag("collection_grid"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(items, key = { it.id }) { item ->
                        LibraryCard(item, { onOpen(item.id) }, { viewModel.toggleFavorite(item.id) },
                            { progressId = item.id }, { deleteId = item.id })
                    }
                }
            }
            else -> LazyColumn(modifier = Modifier.weight(1f).testTag("collection_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState) {
                        snapshotFlow { dismissState.settledValue }.collect { settled ->
                            if (settled == SwipeToDismissBoxValue.EndToStart) {
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                deleteId = item.id
                            }
                        }
                    }
                    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp)).padding(24.dp),
                                contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, stringResource(R.string.delete_accessibility, item.title), tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }) {
                        LibraryListRow(item, { onOpen(item.id) }, { viewModel.toggleFavorite(item.id) },
                            { delta -> viewModel.adjustProgress(item.id, delta) }, { progressId = item.id }, { deleteId = item.id })
                    }
                }
            }
        }
    }
    state.items.find { it.id == progressId }?.let { item ->
        QuickProgressDialog(item, { viewModel.adjustProgress(item.id, it) },
            { viewModel.exactProgress(item.id, it); progressId = null }, { progressId = null })
    }
    state.items.find { it.id == deleteId }?.let { item ->
        DeleteConfirmation(item, { viewModel.delete(item.id); deleteId = null }, { deleteId = null })
    }
}

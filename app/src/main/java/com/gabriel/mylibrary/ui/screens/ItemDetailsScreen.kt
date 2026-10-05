package com.gabriel.mylibrary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.data.local.LibraryItem
import com.gabriel.mylibrary.ui.components.ItemCover
import com.gabriel.mylibrary.util.validateItem
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(viewModel: ItemDetailsViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showProgressEditor by rememberSaveable { mutableStateOf(false) }
    var handledProgressUpdate by rememberSaveable { mutableIntStateOf(state.progressUpdated) }
    var handledCompletion by rememberSaveable { mutableIntStateOf(state.completionCount) }
    val snackbarHost = remember { SnackbarHostState() }
    val completionMessage = stringResource(R.string.upgrade_item_completed)
    LaunchedEffect(state.completionCount) {
        if (state.completionCount > handledCompletion) {
            handledCompletion = state.completionCount
            snackbarHost.showSnackbar(completionMessage)
        }
    }
    val item = state.item
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.progressUpdated) {
        if (state.progressUpdated > handledProgressUpdate) {
            showProgressEditor = false
            handledProgressUpdate = state.progressUpdated
        }
    }
    BackHandler(enabled = state.isWorking) { }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.item_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isWorking, modifier = Modifier.testTag("details_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.item_back))
                    }
                },
                actions = {
                    if (item != null) {
                        IconButton(onClick = { onEdit(item.id) }, enabled = !state.isWorking, modifier = Modifier.testTag("details_edit")) {
                            Icon(Icons.Default.Edit, stringResource(R.string.item_edit_title))
                        }
                    }
                },
            )
        },
    ) { insets ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed || item == null -> ItemLoadFailure(
                modifier = Modifier.fillMaxSize().padding(insets),
                missing = !state.loadFailed,
                onRetry = viewModel::retryLoad,
                onBack = onBack,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets).testTag("details_content"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ItemCover(item, Modifier.width(180.dp).aspectRatio(2f / 3f))
                        Text(item.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.testTag("details_title"))
                        Text(item.author.ifBlank { stringResource(R.string.item_author_empty) }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        FilledTonalButton(onClick = viewModel::toggleFavorite, enabled = !state.isWorking, modifier = Modifier.testTag("details_favorite")) {
                            Icon(if (item.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(if (item.isFavorite) R.string.item_remove_favorite else R.string.item_add_favorite))
                        }
                    }
                }
                if (state.actionFailed) item { ItemErrorCard(stringResource(R.string.item_action_error)) }
                item {
                    ItemSection(stringResource(R.string.item_progress)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.item_progress_counts, item.currentProgress, item.totalCount), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text(NumberFormat.getPercentInstance().format(item.progressFraction.toDouble()), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp))
                        }
                        LinearProgressIndicator(progress = { item.progressFraction }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        listOf(listOf(-10, -5, -1), listOf(1, 5, 10)).forEach { deltas ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                deltas.forEach { delta ->
                                    val amount = kotlin.math.abs(delta)
                                    val description = stringResource(if (delta < 0) R.string.upgrade_item_decrease else R.string.upgrade_item_increase, amount)
                                    OutlinedButton(
                                        onClick = { viewModel.adjustProgress(delta) },
                                        enabled = !state.isWorking && if (delta < 0) item.currentProgress > 0 else item.currentProgress < item.totalCount,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                            .testTag("details_progress_$delta").semantics { contentDescription = description },
                                    ) {
                                        Text(stringResource(if (delta < 0) R.string.upgrade_item_decrease_label else R.string.upgrade_item_increase_label, amount))
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = { viewModel.clearError(); showProgressEditor = true },
                            enabled = !state.isWorking,
                            modifier = Modifier.fillMaxWidth().testTag("details_update_progress"),
                        ) { Text(stringResource(R.string.item_update_progress)) }
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_description)) {
                        Text(item.description.ifBlank { stringResource(R.string.item_description_empty) }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_about)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.category?.let { category ->
                                Box(Modifier.size(18.dp).background(Color(category.colorArgb.toInt()), CircleShape))
                            }
                            ItemMetadata(stringResource(R.string.item_category), state.category?.name ?: stringResource(R.string.upgrade_item_uncategorized))
                        }
                        ItemMetadata(stringResource(R.string.item_subcategory), item.subcategory)
                        ItemMetadata(stringResource(R.string.item_favorite), stringResource(if (item.isFavorite) R.string.item_favorite_yes else R.string.item_favorite_no))
                        ItemMetadata(stringResource(R.string.item_created), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.createdAt)))
                        ItemMetadata(stringResource(R.string.item_updated), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.updatedAt)))
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.clearError(); confirmDelete = true },
                        enabled = !state.isWorking,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("details_delete"),
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.item_delete))
                    }
                }
            }
        }
    }

    if (confirmDelete && item != null) {
        AlertDialog(
            onDismissRequest = { if (!state.isWorking) confirmDelete = false },
            title = { Text(stringResource(R.string.item_delete_title)) },
            text = {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.item_delete_message, item.title))
                    if (state.actionFailed) Text(stringResource(R.string.item_action_error), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::delete, enabled = !state.isWorking, modifier = Modifier.testTag("delete_confirm")) {
                    Text(stringResource(if (state.isWorking) R.string.item_deleting else R.string.item_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }, enabled = !state.isWorking, modifier = Modifier.testTag("delete_cancel")) {
                    Text(stringResource(R.string.item_cancel))
                }
            },
        )
    }
    if (showProgressEditor && item != null) {
        ItemProgressDialog(
            item = item,
            working = state.isWorking,
            failed = state.actionFailed,
            onSave = viewModel::updateProgress,
            onDismiss = { if (!state.isWorking) showProgressEditor = false },
        )
    }
}

@Composable
private fun ItemMetadata(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ItemProgressDialog(item: LibraryItem, working: Boolean, failed: Boolean, onSave: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var current by rememberSaveable { mutableStateOf(item.currentProgress.toString()) }
    var total by rememberSaveable { mutableStateOf(item.totalCount.toString()) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val validation = validateItem(item.title, current, total)
    val currentError = itemValidationMessage(validation.currentErrorKind).takeIf { attempted }
    val totalError = itemValidationMessage(validation.totalErrorKind).takeIf { attempted }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.item_update_progress)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (failed) Text(stringResource(R.string.item_action_error), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    modifier = Modifier.fillMaxWidth().testTag("progress_current"),
                    label = { Text(stringResource(R.string.item_current_label)) },
                    enabled = !working, singleLine = true, isError = currentError != null,
                    supportingText = currentError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = total, onValueChange = { total = it },
                    modifier = Modifier.fillMaxWidth().testTag("progress_total"),
                    label = { Text(stringResource(R.string.item_total_label)) },
                    enabled = !working, singleLine = true, isError = totalError != null,
                    supportingText = totalError?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attempted = true
                    if (validation.isValid) onSave(current.trim().toInt(), total.trim().toInt())
                },
                enabled = !working, modifier = Modifier.testTag("progress_save"),
            ) { Text(stringResource(if (working) R.string.item_saving else R.string.item_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.item_cancel)) } },
    )
}

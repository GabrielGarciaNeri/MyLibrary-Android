package com.gabriel.mylibrary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.model.CoverType
import com.gabriel.mylibrary.ui.components.ItemCover
import com.gabriel.mylibrary.ui.components.CoverGalleryDialog
import com.gabriel.mylibrary.util.ValidationError
import com.gabriel.mylibrary.util.validateItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(viewModel: ItemFormViewModel, onDone: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var chooser by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = state.draft
    val selectedCategory = state.categories.firstOrNull { it.id == draft.categoryId }
    val busy = state.isSaving || state.isImportingCover
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.importCover(uri)
    }
    val validation = validateItem(draft.title, draft.currentProgress, draft.totalCount)
    val titleError = itemValidationMessage(validation.titleErrorKind).takeIf { state.showValidation }
    val currentError = itemValidationMessage(validation.currentErrorKind).takeIf { state.showValidation }
    val totalError = itemValidationMessage(validation.totalErrorKind).takeIf { state.showValidation }
    val leave = {
        if (!busy) {
            if (state.isDirty) confirmDiscard = true else viewModel.discard()
        }
    }
    BackHandler { leave() }
    LaunchedEffect(state.saved, state.discarded) { if (state.saved || state.discarded) onDone() }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(if (state.isEditing) R.string.item_edit_title else R.string.item_add_title)) },
                    navigationIcon = {
                        IconButton(onClick = leave, enabled = !busy, modifier = Modifier.testTag("form_back")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.item_back))
                        }
                    },
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = leave, enabled = !busy, modifier = Modifier.testTag("form_cancel")) {
                        Text(stringResource(R.string.item_cancel))
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isLoading && !busy && state.loadError == null && !state.categoriesLoading && !state.categoriesFailed && state.categories.isNotEmpty(),
                        modifier = Modifier.testTag("form_save"),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(stringResource(if (state.isSaving) R.string.item_saving else R.string.item_save))
                    }
                }
            }
        },
    ) { insets ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null -> ItemLoadFailure(
                modifier = Modifier.fillMaxSize().padding(insets),
                missing = state.loadError == FormLoadError.NOT_FOUND,
                onRetry = viewModel::retryLoad,
                onBack = onDone,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets).testTag("form_content"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (state.saveFailed) item {
                    ItemErrorCard(stringResource(R.string.item_save_error))
                }
                if (state.categoriesFailed) item {
                    ItemErrorCard(stringResource(R.string.upgrade_item_categories_error))
                    TextButton(onClick = viewModel::observeCategories) { Text(stringResource(R.string.item_retry)) }
                }
                item {
                    ItemSection(stringResource(R.string.item_basic_info)) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(title = value) } },
                            modifier = Modifier.fillMaxWidth().testTag("form_title"),
                            label = { Text(stringResource(R.string.item_title_label)) },
                            singleLine = true,
                            enabled = !busy,
                            isError = titleError != null,
                            supportingText = titleError?.let { message -> { Text(message) } },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp),
                        )
                        OutlinedTextField(
                            value = draft.author,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(author = value) } },
                            modifier = Modifier.fillMaxWidth().testTag("form_author"),
                            label = { Text(stringResource(R.string.item_author_label)) },
                            singleLine = true,
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
                item {
                    ItemSection(stringResource(R.string.upgrade_item_cover)) {
                        ItemCover(
                            coverType = draft.coverType,
                            presetId = draft.coverPresetId,
                            fileName = draft.coverFileName,
                            modifier = Modifier.width(132.dp).aspectRatio(2f / 3f).align(Alignment.CenterHorizontally),
                            contentDescription = stringResource(R.string.upgrade_item_cover_preview),
                        )
                        if (state.isImportingCover) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                                Text(stringResource(R.string.upgrade_item_cover_importing))
                            }
                        }
                        if (state.coverImportFailed) ItemErrorCard(stringResource(R.string.upgrade_item_cover_error))
                        OutlinedButton(onClick = { chooser = "cover" }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.upgrade_item_cover_gallery))
                        }
                        OutlinedButton(onClick = viewModel::randomCover, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.upgrade_item_cover_random))
                        }
                        OutlinedButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.upgrade_item_cover_device)) }
                        if (draft.coverType != CoverType.NONE) {
                            TextButton(onClick = viewModel::removeCover, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.upgrade_item_cover_remove))
                            }
                        }
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_description)) {
                        OutlinedTextField(
                            value = draft.description,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(description = value) } },
                            modifier = Modifier.fillMaxWidth().testTag("form_description"),
                            label = { Text(stringResource(R.string.item_description_label)) },
                            minLines = 3,
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_progress)) {
                        OutlinedTextField(
                            value = draft.currentProgress,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(currentProgress = value) } },
                            modifier = Modifier.fillMaxWidth().testTag("form_current"),
                            label = { Text(stringResource(R.string.item_current_label)) },
                            singleLine = true,
                            enabled = !busy,
                            isError = currentError != null,
                            supportingText = currentError?.let { message -> { Text(message) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(16.dp),
                        )
                        OutlinedTextField(
                            value = draft.totalCount,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(totalCount = value) } },
                            modifier = Modifier.fillMaxWidth().testTag("form_total"),
                            label = { Text(stringResource(R.string.item_total_label)) },
                            singleLine = true,
                            enabled = !busy,
                            isError = totalError != null,
                            supportingText = totalError?.let { message -> { Text(message) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(16.dp),
                        )
                        Text(stringResource(R.string.item_progress_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_favorite)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .toggleable(
                                    value = draft.isFavorite,
                                    enabled = !busy,
                                    role = Role.Switch,
                                    onValueChange = { value -> viewModel.updateDraft { it.copy(isFavorite = value) } },
                                ).testTag("form_favorite"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.item_mark_favorite), modifier = Modifier.weight(1f))
                            Switch(checked = draft.isFavorite, onCheckedChange = null, enabled = !busy)
                        }
                    }
                }
                item {
                    ItemSection(stringResource(R.string.item_category)) {
                        ItemSelectionButton(
                            label = stringResource(R.string.item_category), value = selectedCategory?.name ?: stringResource(R.string.upgrade_item_category_loading),
                            enabled = !busy && !state.categoriesLoading && !state.categoriesFailed, modifier = Modifier.testTag("form_category"),
                            onClick = { chooser = "category" },
                        )
                        ItemSelectionButton(
                            label = stringResource(R.string.item_subcategory), value = draft.subcategory,
                            enabled = !busy && selectedCategory != null, modifier = Modifier.testTag("form_subcategory"),
                            onClick = { chooser = "subcategory" },
                        )
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.item_discard_title)) },
            text = { Text(stringResource(R.string.item_discard_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.discard() }, modifier = Modifier.testTag("discard_confirm")) {
                    Text(stringResource(R.string.item_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }, modifier = Modifier.testTag("discard_cancel")) { Text(stringResource(R.string.item_keep_editing)) }
            },
        )
    }
    if (chooser == "category") {
        ItemChoiceDialog(
            title = stringResource(R.string.item_category),
            choices = state.categories.map { it.id.toString() to it.name },
            selected = draft.categoryId.toString(),
            colors = state.categories.associate { it.id.toString() to it.colorArgb },
            tagPrefix = "form_category_",
            onSelect = { value -> state.categories.firstOrNull { it.id.toString() == value }?.let(viewModel::selectCategory); chooser = null },
            onDismiss = { chooser = null },
        )
    } else if (chooser == "cover") {
        CoverGalleryDialog(
            selectedPresetId = draft.coverPresetId,
            onSelect = { viewModel.selectBuiltInCover(it); chooser = null },
            onDismiss = { chooser = null },
        )
    } else if (chooser == "subcategory") {
        ItemChoiceDialog(
            title = stringResource(R.string.item_subcategory),
            choices = ((selectedCategory?.subcategories ?: listOf("General")) + draft.subcategory).distinct().map { it to it },
            selected = draft.subcategory,
            tagPrefix = "form_subcategory_",
            onSelect = { value -> viewModel.updateDraft { it.copy(subcategory = value) }; chooser = null },
            onDismiss = { chooser = null },
        )
    }
}

@Composable
internal fun ItemSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun ItemSelectionButton(label: String, value: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Default.ExpandMore, contentDescription = null)
    }
}

@Composable
private fun ItemChoiceDialog(title: String, choices: List<Pair<String, String>>, selected: String, tagPrefix: String, colors: Map<String, Long> = emptyMap(), onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                choices.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton)
                            .testTag(tagPrefix + value).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        colors[value]?.let { argb ->
                            Box(Modifier.padding(start = 8.dp).size(16.dp).background(Color(argb.toInt()), CircleShape))
                        }
                        Text(label, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.item_done)) } },
    )
}

@Composable
internal fun itemValidationMessage(error: ValidationError?): String? = when (error) {
    ValidationError.REQUIRED -> stringResource(R.string.item_required_error)
    ValidationError.INVALID_NUMBER -> stringResource(R.string.item_integer_error)
    ValidationError.NEGATIVE -> stringResource(R.string.item_negative_error)
    ValidationError.TOO_LARGE -> stringResource(R.string.item_number_too_large_error)
    ValidationError.EXCEEDS_TOTAL -> stringResource(R.string.item_exceeds_total_error)
    null -> null
}

@Composable
internal fun ItemErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
internal fun ItemLoadFailure(modifier: Modifier, missing: Boolean, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(if (missing) R.string.item_missing_title else R.string.item_load_error_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(if (missing) R.string.item_missing_message else R.string.item_load_error_message))
        Spacer(Modifier.height(20.dp))
        if (!missing) Button(onClick = onRetry) { Text(stringResource(R.string.item_retry)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.item_go_back)) }
    }
}

package com.gabriel.mylibrary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.model.Category
import java.util.Locale

val categoryPalette = listOf(0xFF2F6F9FL, 0xFF426B32L, 0xFF984D23L, 0xFF7146A0L,
    0xFFAF355FL, 0xFF00796BL, 0xFF475569L, 0xFF9A6700L, 0xFF2848A8L, 0xFF973D35L,
    0xFF556B2FL, 0xFF80573BL, 0xFF80CBC4L, 0xFFFFCC80L, 0xFFCE93D8L, 0xFF90CAF9L)
fun categoryForeground(color: Color): Color = if (color.luminance() > 0.179f) Color.Black else Color.White

@Composable
fun categoryChipColors(category: Category): SelectableChipColors {
    val color = Color(category.colorArgb)
    return FilterChipDefaults.filterChipColors(
        containerColor = color.copy(alpha = 0.12f),
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = color,
        selectedLabelColor = categoryForeground(color))
}

@Composable
fun CategoryEditor(category: Category?, categories: List<Category>, onSave: (Category, (String?) -> Unit) -> Unit,
                   onDismiss: () -> Unit) {
    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var color by rememberSaveable(category?.id) { mutableLongStateOf(category?.colorArgb ?: categoryPalette.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val blank = stringResource(R.string.category_name_required)
    val duplicate = stringResource(R.string.category_name_duplicate)
    val tooLong = stringResource(R.string.category_name_length)
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(if (category == null) R.string.add_category else R.string.edit_category)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(name, { name = it; error = null }, label = { Text(stringResource(R.string.category_name)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, isError = error != null,
                    enabled = !saving && category?.id != Category.UNCATEGORIZED_ID,
                    supportingText = { if (error != null) Text(error!!) else Text(stringResource(R.string.category_name_length)) })
                Surface(color = Color(color), contentColor = categoryForeground(Color(color)), shape = MaterialTheme.shapes.medium) {
                    Text(name.trim().ifBlank { stringResource(R.string.category_color_preview) }, Modifier.padding(14.dp))
                }
                Text(stringResource(R.string.category_color), style = MaterialTheme.typography.titleSmall)
                categoryPalette.chunked(4).forEachIndexed { row, choices ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        choices.forEachIndexed { column, option ->
                            val label = stringResource(R.string.category_palette_option, row * 4 + column + 1)
                            IconButton({ color = option }, enabled = !saving,
                                modifier = Modifier.size(48.dp).semantics { contentDescription = label }) {
                                Box(Modifier.size(36.dp).background(Color(option), CircleShape), contentAlignment = Alignment.Center) {
                                    if (color == option) Icon(Icons.Default.Check, stringResource(R.string.category_selected_color),
                                        tint = categoryForeground(Color(option)))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            val trimmed = name.trim()
            error = when {
                trimmed.isBlank() -> blank
                trimmed.length > 40 -> tooLong
                categories.any { it.id != category?.id && it.name.lowercase(Locale.ROOT) == trimmed.lowercase(Locale.ROOT) } -> duplicate
                else -> null
            }
            if (error == null) {
                saving = true
                onSave((category ?: Category(name = trimmed)).copy(name = trimmed, colorArgb = color)) { result ->
                    saving = false; error = result; if (result == null) onDismiss()
                }
            }
        }) { Text(stringResource(if (saving) R.string.item_saving else R.string.item_save)) } },
        dismissButton = { TextButton(onDismiss, enabled = !saving) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun CategoryDeleteDialog(category: Category, count: Int, categories: List<Category>,
                         onDelete: (Long, (Boolean) -> Unit) -> Unit, onDismiss: () -> Unit) {
    var replacement by rememberSaveable(category.id) { mutableLongStateOf(Category.UNCATEGORIZED_ID) }
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val options = categories.filter { it.id != category.id }
    val target = options.find { it.id == replacement } ?: options.find { it.id == Category.UNCATEGORIZED_ID }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.delete_category_title, category.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (count == 0) stringResource(R.string.delete_category_empty)
                    else pluralStringResource(R.plurals.delete_category_populated, count, count))
                if (count > 0) {
                    Text(stringResource(R.string.move_items_to))
                    Box {
                        OutlinedButton({ expanded = true }, enabled = !busy) { Text(target?.name.orEmpty()) }
                        DropdownMenu(expanded, { expanded = false }) {
                            options.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = {
                                replacement = option.id; expanded = false
                            }) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy && target != null, onClick = {
            busy = true; onDelete(target!!.id) { success -> busy = false; if (success) onDismiss() }
        }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
}

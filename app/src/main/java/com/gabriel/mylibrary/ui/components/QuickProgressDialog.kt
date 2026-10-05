package com.gabriel.mylibrary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gabriel.mylibrary.R
import com.gabriel.mylibrary.data.local.LibraryItem

@Composable
fun QuickProgressDialog(item: LibraryItem, onAdjust: (Int) -> Unit, onExact: (Int) -> Unit, onDismiss: () -> Unit) {
    var current by remember(item.currentProgress) { mutableStateOf(item.currentProgress.toString()) }
    var submitted by remember { mutableStateOf(false) }
    val number = current.toIntOrNull()
    val valid = number != null && number in 0..item.totalCount
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_progress)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                ProgressDisplay(item)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 5, 10).forEach { delta ->
                        FilledTonalButton({ onAdjust(delta) }, Modifier.weight(1f).heightIn(min = 48.dp),
                            contentPadding = PaddingValues(4.dp), enabled = item.currentProgress < item.totalCount) {
                            Text(stringResource(R.string.progress_plus, delta))
                        }
                    }
                }
                OutlinedTextField(current, { current = it; submitted = false }, singleLine = true,
                    label = { Text(stringResource(R.string.item_current_label)) }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = submitted && !valid,
                    supportingText = { if (submitted && !valid) Text(stringResource(R.string.quick_progress_error, item.totalCount)) })
            }
        },
        confirmButton = { TextButton({ submitted = true; if (valid) onExact(number!!) }) { Text(stringResource(R.string.item_save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.item_done)) } })
}

package io.github.secnewt.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.secnewt.ui.neonFieldColors
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.weather.data.ZipTable

/** Where the forecast is for. [onCancel] is null the first time, when there's nothing to go back to. */
@Composable
fun ZipScreen(
    initial: String,
    error: String?,
    onSave: (String) -> Unit,
    onCancel: (() -> Unit)?,
) {
    var zip by rememberSaveable { mutableStateOf(initial) }
    // Never a greyed-out button (hard to read with low vision): tapping too early explains instead.
    var tooShort by rememberSaveable { mutableStateOf(false) }
    val shown = if (tooShort) "Enter all five digits of your ZIP." else error
    val submit: () -> Unit = { if (ZipTable.isValid(zip)) { tooShort = false; onSave(zip) } else { tooShort = true } }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Newt Weather",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text("Which ZIP code should the forecast be for?", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = zip,
                onValueChange = { typed -> zip = typed.filter { it.isDigit() }.take(5); tooShort = false },
                label = { Text("ZIP code") },
                singleLine = true,
                isError = shown != null,
                supportingText = shown?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = neonFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = submit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("Show the forecast", style = MaterialTheme.typography.titleMedium)
            }
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    border = neonOutline(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Cancel", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "Your ZIP stays on this phone. Only map coordinates for it are sent to the " +
                    "National Weather Service, to get the forecast.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

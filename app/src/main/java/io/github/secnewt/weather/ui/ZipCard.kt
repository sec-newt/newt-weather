package io.github.secnewt.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.secnewt.ui.CardTitle
import io.github.secnewt.ui.neonOutline

/**
 * The only setting, so it's a card at the bottom of the forecast rather than its own page.
 * Closed: where the forecast is for, and a Change ZIP button. Open: the ZIP box, with Save
 * and Cancel; nothing changes until Save.
 */
@Composable
fun ZipCard(
    place: String,
    zip: String,
    editing: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        border = neonOutline(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle("Location")
            Text(
                if (place.isEmpty()) "Forecast for ZIP $zip" else "Forecast for $place · ZIP $zip",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (editing) {
                ZipEntry(initial = zip, error = error, saveLabel = "Save", onSave = onSave, onCancel = onCancel)
                PrivacyNote()
            } else {
                OutlinedButton(
                    onClick = onEdit,
                    border = neonOutline(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Change ZIP", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

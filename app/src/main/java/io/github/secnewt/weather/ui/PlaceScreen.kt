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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.secnewt.ui.NewtButton
import io.github.secnewt.ui.neonFieldColors
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.newtContainerColor
import io.github.secnewt.weather.data.CitySearch
import io.github.secnewt.weather.data.Location

/** What the last search in the place box found: a problem to show, or places to choose from. */
data class PlaceSearch(
    val error: String? = null,
    val choices: List<Location> = emptyList(),
    /** More matched than [choices] shows. */
    val more: Boolean = false,
)

/** The first-run screen: which place the forecast should be for. */
@Composable
fun PlaceScreen(
    search: PlaceSearch,
    onSearch: (String) -> Unit,
    onPick: (Location) -> Unit,
    onTyped: () -> Unit,
) {
    Scaffold(containerColor = newtContainerColor()) { padding ->
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
            Text("Which city or ZIP code should the forecast be for?", style = MaterialTheme.typography.bodyLarge)
            PlaceEntry(search, saveLabel = "Show the forecast", onSearch = onSearch, onPick = onPick, onTyped = onTyped, onCancel = null)
            PrivacyNote()
        }
    }
}

/**
 * The "City or ZIP code" box and its button, shared by the first-run screen and the Places
 * card. When a name matches more than one place (Springfield), each one is a button to pick.
 */
@Composable
fun PlaceEntry(
    search: PlaceSearch,
    saveLabel: String,
    onSearch: (String) -> Unit,
    onPick: (Location) -> Unit,
    onTyped: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val submit = { onSearch(query) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { typed -> query = typed.take(MAX_LENGTH); onTyped() },
            label = { Text("City or ZIP code") },
            placeholder = { Text("Paris, TX or 75460") },
            singleLine = true,
            isError = search.error != null,
            supportingText = search.error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            colors = neonFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        // Never a greyed-out button (hard to read with low vision): tapping too early explains instead.
        Button(onClick = submit, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(saveLabel, style = MaterialTheme.typography.titleMedium)
        }
        if (search.choices.isNotEmpty()) {
            Text(
                "Which one?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            search.choices.forEach { place ->
                NewtButton(place.name, onClick = { onPick(place) }, modifier = Modifier.fillMaxWidth())
            }
            if (search.more) {
                Text(
                    "Showing the first ${CitySearch.LIMIT}. Add the state to find the right one, like Springfield, IL.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
    }
}

private const val MAX_LENGTH = 60

@Composable
fun PrivacyNote() {
    Text(
        "What you type stays on this phone: it's looked up in a table built into the app. Only " +
            "map coordinates go to the National Weather Service, to get the forecast.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

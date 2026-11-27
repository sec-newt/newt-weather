package io.github.secnewt.weather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import io.github.secnewt.ui.NewtButton
import io.github.secnewt.ui.NewtSettingRow
import io.github.secnewt.ui.NewtSettingsCard
import io.github.secnewt.ui.NewtStateChip
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.NewtValueRow
import io.github.secnewt.weather.data.Location

/**
 * The saved places, at the bottom of the forecast. Tap a place to show its forecast; the one
 * showing says SHOWING. Each has a Remove button while there's more than one. "Add a place"
 * opens the City or ZIP box in the card; nothing changes until a place is picked.
 */
@Composable
fun PlacesCard(
    locations: List<Location>,
    active: Location,
    adding: Boolean,
    search: PlaceSearch,
    onShow: (Location) -> Unit,
    onRemove: (Location) -> Unit,
    onAdd: () -> Unit,
    onSearch: (String) -> Unit,
    onPick: (Location) -> Unit,
    onTyped: () -> Unit,
    onCancel: () -> Unit,
) {
    NewtSettingsCard("Places", icon = Icons.Filled.Place) {
        locations.forEach { place ->
            val showing = place.key == active.key
            NewtSettingRow(
                title = place.label,
                // The ZIP tells apart two places NOAA gives the same name.
                hint = listOfNotNull(place.zip?.let { "ZIP $it" }, if (showing) null else "Tap to show").joinToString(" · ").ifEmpty { null },
                modifier = if (showing) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = "Show the forecast") { onShow(place) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showing) NewtStateChip("SHOWING", NewtTone.Good)
                        if (locations.size > 1) RemoveButton(place) { onRemove(place) }
                    }
                },
            )
        }
        if (adding) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlaceEntry(search, saveLabel = "Add", onSearch = onSearch, onPick = onPick, onTyped = onTyped, onCancel = onCancel)
                PrivacyNote()
            }
        } else {
            NewtValueRow("Add a place", value = null, onClick = onAdd, hint = "A city or a ZIP code")
        }
    }
}

/** Reads "Remove Paris, TX · ZIP 75460", so no two Remove buttons sound the same. */
@Composable
private fun RemoveButton(place: Location, onClick: () -> Unit) {
    val spoken = "Remove ${place.fullLabel}"
    NewtButton(
        "Remove",
        onClick = onClick,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            role = Role.Button
            onClick { onClick(); true }
        },
    )
}

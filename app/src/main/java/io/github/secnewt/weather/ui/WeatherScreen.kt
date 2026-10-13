package io.github.secnewt.weather.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.secnewt.ui.CardTitle
import io.github.secnewt.ui.NewtFonts
import io.github.secnewt.ui.NewtPromptLine
import io.github.secnewt.ui.neonOutline
import io.github.secnewt.ui.newtContainerColor
import io.github.secnewt.weather.data.Alert
import io.github.secnewt.weather.data.Day
import io.github.secnewt.weather.data.Forecast
import io.github.secnewt.weather.data.HighLow
import java.time.OffsetDateTime
import java.time.ZoneId

/** The main screen: now, alerts, and the 7-day forecast. */
@Composable
fun WeatherScreen(
    forecast: Forecast,
    now: OffsetDateTime,
    zone: ZoneId,
    refreshing: Boolean,
    status: String?,
    notificationsAllowed: Boolean,
    onRefresh: () -> Unit,
    onAllowNotifications: () -> Unit,
    onNotificationSettings: () -> Unit,
    editingZip: Boolean,
    zipError: String?,
    onEditZip: () -> Unit,
    onSaveZip: (String) -> Unit,
    onCancelZip: () -> Unit,
) {
    val hasWarning = forecast.alerts.any { it.isWarning }
    val list = rememberLazyListState()
    // Opening the ZIP card from the title brings it into view.
    LaunchedEffect(editingZip) {
        if (editingZip) list.animateScrollToItem(list.layoutInfo.totalItemsCount - 1)
    }
    Scaffold(containerColor = newtContainerColor()) { padding ->
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PlaceTitle(forecast.place.label.ifEmpty { "ZIP ${forecast.zip}" }, onEditZip) }
            // Terminal lines above each section in the terminal look (nothing in the clean one).
            item { NewtPromptLine("weather", "now", title = null) }
            item { NowCard(forecast, now) }
            if (forecast.alerts.isNotEmpty()) item { NewtPromptLine("weather", "alerts", title = null) }
            items(forecast.alerts, key = { it.id }) { AlertCard(it) }
            if (hasWarning && !notificationsAllowed) {
                item(key = "warnings-top") { NotificationsCard(false, onAllowNotifications, onNotificationSettings) }
            }
            item { NewtPromptLine("weather", "forecast --7d", title = "Next 7 days") }
            items(forecast.days(now), key = { it.name }) { DayCard(it) }
            item { NewtPromptLine("weather", "config", title = null) }
            item { Footer(forecast, zone, refreshing, status, onRefresh) }
            item { NotificationsCard(notificationsAllowed, onAllowNotifications, onNotificationSettings) }
            item {
                ZipCard(
                    place = forecast.place.label,
                    zip = forecast.zip,
                    editing = editingZip,
                    error = zipError,
                    onEdit = onEditZip,
                    onSave = onSaveZip,
                    onCancel = onCancelZip,
                )
            }
        }
    }
}

/** The place name; tapping it is a shortcut to the ZIP card at the bottom. */
@Composable
private fun PlaceTitle(title: String, onEditZip: () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = "Change ZIP", onClick = onEditZip)
            .semantics { heading() }
            .padding(vertical = 8.dp),
    )
}

@Composable
private fun NowCard(forecast: Forecast, now: OffsetDateTime) {
    val temp = forecast.currentTemperature(now)
    val condition = forecast.currentCondition(now)
    val description = forecast.currentDescription(now)
    val highLow = forecast.highLow(now)
    val isDaytime = forecast.currentIsDaytime(now)
    val spoken = buildString {
        append("Now ")
        append(temp?.let { "$it degrees" } ?: "temperature unknown")
        if (description.isNotEmpty()) append(", ${description.lowercase()}")
        append(".")
        highLow?.let { append(" ${highLowSpoken(it)}.") }
    }
    Card(
        border = neonOutline(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (condition != null) WeatherIcon(condition, isDaytime, size = 72.dp, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = temp?.let { "$it°" } ?: "--°",
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = NewtFonts.Mono,
                    fontWeight = FontWeight.Bold,
                )
                if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.titleMedium)
                highLow?.let { HighLowLabel(it) }
            }
        }
    }
}

@Composable
private fun HighLowLabel(highLow: HighLow) {
    Text(
        text = highLowText(highLow),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (highLow.isHigh) TemperatureColors.high() else TemperatureColors.low(),
    )
}

@Composable
private fun AlertCard(alert: Alert) {
    var open by rememberSaveable(alert.id) { mutableStateOf(false) }
    val color = if (alert.isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    Card(
        border = BorderStroke(if (alert.isWarning) 3.dp else 1.5.dp, color),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = if (open) "Hide details" else "Show details") { open = !open },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = alert.event,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.semantics { heading() },
            )
            alert.ends?.let { Text("Until ${dayTimeText(it)}", style = MaterialTheme.typography.bodyLarge) }
            if (open) {
                if (alert.instruction.isNotBlank()) {
                    Text("What to do", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(alert.instruction, style = MaterialTheme.typography.bodyLarge)
                }
                Text(alert.description, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "Tap for details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Warning notifications: always reachable from the bottom of the forecast, so they can be
 * turned on before a warning arrives, and shown near the top too while a warning is active
 * and they're off.
 */
@Composable
fun NotificationsCard(allowed: Boolean, onTurnOn: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        border = neonOutline(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardTitle("Warning notifications")
            if (allowed) {
                Text(
                    "On. You'll get a notification when the National Weather Service issues a " +
                        "warning for your ZIP. Watches and advisories stay quiet.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(
                    onClick = onOpenSettings,
                    border = neonOutline(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Notification settings", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text(
                    "Off. Turn on to get a notification when the National Weather Service issues a " +
                        "warning for your ZIP (tornado, severe thunderstorm, flood, freeze). Watches " +
                        "and advisories stay quiet.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onTurnOn, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Turn on warnings", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: Day) {
    var open by rememberSaveable(day.name) { mutableStateOf(false) }
    val main = day.day ?: day.night ?: return
    val rain = listOfNotNull(day.day?.precipChance, day.night?.precipChance).maxOrNull()
    val spoken = buildString {
        append("${day.name}: ${main.shortForecast.lowercase()}.")
        day.day?.let { append(" High ${it.temperature}.") }
        day.night?.let { append(" Low ${it.temperature}.") }
        if (rain != null && rain > 0) append(" $rain percent chance of rain.")
    }
    Card(
        border = neonOutline(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClickLabel = if (open) "Hide details" else "Show details") { open = !open },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            ) {
                WeatherIcon(main.condition, main.isDaytime, size = 40.dp, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(day.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(main.shortForecast, style = MaterialTheme.typography.bodyMedium)
                    if (rain != null && rain > 0) {
                        Text("$rain% chance of rain", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    day.day?.let { HighLowLabel(HighLow(it.temperature, isHigh = true)) }
                    day.night?.let { HighLowLabel(HighLow(it.temperature, isHigh = false)) }
                }
            }
            if (open) {
                listOfNotNull(day.day, day.night).forEach { period ->
                    Text(period.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(period.detailedForecast, style = MaterialTheme.typography.bodyLarge)
                    if (period.wind.isNotBlank()) Text("Wind: ${period.wind}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Footer(forecast: Forecast, zone: ZoneId, refreshing: Boolean, status: String?, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "From the National Weather Service. Updated ${timeText(forecast.fetchedAt.atZone(zone).toOffsetDateTime())}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (status != null) Text(status, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        OutlinedButton(
            onClick = onRefresh,
            enabled = !refreshing,
            border = neonOutline(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(if (refreshing) "Updating…" else "Update now", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Shown until the first forecast arrives, or if it can't. */
@Composable
fun LoadingScreen(status: String?, refreshing: Boolean, onRetry: () -> Unit, onChangeZip: () -> Unit) {
    Scaffold(containerColor = newtContainerColor()) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Newt Weather",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                status ?: "Getting the forecast…",
                style = MaterialTheme.typography.bodyLarge,
                color = if (status != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            )
            Button(onClick = onRetry, enabled = !refreshing, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (refreshing) "Updating…" else "Try again", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onChangeZip, border = neonOutline(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Change ZIP", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

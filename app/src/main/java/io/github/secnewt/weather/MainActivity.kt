package io.github.secnewt.weather

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.github.secnewt.ui.NewtLookTheme
import io.github.secnewt.ui.newtContainerColor
import io.github.secnewt.ui.NewtTheme
import io.github.secnewt.ui.rememberFamilyLook
import io.github.secnewt.weather.alerts.WarningAlerts
import io.github.secnewt.weather.alerts.WarningNotifier
import io.github.secnewt.weather.data.CityTable
import io.github.secnewt.weather.data.Forecast
import io.github.secnewt.weather.data.Location
import io.github.secnewt.weather.data.Lookup
import io.github.secnewt.weather.data.PlaceFinder
import io.github.secnewt.weather.data.RefreshException
import io.github.secnewt.weather.data.WeatherRepository
import io.github.secnewt.weather.data.WeatherStore
import io.github.secnewt.weather.data.ZipTable
import io.github.secnewt.weather.share.ForecastShare
import io.github.secnewt.weather.ui.LoadingScreen
import io.github.secnewt.weather.ui.PlaceScreen
import io.github.secnewt.weather.ui.PlaceSearch
import io.github.secnewt.weather.ui.PlacesCard
import io.github.secnewt.weather.ui.WeatherScreen
import io.github.secnewt.weather.work.RefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val store by lazy { WeatherStore(this) }

    private var locations by mutableStateOf<List<Location>>(emptyList())
    /** The place showing; null only before the first one is chosen. */
    private var active by mutableStateOf<Location?>(null)
    private var forecast by mutableStateOf<Forecast?>(null)
    private var addingPlace by mutableStateOf(false)
    private var search by mutableStateOf(PlaceSearch())
    private var refreshing by mutableStateOf(false)
    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    /** False until the saved places have been read (off the main thread) at start. */
    private var loaded by mutableStateOf(false)
    /** The place the running refresh is for. */
    private var refreshingKey: String? = null
    private var status by mutableStateOf<String?>(null)
    private var notificationsAllowed by mutableStateOf(false)
    /** The Warning alerts switch as stored: null until it's first touched. */
    private var warningAlertsStored by mutableStateOf<Boolean?>(null)

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        if (granted) forecast?.let { shown -> notifyWarnings(shown) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The first read can move an old single-ZIP setup into places, which reads the ZIP
        // table, so it happens off the main thread; the screen stays blank for that moment.
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() }
            reloadPlaces()
            forecast = saved
            warningAlertsStored = store.warningAlerts
            loaded = true
            if (active != null) RefreshWorker.schedule(this@MainActivity)
            refreshIfStale()
        }
        setContent {
            NewtTheme {
                // Follows Newt Home's look (`look terminal` / `look clean`); clean without it.
                NewtLookTheme(rememberFamilyLook()) {
                    App()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        notificationsAllowed = WarningNotifier.allowed(this)
        if (!loaded) return // onCreate's first read does the rest
        warningAlertsStored = store.warningAlerts
        // A background refresh may have named a ZIP-only place, or brought a newer forecast.
        reloadPlaces()
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() }
            if (saved != null && saved.location.key == active?.key) {
                val shown = forecast
                if (shown == null || shown.location.key != saved.location.key || saved.fetchedAt > shown.fetchedAt) forecast = saved
            }
            refreshIfStale()
        }
    }

    /** Warnings read and write their history on disk, so they're sent off the main thread. */
    private fun notifyWarnings(forecast: Forecast) {
        // Not tied to the screen: closing the app right then mustn't drop a warning.
        val context = applicationContext
        BACKGROUND.launch { WarningNotifier(context).notifyNew(forecast) }
    }

    private fun reloadPlaces() {
        locations = store.locations
        active = store.active
    }

    private fun refreshIfStale() {
        val last = forecast?.takeIf { it.location.key == active?.key }?.fetchedAt
        if (active != null && (last == null || Duration.between(last, Instant.now()) > STALE)) refresh()
    }

    @Composable
    private fun App() {
        if (!loaded) {
            Box(Modifier.fillMaxSize().background(newtContainerColor()))
            return
        }
        val place = active
        val current = forecast?.takeIf { it.location.key == place?.key }
        // Back closes the "Add a place" box instead of leaving the app.
        BackHandler(enabled = addingPlace) { closeAdding() }
        if (place == null) {
            PlaceScreen(search, onSearch = ::searchPlaces, onPick = ::pick, onTyped = ::clearSearch)
            return
        }
        val places = @Composable {
            PlacesCard(
                locations = locations,
                active = place,
                adding = addingPlace,
                search = search,
                onShow = ::show,
                onRemove = ::remove,
                onAdd = { addingPlace = true },
                onSearch = ::searchPlaces,
                onPick = ::pick,
                onTyped = ::clearSearch,
                onCancel = ::closeAdding,
            )
        }
        when {
            current == null -> LoadingScreen(place.label, status, refreshing, onRetry = ::refresh, places = places)
            else -> WeatherScreen(
                forecast = current,
                now = OffsetDateTime.now(),
                zone = ZoneId.systemDefault(),
                refreshing = refreshing,
                status = status,
                warningAlertsOn = WarningAlerts.switchOn(warningAlertsStored, notificationsAllowed),
                notificationsBlocked = !notificationsAllowed,
                onRefresh = ::refresh,
                onWarningAlertsChange = ::setWarningAlerts,
                onNotificationSettings = ::openNotificationSettings,
                addingPlace = addingPlace,
                places = places,
            )
        }
    }

    /**
     * Asks for the notification permission. After two "Don't allow"s Android stops asking,
     * so from then on this opens the app's notification settings instead of doing nothing.
     */
    /** The switch always flips. Turning it on asks Android for permission if it's needed. */
    private fun setWarningAlerts(on: Boolean) {
        store.warningAlerts = on
        warningAlertsStored = on
        if (on && !notificationsAllowed) turnOnWarnings()
    }

    private fun turnOnWarnings() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (store.askedNotifications && !shouldShowRequestPermissionRationale(permission)) {
            openNotificationSettings()
        } else {
            store.askedNotifications = true
            askNotifications.launch(permission)
        }
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun closeAdding() {
        addingPlace = false
        searchJob?.cancel()
        search = PlaceSearch()
    }

    /** Typing again drops the last search's answer, and any search still running. */
    private fun clearSearch() {
        searchJob?.cancel()
        if (search != PlaceSearch()) search = PlaceSearch()
    }

    /** Looks up what was typed (off the main thread: the tables are big files). One match is added straight away. */
    private fun searchPlaces(query: String) {
        // Cancelled by Cancel, Back or more typing, so a slow answer can't add a place nobody wants now.
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) {
                val context = this@MainActivity
                PlaceFinder(zip = { ZipTable(context).lookup(it) }, city = { CityTable(context).search(it) }).find(query)
            }
            when (found) {
                is Lookup.Problem -> search = PlaceSearch(error = found.message)
                is Lookup.Found -> if (found.places.size == 1) {
                    pick(found.places.single())
                } else {
                    search = PlaceSearch(choices = found.places, more = found.more)
                }
            }
        }
    }

    /** Saves the place (or finds it already saved) and shows it. */
    private fun pick(place: Location) {
        val saved = store.add(place)
        closeAdding()
        locations = store.locations
        show(saved)
    }

    private fun show(place: Location) {
        // The old place's refresh stops, so it can't keep the new place's Update button busy.
        if (refreshingKey != place.key) stopRefresh()
        store.activate(place)
        active = place
        status = null
        // Newt Home follows the place showing.
        ForecastShare.notifyChanged(this)
        RefreshWorker.schedule(this)
        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { store.load(place) }
            // Tapping quickly through places: only the last one tapped lands.
            if (active?.key != place.key) return@launch
            // A refresh for this place may have landed first; keep the newer one.
            val shown = forecast
            if (shown == null || shown.location.key != place.key || (saved != null && saved.fetchedAt > shown.fetchedAt)) forecast = saved
            refreshIfStale()
        }
    }

    private fun remove(place: Location) {
        val wasShowing = place.key == active?.key
        if (refreshingKey == place.key) stopRefresh()
        // The list changes now, so the place can't be tapped again; its files go in the background.
        store.remove(place)
        BACKGROUND.launch { store.deleteFiles(place) }
        locations = store.locations
        val next = store.active
        if (next == null) {
            active = null
            forecast = null
        } else if (wasShowing) {
            show(next)
        }
    }

    private fun stopRefresh() {
        refreshJob?.cancel()
        refreshJob = null
        refreshingKey = null
        refreshing = false
    }

    /**
     * Refreshes the place showing. Switching places mid-refresh drops the old one's refresh,
     * so its answer can't land on top of the new place.
     */
    private fun refresh() {
        val place = active ?: return
        if (refreshing && refreshJob?.isActive == true && refreshingKey == place.key) return
        refreshJob?.cancel()
        refreshing = true
        refreshingKey = place.key
        val job = lifecycleScope.launch(start = CoroutineStart.LAZY) {
            try {
                val fresh = WeatherRepository(this@MainActivity).refresh(place)
                if (active?.key != place.key) return@launch
                forecast = fresh
                status = null
                // A ZIP-only place may have just been given NOAA's name for it.
                reloadPlaces()
                notifyWarnings(fresh)
            } catch (e: RefreshException) {
                if (active?.key == place.key) status = e.message
            } finally {
                if (refreshJob === coroutineContext[Job]) refreshing = false
            }
        }
        refreshJob = job
        job.start()
    }

    private companion object {
        val STALE: Duration = Duration.ofMinutes(30)
        val BACKGROUND = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

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
import io.github.secnewt.ui.NewtLookTheme
import io.github.secnewt.ui.NewtTheme
import io.github.secnewt.ui.rememberFamilyLook
import io.github.secnewt.weather.alerts.WarningNotifier
import io.github.secnewt.weather.data.Forecast
import io.github.secnewt.weather.data.RefreshException
import io.github.secnewt.weather.data.WeatherRepository
import io.github.secnewt.weather.data.WeatherStore
import io.github.secnewt.weather.data.ZipTable
import io.github.secnewt.weather.ui.LoadingScreen
import io.github.secnewt.weather.ui.WeatherScreen
import io.github.secnewt.weather.ui.ZipScreen
import io.github.secnewt.weather.work.RefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val store by lazy { WeatherStore(this) }

    private var zip by mutableStateOf<String?>(null)
    private var forecast by mutableStateOf<Forecast?>(null)
    private var editingZip by mutableStateOf(false)
    private var zipError by mutableStateOf<String?>(null)
    private var refreshing by mutableStateOf(false)
    private var status by mutableStateOf<String?>(null)
    private var notificationsAllowed by mutableStateOf(false)

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
        if (granted) forecast?.let { WarningNotifier(this).notifyNew(it.alerts) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        zip = store.zip
        forecast = store.load()
        if (zip != null) RefreshWorker.schedule(this)
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
        val last = forecast?.fetchedAt
        if (zip != null && (last == null || Duration.between(last, Instant.now()) > STALE)) refresh()
    }

    @Composable
    private fun App() {
        val current = forecast
        // Back closes the ZIP box (card or screen) instead of leaving the app.
        BackHandler(enabled = editingZip) { editingZip = false; zipError = null }
        when {
            zip == null || (editingZip && current == null) -> ZipScreen(
                initial = zip.orEmpty(),
                error = zipError,
                onSave = ::saveZip,
                onCancel = if (zip != null) ({ editingZip = false; zipError = null }) else null,
            )
            current == null -> LoadingScreen(status, refreshing, onRetry = ::refresh, onChangeZip = { editingZip = true })
            else -> WeatherScreen(
                forecast = current,
                now = OffsetDateTime.now(),
                zone = ZoneId.systemDefault(),
                refreshing = refreshing,
                status = status,
                notificationsAllowed = notificationsAllowed,
                onRefresh = ::refresh,
                onAllowNotifications = ::turnOnWarnings,
                onNotificationSettings = ::openNotificationSettings,
                editingZip = editingZip,
                zipError = zipError,
                onEditZip = { editingZip = true },
                onSaveZip = ::saveZip,
                onCancelZip = { editingZip = false; zipError = null },
            )
        }
    }

    /**
     * Asks for the notification permission. After two "Don't allow"s Android stops asking,
     * so from then on this opens the app's notification settings instead of doing nothing.
     */
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

    private fun saveZip(newZip: String) {
        lifecycleScope.launch {
            val known = withContext(Dispatchers.IO) { ZipTable(this@MainActivity).lookup(newZip) != null }
            if (!known) {
                zipError = "That ZIP isn't one we know. Check the five digits."
                return@launch
            }
            zipError = null
            editingZip = false
            if (newZip != zip) {
                store.zip = newZip
                zip = newZip
                forecast = null
                RefreshWorker.schedule(this@MainActivity)
                refresh()
            }
        }
    }

    private fun refresh() {
        if (refreshing) return
        refreshing = true
        lifecycleScope.launch {
            try {
                val fresh = WeatherRepository(this@MainActivity).refresh()
                forecast = fresh
                status = null
                WarningNotifier(this@MainActivity).notifyNew(fresh.alerts)
            } catch (e: RefreshException) {
                status = e.message
            } finally {
                refreshing = false
            }
        }
    }

    private companion object {
        val STALE: Duration = Duration.ofMinutes(30)
    }
}

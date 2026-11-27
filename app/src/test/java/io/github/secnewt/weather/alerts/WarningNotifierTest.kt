package io.github.secnewt.weather.alerts

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.weather.data.Coordinates
import io.github.secnewt.weather.data.Fixtures
import io.github.secnewt.weather.data.Location
import io.github.secnewt.weather.data.WeatherStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WarningNotifierTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun aWarningSaysWhichPlaceItsFor() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val store = WeatherStore(app).apply { warningAlerts = true; add(Fixtures.OKC) }
        val forecast = Fixtures.forecast()
        WarningNotifier(app, store).notifyNew(forecast)
        val shown = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications
        val warnings = forecast.alerts.filter { it.isWarning }
        assertEquals(warnings.size, shown.size)
        assertEquals(
            "${warnings.first().event} · Oklahoma City, OK",
            shown.first().extras.getString(Notification.EXTRA_TITLE),
        )
        // Each one notifies once.
        WarningNotifier(app, store).notifyNew(forecast)
        assertEquals(warnings.size, shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications.size)
    }

    @Test
    fun showingAnotherPlaceDoesntMakeOldWarningsBuzzAgain() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val elsewhere = Location("Paris, TX", null, Coordinates("33.67", "-95.55"))
        val store = WeatherStore(app).apply { warningAlerts = true; add(Fixtures.OKC); add(elsewhere) }
        val notifier = WarningNotifier(app, store)
        val okc = Fixtures.forecast()
        val manager = app.getSystemService(NotificationManager::class.java)
        notifier.notifyNew(okc)
        assertTrue(shadowOf(manager).allNotifications.isNotEmpty())
        manager.cancelAll() // swiped away
        // Paris has no alerts; back in Oklahoma City the same warnings are still up, and stay quiet.
        notifier.notifyNew(okc.copy(location = elsewhere, alerts = emptyList()))
        notifier.notifyNew(okc)
        assertTrue(shadowOf(manager).allNotifications.isEmpty())
    }
}

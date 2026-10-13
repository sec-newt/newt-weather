package io.github.secnewt.weather.share

import android.content.ComponentName
import android.content.Context
import android.content.pm.PermissionInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.weather.data.Fixtures
import io.github.secnewt.weather.data.WeatherStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ForecastShareTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetClock() {
        ForecastProvider.now = { java.time.OffsetDateTime.now() }
    }

    @Test
    fun middaySummary() {
        val row = ForecastShare.summaryRow(Fixtures.forecast(), Fixtures.NOON)
        val byName = ForecastShare.SUMMARY_COLUMNS.zip(row.toList()).toMap()
        assertEquals(82, byName[ForecastShare.TEMPERATURE])
        assertEquals(90, byName[ForecastShare.HIGH_LOW])
        assertEquals(1, byName[ForecastShare.IS_HIGH])
        assertEquals("PARTLY_CLOUDY", byName[ForecastShare.CONDITION])
        assertEquals(1, byName[ForecastShare.IS_DAYTIME])
        assertEquals("Mostly Sunny", byName[ForecastShare.DESCRIPTION])
        assertEquals(1, byName[ForecastShare.WARNINGS]) // the Flood Warning, not the Flood Watch
        assertEquals("Flood Warning", byName[ForecastShare.WARNING_EVENT])
        assertEquals(ForecastShare.VERSION, byName[ForecastShare.SCHEMA])
    }

    @Test
    fun eveningSummaryHasTonightsLow() {
        val row = ForecastShare.summaryRow(Fixtures.forecast(), Fixtures.EVENING)
        val byName = ForecastShare.SUMMARY_COLUMNS.zip(row.toList()).toMap()
        assertEquals(75, byName[ForecastShare.HIGH_LOW])
        assertEquals(0, byName[ForecastShare.IS_HIGH])
        assertEquals(0, byName[ForecastShare.IS_DAYTIME])
    }

    @Test
    fun hourlyStartsThisHour() {
        val rows = ForecastShare.hourlyRows(Fixtures.forecast(), Fixtures.NOON)
        assertEquals(24, rows.size)
        assertEquals(Fixtures.NOON.withMinute(0).toInstant().toEpochMilli(), rows[0][0])
        assertEquals(83, rows[0][1])
    }

    @Test
    fun providerServesTheSavedForecast() {
        saveFixtures()
        ForecastProvider.now = { Fixtures.NOON }
        val provider = Robolectric.buildContentProvider(ForecastProvider::class.java)
            .create(ForecastShare.authority(context.packageName)).get()
        val summary = provider.query(ForecastShare.summaryUri(context.packageName), null, null, null, null)!!
        summary.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            assertEquals(82, it.getInt(it.getColumnIndexOrThrow(ForecastShare.TEMPERATURE)))
            assertEquals(90, it.getInt(it.getColumnIndexOrThrow(ForecastShare.HIGH_LOW)))
        }
        val hourlyUri = ForecastShare.summaryUri(context.packageName).buildUpon().path(ForecastShare.PATH_HOURLY).build()
        provider.query(hourlyUri, null, null, null, null)!!.use { assertEquals(24, it.count) }
        val other = ForecastShare.summaryUri(context.packageName).buildUpon().path("zip").build()
        assertNull(provider.query(other, null, null, null, null))
    }

    @Test
    fun noForecastYetMeansNoRows() {
        val provider = Robolectric.buildContentProvider(ForecastProvider::class.java)
            .create(ForecastShare.authority(context.packageName)).get()
        provider.query(ForecastShare.summaryUri(context.packageName), null, null, null, null)!!.use {
            assertEquals(0, it.count)
        }
    }

    /** The security of the sharing rests on these two manifest lines, so they're checked. */
    @Test
    fun onlyNewtSignedAppsCanRead() {
        val permission = ForecastShare.permission(context.packageName)
        val info = context.packageManager.getPermissionInfo(permission, 0)
        assertEquals(PermissionInfo.PROTECTION_SIGNATURE, info.protection)
        val provider = context.packageManager.getProviderInfo(ComponentName(context, ForecastProvider::class.java), 0)
        assertEquals(permission, provider.readPermission)
        assertEquals(permission, provider.writePermission)
    }

    private fun saveFixtures() {
        val store = WeatherStore(context)
        store.zip = "73102"
        store.savePoints("73102", Fixtures.read("points.json"), "KOKC")
        store.save(
            "73102", Fixtures.read("forecast.json"), Fixtures.read("hourly.json"),
            Fixtures.read("observation.json"), Fixtures.read("alerts.json"), Instant.parse("2026-10-13T16:25:00Z"),
        )
    }
}

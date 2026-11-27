package io.github.secnewt.weather.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WeatherStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val paris = Location("Paris, TX", null, Coordinates("33.67", "-95.55"))
    private val fetched = Instant.parse("2026-10-13T16:25:00Z")

    private fun saveForecast(store: WeatherStore, place: Location) {
        store.savePoints(place, Fixtures.read("points.json"), "KOKC")
        store.save(place, Fixtures.read("forecast.json"), Fixtures.read("hourly.json"), null, Fixtures.read("alerts.json"), fetched)
    }

    @Test
    fun addingShowsItAndTheSameSpotIsSavedOnce() {
        val store = WeatherStore(context)
        store.add(Fixtures.OKC)
        store.add(paris)
        assertEquals(paris, store.active)
        store.add(Fixtures.OKC.copy(name = "again"))
        assertEquals(listOf(Fixtures.OKC, paris), store.locations)
        assertEquals(Fixtures.OKC, store.active)
    }

    @Test
    fun eachPlaceKeepsItsOwnForecast() {
        val store = WeatherStore(context)
        store.add(Fixtures.OKC)
        saveForecast(store, Fixtures.OKC)
        store.add(paris)
        assertNull("Paris has no forecast yet", store.load())
        store.activate(Fixtures.OKC)
        assertEquals(Fixtures.OKC, store.load()?.location)
    }

    @Test
    fun removingTheOneShowingShowsTheNext() {
        val store = WeatherStore(context)
        store.add(Fixtures.OKC)
        saveForecast(store, Fixtures.OKC)
        store.add(paris)
        store.activate(Fixtures.OKC)
        store.remove(Fixtures.OKC)
        store.deleteFiles(Fixtures.OKC)
        assertEquals(listOf(paris), store.locations)
        assertEquals(paris, store.active)
        assertNull("its forecast went with it", store.load(Fixtures.OKC))
        store.remove(paris)
        assertNull(store.active)
    }

    @Test
    fun renamingAZipPlace() {
        val store = WeatherStore(context)
        val zipOnly = Fixtures.OKC.copy(name = "")
        store.add(zipOnly)
        store.rename(zipOnly, "Oklahoma City, OK")
        assertEquals(Fixtures.OKC, store.active)
    }

    /** Before places there was one ZIP and one forecast; an update keeps both. */
    @Test
    fun theOldSingleZipBecomesTheFirstPlace() {
        val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
        val dir = File(context.filesDir, "noaa").apply { mkdirs() }
        File(dir, "points.json").writeText(Fixtures.read("points.json"))
        File(dir, "forecast.json").writeText(Fixtures.read("forecast.json"))
        File(dir, "hourly.json").writeText(Fixtures.read("hourly.json"))
        File(dir, "alerts.json").writeText(Fixtures.read("alerts.json"))
        prefs.edit()
            .putString("zip", "73102").putString("points_zip", "73102").putString("station", "KOKC")
            .putString("forecast_zip", "73102").putLong("fetched_at", fetched.toEpochMilli())
            .putStringSet("notified_warnings", setOf("warning-1"))
            .commit()

        val store = WeatherStore(context)

        val place = store.active
        assertNotNull(place)
        assertEquals("73102", place!!.zip)
        assertEquals(Coordinates("35.4707", "-97.5191"), place.coordinates)
        assertEquals("Oklahoma City, OK", place.name)
        assertEquals(fetched, store.load()?.fetchedAt)
        assertEquals("KOKC", store.cachedPoints(place)?.second)
        assertEquals(setOf("warning-1"), store.notifiedWarnings(place))
        assertNull("the old keys are gone", prefs.getString("zip", null))
        // A second start doesn't migrate again.
        assertEquals(listOf(place), WeatherStore(context).locations)
    }

    @Test
    fun aRefreshFinishingAfterItsPlaceWasRemovedLeavesNothingBehind() {
        val store = WeatherStore(context)
        store.add(Fixtures.OKC)
        store.add(paris)
        store.remove(paris)
        saveForecast(store, paris)
        store.rename(paris, "Somewhere")
        assertNull(store.load(paris))
        assertEquals(listOf(Fixtures.OKC), store.locations)
        assertEquals(false, File(context.filesDir, "noaa/${paris.key}").exists())
    }

    @Test
    fun addedBackBeforeItsFilesWentKeepsThem() {
        val store = WeatherStore(context)
        store.add(Fixtures.OKC)
        saveForecast(store, Fixtures.OKC)
        store.add(paris)
        store.remove(Fixtures.OKC)
        store.add(Fixtures.OKC)
        store.deleteFiles(Fixtures.OKC)
        assertEquals(Fixtures.OKC, store.load(Fixtures.OKC)?.location)
    }
}

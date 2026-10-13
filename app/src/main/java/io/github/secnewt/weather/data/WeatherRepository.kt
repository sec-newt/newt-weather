package io.github.secnewt.weather.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/** Why a refresh didn't work, in words for the screen. */
class RefreshException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Fetches a fresh forecast for the saved ZIP and stores it. */
class WeatherRepository(
    context: Context,
    private val store: WeatherStore = WeatherStore(context),
    private val zips: ZipTable = ZipTable(context),
    private val noaa: NoaaClient = NoaaClient(),
) {

    /** Runs on a background thread: it reads the ZIP table and writes files as well as fetching. */
    suspend fun refresh(): Forecast = withContext(Dispatchers.IO) {
        val zip = store.zip ?: throw RefreshException("No ZIP set yet.")
        val at = zips.lookup(zip) ?: throw RefreshException("ZIP $zip isn't in the table.")
        try {
            val (pointsJson, cachedStation) = store.cachedPoints(zip) ?: run {
                val json = noaa.points(at)
                // Stations come nearest first; only the nearest is needed.
                val station = NoaaParser.nearestStation(noaa.get(NoaaParser.points(json).stationsUrl + "?limit=1"))
                store.savePoints(zip, json, station)
                json to station
            }
            val points = NoaaParser.points(pointsJson)
            val forecast = noaa.get(points.forecastUrl)
            val hourly = noaa.get(points.hourlyUrl)
            val alerts = noaa.alerts(at)
            // A station that's down shouldn't stop the forecast: the hourly forecast stands in.
            val observation = cachedStation?.let { runCatching { noaa.latestObservation(it) }.getOrNull() }
            // Parse before saving, so a broken answer never replaces a good forecast.
            NoaaParser.periods(forecast); NoaaParser.periods(hourly); NoaaParser.alerts(alerts)
            store.save(zip, forecast, hourly, observation, alerts, Instant.now())
        } catch (e: RefreshException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // the screen closed; not a failure
        } catch (e: Exception) {
            throw RefreshException("Couldn't reach the National Weather Service.", e)
        }
        store.load() ?: throw RefreshException("The forecast couldn't be read.")
    }
}

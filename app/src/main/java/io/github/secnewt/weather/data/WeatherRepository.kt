package io.github.secnewt.weather.data

import android.content.Context
import io.github.secnewt.weather.share.ForecastShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/** Why a refresh didn't work, in words for the screen. */
class RefreshException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Fetches a fresh forecast for a saved place (the one showing, unless told otherwise) and stores it. */
class WeatherRepository(
    private val context: Context,
    private val store: WeatherStore = WeatherStore(context),
    private val noaa: NoaaClient = NoaaClient(),
) {

    /** Runs on a background thread: it writes files as well as fetching. */
    suspend fun refresh(location: Location? = null): Forecast = withContext(Dispatchers.IO) {
        var place = location ?: store.active ?: throw RefreshException("No place set yet.")
        val at = place.coordinates
        try {
            val (pointsJson, cachedStation) = store.cachedPoints(place) ?: run {
                val json = noaa.points(at)
                // Stations come nearest first; only the nearest is needed.
                val station = NoaaParser.nearestStation(noaa.get(NoaaParser.points(json).stationsUrl + "?limit=1"))
                store.savePoints(place, json, station)
                json to station
            }
            val points = NoaaParser.points(pointsJson)
            // A place added by ZIP takes NOAA's name for it ("Paris, TX").
            if (place.name.isEmpty() && points.place.label.isNotEmpty()) place = store.rename(place, points.place.label)
            val forecast = noaa.get(points.forecastUrl)
            val hourly = noaa.get(points.hourlyUrl)
            val alerts = noaa.alerts(at)
            // A station that's down shouldn't stop the forecast: the hourly forecast stands in.
            val observation = cachedStation?.let { runCatching { noaa.latestObservation(it) }.getOrNull() }
            // Parse before saving, so a broken answer never replaces a good forecast.
            NoaaParser.periods(forecast); NoaaParser.periods(hourly); NoaaParser.alerts(alerts)
            store.save(place, forecast, hourly, observation, alerts, Instant.now())
            // Newt Home redraws the weather as soon as a fresh forecast lands.
            ForecastShare.notifyChanged(context)
        } catch (e: RefreshException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // the screen closed; not a failure
        } catch (e: Exception) {
            throw RefreshException("Couldn't reach the National Weather Service.", e)
        }
        store.load(place) ?: throw RefreshException("The forecast couldn't be read.")
    }
}

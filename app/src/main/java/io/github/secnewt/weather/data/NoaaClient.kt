package io.github.secnewt.weather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the National Weather Service (api.weather.gov), the only place this app connects to.
 * HTTPS only: Android blocks plain http for this app.
 */
class NoaaClient(private val baseUrl: String = "https://api.weather.gov") {

    suspend fun points(at: Coordinates): String = get("$baseUrl/points/${at.latitude},${at.longitude}")

    suspend fun alerts(at: Coordinates): String = get("$baseUrl/alerts/active?point=${at.latitude},${at.longitude}")

    suspend fun latestObservation(station: String): String = get("$baseUrl/stations/$station/observations/latest")

    /** Any link NOAA handed back (forecast, hourly, stations), as long as it's NOAA's own. */
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        require(url.startsWith("$baseUrl/")) { "Not a NOAA link: $url" }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            // NOAA asks every app to say who it is, so they can reach the developer if it misbehaves.
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/geo+json")
            val status = connection.responseCode
            if (status !in 200..299) throw IOException("NOAA answered $status for $url")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 20_000
        const val USER_AGENT = "NewtWeather (https://github.com/sec-newt/newt-weather)"
    }
}

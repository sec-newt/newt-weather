package io.github.secnewt.weather.data

import android.content.Context
import java.io.File
import java.time.Instant

/**
 * Keeps the ZIP and NOAA's last answers in the app's private storage (never backed up),
 * so the screens show the last forecast even offline. The raw responses are saved and
 * parsed again on load, so there's no second format to keep in step with the parser.
 */
class WeatherStore(context: Context) {

    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "noaa").apply { mkdirs() }

    var zip: String?
        get() = prefs.getString(KEY_ZIP, null)
        set(value) { prefs.edit().putString(KEY_ZIP, value).apply() }

    /** Warnings already notified, so each one buzzes once. */
    var notifiedWarnings: Set<String>
        get() = prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
        set(value) { prefs.edit().putStringSet(KEY_NOTIFIED, value).apply() }

    /** The points answer and station for a ZIP rarely change, so they're reused until the ZIP does. */
    fun cachedPoints(zip: String): Pair<String, String?>? {
        if (prefs.getString(KEY_POINTS_ZIP, null) != zip) return null
        val points = read(POINTS) ?: return null
        return points to prefs.getString(KEY_STATION, null)
    }

    fun savePoints(zip: String, pointsJson: String, station: String?) {
        write(POINTS, pointsJson)
        prefs.edit().putString(KEY_POINTS_ZIP, zip).putString(KEY_STATION, station).apply()
    }

    fun save(zip: String, forecast: String, hourly: String, observation: String?, alerts: String, at: Instant) {
        write(FORECAST, forecast)
        write(HOURLY, hourly)
        if (observation != null) write(OBSERVATION, observation) else File(dir, OBSERVATION).delete()
        write(ALERTS, alerts)
        prefs.edit().putString(KEY_FORECAST_ZIP, zip).putLong(KEY_FETCHED, at.toEpochMilli()).apply()
    }

    /** The last forecast for the current ZIP, or null if there isn't one yet (or it can't be read). */
    fun load(): Forecast? {
        val zip = zip ?: return null
        if (prefs.getString(KEY_FORECAST_ZIP, null) != zip) return null
        return runCatching {
            val points = NoaaParser.points(read(POINTS)!!)
            Forecast(
                zip = zip,
                place = points.place,
                observation = read(OBSERVATION)?.let { NoaaParser.observation(it) },
                periods = NoaaParser.periods(read(FORECAST)!!),
                hourly = NoaaParser.periods(read(HOURLY)!!),
                alerts = read(ALERTS)?.let { NoaaParser.alerts(it) }.orEmpty(),
                fetchedAt = Instant.ofEpochMilli(prefs.getLong(KEY_FETCHED, 0)),
            )
        }.getOrNull()
    }

    private fun read(name: String): String? = File(dir, name).takeIf { it.exists() }?.readText()

    /** Write to a temporary file, then swap it in, so a half-written file is never read. */
    private fun write(name: String, text: String) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(dir, name))
    }

    private companion object {
        const val KEY_ZIP = "zip"
        const val KEY_NOTIFIED = "notified_warnings"
        const val KEY_POINTS_ZIP = "points_zip"
        const val KEY_STATION = "station"
        const val KEY_FORECAST_ZIP = "forecast_zip"
        const val KEY_FETCHED = "fetched_at"
        const val POINTS = "points.json"
        const val FORECAST = "forecast.json"
        const val HOURLY = "hourly.json"
        const val OBSERVATION = "observation.json"
        const val ALERTS = "alerts.json"
    }
}

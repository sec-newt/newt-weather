package io.github.secnewt.weather.data

import android.content.Context
import java.io.File
import java.time.Instant

/**
 * Keeps the saved places, which one is showing, and NOAA's last answers for each place in
 * the app's private storage (never backed up), so the screens show the last forecast even
 * offline, and switching back to a place shows its forecast straight away. The raw responses
 * are saved and parsed again on load, so there's no second format to keep in step with the
 * parser.
 */
class WeatherStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "noaa").apply { mkdirs() }

    init {
        migrateSingleZip()
    }

    /** Saved places, in the order they were added. */
    var locations: List<Location>
        get() = Location.decodeAll(prefs.getString(KEY_LOCATIONS, null).orEmpty())
        private set(value) { prefs.edit().putString(KEY_LOCATIONS, Location.encodeAll(value)).apply() }

    /** The place the forecast is showing for (and the one Newt Home and warnings follow). */
    val active: Location?
        get() {
            val all = locations
            val key = prefs.getString(KEY_ACTIVE, null)
            return all.firstOrNull { it.key == key } ?: all.firstOrNull()
        }

    /** Saves a place (once: the same spot again is the one already saved) and shows it. */
    fun add(location: Location): Location = synchronized(LOCK) {
        val existing = locations.firstOrNull { it.key == location.key }
        if (existing == null) locations = locations + location
        (existing ?: location).also { activate(it) }
    }

    fun activate(location: Location) {
        prefs.edit().putString(KEY_ACTIVE, location.key).apply()
    }

    /**
     * Forgets a place. If it was showing, the first place left shows instead. Quick, so the
     * screen can call it; its saved forecast goes with [deleteFiles], off the main thread.
     */
    fun remove(location: Location) = synchronized(LOCK) {
        val wasActive = active?.key == location.key
        locations = locations.filterNot { it.key == location.key }
        if (wasActive) {
            val next = locations.firstOrNull()
            prefs.edit().apply { if (next != null) putString(KEY_ACTIVE, next.key) else remove(KEY_ACTIVE) }.apply()
        }
    }

    /** Deletes a removed place's saved forecast (kept if the place was added again meanwhile). */
    fun deleteFiles(location: Location) = synchronized(LOCK) {
        if (!isSaved(location)) dir(location).deleteRecursively()
    }

    /** Gives a ZIP-only place NOAA's name for it. */
    fun rename(location: Location, name: String): Location = synchronized(LOCK) {
        val renamed = location.copy(name = name)
        // A place removed while its forecast was loading stays removed.
        if (isSaved(location)) locations = locations.map { if (it.key == location.key) renamed else it }
        renamed
    }

    /**
     * Warnings already notified for a place, so each one buzzes once. Kept per place, so
     * showing another place doesn't make the first one's warnings buzz again on the way back.
     */
    fun notifiedWarnings(location: Location): Set<String> =
        read(dir(location), NOTIFIED)?.lines()?.filter { it.isNotEmpty() }?.toSet().orEmpty()

    fun saveNotifiedWarnings(location: Location, ids: Set<String>) = synchronized(LOCK) {
        if (isSaved(location)) write(dir(location), NOTIFIED, ids.joinToString("\n"))
    }

    /** Whether a place is still saved: a refresh that finishes after it was removed must not bring its folder back. */
    fun isSaved(location: Location): Boolean = locations.any { it.key == location.key }

    /** Whether the notification permission has been asked for, to tell "never asked" from "said no". */
    var askedNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) { prefs.edit().putBoolean(KEY_ASKED, value).apply() }

    /** The Warning alerts switch, or null if it's never been touched (then it follows the permission). */
    var warningAlerts: Boolean?
        get() = if (prefs.contains(KEY_WARNING_ALERTS)) prefs.getBoolean(KEY_WARNING_ALERTS, false) else null
        set(value) {
            if (value == null) prefs.edit().remove(KEY_WARNING_ALERTS).apply() else prefs.edit().putBoolean(KEY_WARNING_ALERTS, value).apply()
        }

    /** The points answer and station for a place rarely change, so they're reused. */
    fun cachedPoints(location: Location): Pair<String, String?>? {
        val dir = dir(location)
        val points = read(dir, POINTS) ?: return null
        return points to read(dir, STATION)?.ifEmpty { null }
    }

    fun savePoints(location: Location, pointsJson: String, station: String?) = synchronized(LOCK) {
        if (!isSaved(location)) return
        val dir = dir(location)
        write(dir, POINTS, pointsJson)
        write(dir, STATION, station.orEmpty())
    }

    fun save(location: Location, forecast: String, hourly: String, observation: String?, alerts: String, at: Instant) = synchronized(LOCK) {
        if (!isSaved(location)) return
        val dir = dir(location)
        write(dir, FORECAST, forecast)
        write(dir, HOURLY, hourly)
        if (observation != null) write(dir, OBSERVATION, observation) else File(dir, OBSERVATION).delete()
        write(dir, ALERTS, alerts)
        write(dir, FETCHED, at.toEpochMilli().toString())
    }

    /** The last forecast for the place showing, or null if there isn't one yet (or it can't be read). */
    fun load(): Forecast? = active?.let { load(it) }

    // Under the lock, so the files all come from one save: an old forecast never gets a new time.
    fun load(location: Location): Forecast? = synchronized(LOCK) {
        val dir = dir(location)
        runCatching {
            val points = NoaaParser.points(read(dir, POINTS)!!)
            Forecast(
                location = location,
                place = points.place,
                observation = read(dir, OBSERVATION)?.let { NoaaParser.observation(it) },
                periods = NoaaParser.periods(read(dir, FORECAST)!!),
                hourly = NoaaParser.periods(read(dir, HOURLY)!!),
                alerts = read(dir, ALERTS)?.let { NoaaParser.alerts(it) }.orEmpty(),
                fetchedAt = Instant.ofEpochMilli(read(dir, FETCHED)!!.toLong()),
            )
        }.getOrNull()
    }

    private fun dir(location: Location) = File(root, location.key)

    private fun read(dir: File, name: String): String? = File(dir, name).takeIf { it.exists() }?.readText()

    /** Write to a temporary file, then swap it in, so a half-written file is never read. */
    private fun write(dir: File, name: String, text: String) {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(text)
        tmp.renameTo(File(dir, name))
    }

    /**
     * Before places, the app kept one ZIP and one forecast. That ZIP becomes the first saved
     * place, and its forecast moves into the place's folder, so an update loses nothing.
     */
    private fun migrateSingleZip() = synchronized(LOCK) {
        // Under the lock, so the app, the background refresh and Newt Home's read can't run it twice.
        val zip = prefs.getString(OLD_ZIP, null) ?: return
        val at = if (prefs.contains(KEY_LOCATIONS)) null else ZipTable(context).lookup(zip)
        if (at != null) {
            val pointsAreForZip = prefs.getString(OLD_POINTS_ZIP, null) == zip
            val name = if (pointsAreForZip) {
                File(root, POINTS).takeIf { it.exists() }
                    ?.let { runCatching { NoaaParser.points(it.readText()).place.label }.getOrNull() }
            } else {
                null
            }
            val location = Location(name.orEmpty(), zip, at)
            val dir = dir(location).apply { mkdirs() }
            if (pointsAreForZip) {
                File(root, POINTS).renameTo(File(dir, POINTS))
                write(dir, STATION, prefs.getString(OLD_STATION, null).orEmpty())
            }
            if (prefs.getString(OLD_FORECAST_ZIP, null) == zip) {
                listOf(FORECAST, HOURLY, OBSERVATION, ALERTS).forEach { File(root, it).renameTo(File(dir, it)) }
                write(dir, FETCHED, prefs.getLong(OLD_FETCHED, 0).toString())
            }
            write(dir, NOTIFIED, prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().joinToString("\n"))
            locations = listOf(location)
            activate(location)
        }
        root.listFiles { file -> file.isFile }?.forEach { it.delete() }
        prefs.edit()
            .remove(OLD_ZIP).remove(KEY_NOTIFIED).remove(OLD_POINTS_ZIP).remove(OLD_STATION).remove(OLD_FORECAST_ZIP).remove(OLD_FETCHED)
            .apply()
    }

    private companion object {
        /** One lock for the whole app: the screen, the background refresh and Newt Home's reads share these files. */
        val LOCK = Any()
        const val KEY_LOCATIONS = "locations"
        const val KEY_ACTIVE = "active_location"
        const val KEY_NOTIFIED = "notified_warnings"
        const val KEY_ASKED = "asked_notifications"
        const val KEY_WARNING_ALERTS = "warning_alerts"
        const val OLD_ZIP = "zip"
        const val OLD_POINTS_ZIP = "points_zip"
        const val OLD_STATION = "station"
        const val OLD_FORECAST_ZIP = "forecast_zip"
        const val OLD_FETCHED = "fetched_at"
        const val POINTS = "points.json"
        const val STATION = "station"
        const val FORECAST = "forecast.json"
        const val HOURLY = "hourly.json"
        const val OBSERVATION = "observation.json"
        const val ALERTS = "alerts.json"
        const val FETCHED = "fetched_at"
        const val NOTIFIED = "notified_warnings"
    }
}

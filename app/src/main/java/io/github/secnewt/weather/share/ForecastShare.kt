package io.github.secnewt.weather.share

import android.net.Uri
import io.github.secnewt.weather.data.Forecast
import java.time.OffsetDateTime

/**
 * What Newt Weather shares with other apps signed with the Newt key (Newt Home), and in what
 * shape. Only what a home screen shows: temperatures, conditions and active warnings. Not the
 * ZIP, the place name or NOAA's text, which stay in this app.
 *
 * Readers need the signature permission `<package>.permission.READ_FORECAST` and query:
 *   content://<package>.forecast/summary  → 0 rows (no forecast yet) or 1 row of SUMMARY_COLUMNS
 *   content://<package>.forecast/hourly   → up to 24 rows of HOURLY_COLUMNS, from this hour on
 * Values are worked out when queried, so they stay right between refreshes. Readers are told
 * (ContentResolver.notifyChange on the summary link) whenever a refresh lands.
 * Columns are only ever added, never renamed or removed; VERSION goes up when they are.
 */
object ForecastShare {
    const val VERSION = 1

    const val PATH_SUMMARY = "summary"
    const val PATH_HOURLY = "hourly"

    // Summary
    const val TEMPERATURE = "temperature"         // Int °F, or null if unknown
    const val HIGH_LOW = "high_low"               // Int °F: today's high or tonight's low
    const val IS_HIGH = "is_high"                 // 1 = high (daytime period), 0 = low
    const val CONDITION = "condition"             // Condition name: CLEAR, RAIN, THUNDERSTORM…
    const val IS_DAYTIME = "is_daytime"           // 1 / 0, for sun or moon icons
    const val DESCRIPTION = "description"         // NOAA's short words, like "Mostly Sunny"
    const val WARNINGS = "warnings"               // Int: active Warnings (not watches/advisories)
    const val WARNING_EVENT = "warning_event"     // the first one's name, like "Tornado Warning", or null
    const val UPDATED_AT = "updated_at"           // Long epoch millis of the last refresh
    const val SCHEMA = "version"                  // Int, VERSION

    val SUMMARY_COLUMNS = arrayOf(
        TEMPERATURE, HIGH_LOW, IS_HIGH, CONDITION, IS_DAYTIME, DESCRIPTION, WARNINGS, WARNING_EVENT, UPDATED_AT, SCHEMA,
    )

    // Hourly
    const val START = "start"                     // Long epoch millis of the hour
    const val HOUR_TEMPERATURE = "temperature"    // Int °F
    const val HOUR_CONDITION = "condition"
    const val HOUR_IS_DAYTIME = "is_daytime"
    const val PRECIP_CHANCE = "precip_chance"     // Int percent, or null

    val HOURLY_COLUMNS = arrayOf(START, HOUR_TEMPERATURE, HOUR_CONDITION, HOUR_IS_DAYTIME, PRECIP_CHANCE)

    const val HOURS = 24

    fun authority(packageName: String) = "$packageName.forecast"

    fun summaryUri(packageName: String): Uri = Uri.parse("content://${authority(packageName)}/$PATH_SUMMARY")

    fun permission(packageName: String) = "$packageName.permission.READ_FORECAST"

    /** The summary row, in SUMMARY_COLUMNS order. */
    fun summaryRow(forecast: Forecast, now: OffsetDateTime): Array<Any?> {
        val highLow = forecast.highLow(now)
        val warnings = forecast.alerts.filter { it.isWarning && (it.ends == null || it.ends > now) }
        return arrayOf(
            forecast.currentTemperature(now),
            highLow?.temperature,
            highLow?.let { if (it.isHigh) 1 else 0 },
            forecast.currentCondition(now)?.name,
            if (forecast.currentIsDaytime(now)) 1 else 0,
            forecast.currentDescription(now),
            warnings.size,
            warnings.firstOrNull()?.event,
            forecast.fetchedAt.toEpochMilli(),
            VERSION,
        )
    }

    /** The next HOURS hours, from the current one, in HOURLY_COLUMNS order. */
    fun hourlyRows(forecast: Forecast, now: OffsetDateTime): List<Array<Any?>> =
        forecast.hoursFrom(now).take(HOURS).map { hour ->
            arrayOf(
                hour.start.toInstant().toEpochMilli(),
                hour.temperature,
                hour.condition.name,
                if (hour.isDaytime) 1 else 0,
                hour.precipChance,
            )
        }
}

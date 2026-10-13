package io.github.secnewt.weather.data

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

/** The handful of weather kinds Newt shows an icon for. */
enum class Condition { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, THUNDERSTORM, SNOW, FOG, WIND }

/** Where the forecast is for, as NOAA names it. */
data class Place(val city: String, val state: String) {
    val label: String get() = if (state.isEmpty()) city else "$city, $state"
}

/** One forecast period: a day or night in the 7-day forecast, or one hour in the hourly one. */
data class Period(
    /** "Tonight", "Wednesday"… (empty for hourly periods). */
    val name: String,
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val isDaytime: Boolean,
    /** Degrees Fahrenheit. */
    val temperature: Int,
    val condition: Condition,
    val shortForecast: String,
    val detailedForecast: String,
    /** Chance of rain or snow, in percent. */
    val precipChance: Int?,
    /** Relative humidity in percent (hourly periods only). */
    val humidity: Int?,
    /** Like "S 15 mph". */
    val wind: String,
)

/** The latest reading from the nearest weather station. */
data class Observation(
    val temperature: Int,
    val observedAt: OffsetDateTime,
    val description: String,
)

/** A NOAA alert. Only Warnings (danger now) make a notification. */
data class Alert(
    val id: String,
    val event: String,
    val headline: String,
    val description: String,
    val instruction: String,
    val ends: OffsetDateTime?,
) {
    val isWarning: Boolean get() = event.endsWith("Warning")
}

/** Today's high or tonight's low, whichever NOAA's current forecast period is. */
data class HighLow(val temperature: Int, val isHigh: Boolean)

/** Everything the screens need, from the last successful refresh. */
data class Forecast(
    val zip: String,
    val place: Place,
    val observation: Observation?,
    /** The 7-day forecast, day and night periods in order. */
    val periods: List<Period>,
    val hourly: List<Period>,
    val alerts: List<Alert>,
    val fetchedAt: Instant,
) {
    /** The station reading if it's recent, otherwise this hour's forecast. */
    fun currentTemperature(now: OffsetDateTime): Int? {
        val reading = observation?.takeIf { Duration.between(it.observedAt, now) <= MAX_READING_AGE }
        return reading?.temperature ?: hourAt(now)?.temperature
    }

    /** The condition right now, from this hour's forecast. */
    fun currentCondition(now: OffsetDateTime): Condition? =
        (hourAt(now) ?: periods.firstOrNull())?.condition

    fun currentDescription(now: OffsetDateTime): String =
        hourAt(now)?.shortForecast ?: periods.firstOrNull()?.shortForecast.orEmpty()

    /** "Today" means a high is coming; "Tonight" means a low. */
    fun highLow(now: OffsetDateTime): HighLow? {
        val period = periods.firstOrNull { it.end > now } ?: return null
        return HighLow(period.temperature, isHigh = period.isDaytime)
    }

    /** Hours from the current one onward, for the hourly strip. */
    fun hoursFrom(now: OffsetDateTime): List<Period> = hourly.filter { it.end > now }

    /** The 7-day forecast as days, each with its daytime and night periods. */
    fun days(): List<Day> {
        val days = mutableListOf<Day>()
        var i = 0
        while (i < periods.size) {
            val p = periods[i]
            if (p.isDaytime) {
                val night = periods.getOrNull(i + 1)?.takeIf { !it.isDaytime }
                days += Day(p.name, p, night)
                i += if (night != null) 2 else 1
            } else {
                // The list starts at night (it's evening now): "Tonight" stands alone.
                days += Day(p.name, null, p)
                i += 1
            }
        }
        return days
    }

    private fun hourAt(now: OffsetDateTime): Period? =
        hourly.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }

    companion object {
        val MAX_READING_AGE: Duration = Duration.ofHours(2)
    }
}

/** One row of the 7-day forecast. Either half can be missing at the ends of the list. */
data class Day(val name: String, val day: Period?, val night: Period?)

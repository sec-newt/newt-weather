package io.github.secnewt.weather.data

import org.json.JSONObject
import java.time.OffsetDateTime
import kotlin.math.roundToInt

/** What /points/{lat},{lon} says about a place: its name and where its forecasts live. */
data class Points(
    val place: Place,
    val forecastUrl: String,
    val hourlyUrl: String,
    val stationsUrl: String,
)

/** Turns NOAA's GeoJSON responses into Newt's models. Pure functions, so they're easy to test. */
object NoaaParser {

    fun points(json: String): Points {
        val props = JSONObject(json).getJSONObject("properties")
        val where = props.optJSONObject("relativeLocation")?.optJSONObject("properties")
        return Points(
            place = Place(where?.optString("city").orEmpty(), where?.optString("state").orEmpty()),
            forecastUrl = props.getString("forecast"),
            hourlyUrl = props.getString("forecastHourly"),
            stationsUrl = props.getString("observationStations"),
        )
    }

    /** The 7-day or hourly forecast. */
    fun periods(json: String): List<Period> {
        val list = JSONObject(json).getJSONObject("properties").getJSONArray("periods")
        return (0 until list.length()).map { i ->
            val p = list.getJSONObject(i)
            val isDaytime = p.optBoolean("isDaytime", true)
            val short = p.optString("shortForecast")
            val unit = p.optString("temperatureUnit", "F")
            val temp = p.getInt("temperature")
            Period(
                name = p.optString("name"),
                start = OffsetDateTime.parse(p.getString("startTime")),
                end = OffsetDateTime.parse(p.getString("endTime")),
                isDaytime = isDaytime,
                temperature = if (unit == "C") celsiusToFahrenheit(temp.toDouble()) else temp,
                condition = condition(p.optString("icon"), short),
                shortForecast = short,
                detailedForecast = p.optString("detailedForecast"),
                precipChance = percent(p, "probabilityOfPrecipitation"),
                humidity = percent(p, "relativeHumidity"),
                wind = listOf(p.optString("windDirection"), p.optString("windSpeed"))
                    .filter { it.isNotBlank() }.joinToString(" "),
            )
        }
    }

    /** The first (nearest) station's ID, like "KOKC". */
    fun nearestStation(json: String): String? {
        val features = JSONObject(json).optJSONArray("features") ?: return null
        if (features.length() == 0) return null
        return features.getJSONObject(0).getJSONObject("properties").optString("stationIdentifier").ifEmpty { null }
    }

    /** The station's latest reading, or null if it has no temperature (stations do skip it). */
    fun observation(json: String): Observation? {
        val props = JSONObject(json).getJSONObject("properties")
        val temp = props.optJSONObject("temperature") ?: return null
        if (temp.isNull("value")) return null
        val value = temp.getDouble("value")
        val fahrenheit = if (temp.optString("unitCode").endsWith("degF")) value.roundToInt() else celsiusToFahrenheit(value)
        return Observation(
            temperature = fahrenheit,
            observedAt = OffsetDateTime.parse(props.getString("timestamp")),
            description = props.optString("textDescription"),
        )
    }

    fun alerts(json: String): List<Alert> {
        val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
        return (0 until features.length()).map { i ->
            val p = features.getJSONObject(i).getJSONObject("properties")
            val ends = p.optString("ends").ifEmpty { p.optString("expires") }
            Alert(
                id = p.getString("id"),
                event = p.optString("event"),
                headline = p.optString("headline"),
                description = p.optString("description"),
                instruction = if (p.isNull("instruction")) "" else p.optString("instruction"),
                ends = ends.takeIf { it.isNotEmpty() && it != "null" }?.let { OffsetDateTime.parse(it) },
            )
        }
    }

    /**
     * NOAA's icon link carries a condition code: ".../icons/land/night/tsra_hi,20/tsra_hi,30?size=medium"
     * → "tsra_hi". If that format ever changes, the short forecast text decides instead.
     */
    fun condition(iconUrl: String, shortForecast: String): Condition =
        iconCode(iconUrl)?.let { CODES[it] } ?: conditionFromText(shortForecast)

    internal fun iconCode(iconUrl: String): String? {
        val path = iconUrl.substringBefore('?')
        val marker = listOf("/land/day/", "/land/night/").firstOrNull { it in path } ?: return null
        return path.substringAfter(marker).substringBefore('/').substringBefore(',').ifEmpty { null }
    }

    internal fun conditionFromText(text: String): Condition {
        val t = text.lowercase()
        return when {
            "thunder" in t || "tornado" in t || "hurricane" in t || "tropical storm" in t -> Condition.THUNDERSTORM
            listOf("snow", "sleet", "ice", "freezing", "flurr", "blizzard").any { it in t } -> Condition.SNOW
            listOf("rain", "shower", "drizzle").any { it in t } -> Condition.RAIN
            listOf("fog", "haze", "smoke", "dust").any { it in t } -> Condition.FOG
            listOf("windy", "breezy", "blustery").any { it in t } -> Condition.WIND
            "partly" in t || "mostly sunny" in t || "mostly clear" in t -> Condition.PARTLY_CLOUDY
            "cloud" in t || "overcast" in t -> Condition.CLOUDY
            else -> Condition.CLEAR
        }
    }

    fun celsiusToFahrenheit(c: Double): Int = (c * 9.0 / 5.0 + 32.0).roundToInt()

    private fun percent(p: JSONObject, key: String): Int? {
        val o = p.optJSONObject(key) ?: return null
        return if (o.isNull("value")) null else o.getDouble("value").roundToInt()
    }

    /** NOAA's icon codes (https://api.weather.gov/icons) → the icon Newt shows. */
    private val CODES: Map<String, Condition> = buildMap {
        listOf("skc", "few", "hot", "cold").forEach { put(it, Condition.CLEAR) }
        put("sct", Condition.PARTLY_CLOUDY)
        listOf("bkn", "ovc").forEach { put(it, Condition.CLOUDY) }
        listOf("wind_skc", "wind_few", "wind_sct", "wind_bkn", "wind_ovc").forEach { put(it, Condition.WIND) }
        listOf("rain", "rain_showers", "rain_showers_hi").forEach { put(it, Condition.RAIN) }
        listOf("tsra", "tsra_sct", "tsra_hi", "tornado", "hurricane", "tropical_storm").forEach { put(it, Condition.THUNDERSTORM) }
        listOf("snow", "rain_snow", "rain_sleet", "snow_sleet", "fzra", "rain_fzra", "snow_fzra", "sleet", "blizzard")
            .forEach { put(it, Condition.SNOW) }
        listOf("fog", "haze", "smoke", "dust").forEach { put(it, Condition.FOG) }
    }
}

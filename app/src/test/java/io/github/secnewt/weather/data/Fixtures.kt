package io.github.secnewt.weather.data

import java.time.Instant
import java.time.OffsetDateTime

/** Real National Weather Service answers for Oklahoma City, saved 2026-10-13 (src/test/resources/noaa). */
object Fixtures {
    fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("noaa/$name")) { "missing fixture $name" }.readText()

    /** 11:30 AM in Oklahoma City, ten minutes after the station's reading. */
    val NOON: OffsetDateTime = OffsetDateTime.parse("2026-10-13T11:30:00-05:00")

    /** 7:30 PM the same day: the forecast's "Tonight" period. */
    val EVENING: OffsetDateTime = OffsetDateTime.parse("2026-10-13T19:30:00-05:00")

    fun forecast(alerts: Boolean = true): Forecast = Forecast(
        zip = "73102",
        place = NoaaParser.points(read("points.json")).place,
        observation = NoaaParser.observation(read("observation.json")),
        periods = NoaaParser.periods(read("forecast.json")),
        hourly = NoaaParser.periods(read("hourly.json")),
        alerts = if (alerts) NoaaParser.alerts(read("alerts.json")) else emptyList(),
        fetchedAt = Instant.parse("2026-10-13T16:25:00Z"),
    )
}

package io.github.secnewt.weather.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.secnewt.weather.data.Fixtures.read
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs under Robolectric because Android's org.json does the parsing. */
@RunWith(AndroidJUnit4::class)
class NoaaParserTest {

    @Test
    fun pointsGiveThePlaceAndForecastLinks() {
        val points = NoaaParser.points(read("points.json"))
        assertEquals("Oklahoma City, OK", points.place.label)
        assertEquals("https://api.weather.gov/gridpoints/OUN/97,94/forecast", points.forecastUrl)
        assertEquals("https://api.weather.gov/gridpoints/OUN/97,94/forecast/hourly", points.hourlyUrl)
    }

    @Test
    fun sevenDayPeriods() {
        val periods = NoaaParser.periods(read("forecast.json"))
        assertEquals(14, periods.size)
        val today = periods[0]
        assertEquals("Today", today.name)
        assertTrue(today.isDaytime)
        assertEquals(90, today.temperature)
        assertEquals(Condition.PARTLY_CLOUDY, today.condition) // "sct", Mostly Sunny
        assertEquals(2, today.precipChance)
        assertEquals("S 15 mph", today.wind)
        val tonight = periods[1]
        assertFalse(tonight.isDaytime)
        assertEquals(75, tonight.temperature)
        assertEquals(Condition.THUNDERSTORM, tonight.condition) // "tsra_hi,20/tsra_hi,30"
        assertEquals(Condition.RAIN, periods[6].condition) // Friday: "rain_showers,20/wind_bkn"
    }

    @Test
    fun hourlyPeriodsHaveHumidity() {
        val hours = NoaaParser.periods(read("hourly.json"))
        assertEquals(36, hours.size)
        assertEquals(83, hours[0].temperature)
        assertEquals(70, hours[0].humidity)
        assertEquals("", hours[0].name)
    }

    @Test
    fun observationIsConvertedToFahrenheit() {
        val reading = NoaaParser.observation(read("observation.json"))!!
        assertEquals(82, reading.temperature) // 28 °C
    }

    @Test
    fun observationWithoutATemperatureIsSkipped() {
        val json = """{"properties":{"timestamp":"2026-10-13T16:20:00+00:00","temperature":{"unitCode":"wmoUnit:degC","value":null}}}"""
        assertNull(NoaaParser.observation(json))
    }

    @Test
    fun nearestStation() = assertEquals("KOKC", NoaaParser.nearestStation(read("stations.json")))

    @Test
    fun onlyWarningsAreWarnings() {
        val alerts = NoaaParser.alerts(read("alerts.json"))
        assertEquals(listOf("Flood Warning", "Flood Watch"), alerts.map { it.event })
        assertTrue(alerts[0].isWarning)
        assertFalse(alerts[1].isWarning)
        assertTrue(alerts[0].ends != null)
    }

    @Test
    fun iconCodes() {
        assertEquals("tsra_hi", NoaaParser.iconCode("https://api.weather.gov/icons/land/night/tsra_hi,20/tsra_hi,30?size=medium"))
        assertEquals("skc", NoaaParser.iconCode("https://api.weather.gov/icons/land/day/skc?size=small"))
        assertNull(NoaaParser.iconCode("https://example.com/something-else.png"))
    }

    @Test
    fun textDecidesWhenTheIconCantBeRead() {
        assertEquals(Condition.THUNDERSTORM, NoaaParser.condition("", "Chance Showers And Thunderstorms"))
        assertEquals(Condition.SNOW, NoaaParser.condition("", "Light Snow Likely"))
        assertEquals(Condition.RAIN, NoaaParser.condition("", "Slight Chance Rain Showers"))
        assertEquals(Condition.FOG, NoaaParser.condition("", "Patchy Fog"))
        assertEquals(Condition.PARTLY_CLOUDY, NoaaParser.condition("", "Mostly Sunny"))
        assertEquals(Condition.CLOUDY, NoaaParser.condition("", "Cloudy"))
        assertEquals(Condition.CLEAR, NoaaParser.condition("", "Sunny"))
    }
}

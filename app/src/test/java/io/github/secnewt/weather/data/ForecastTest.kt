package io.github.secnewt.weather.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForecastTest {

    private val forecast = Fixtures.forecast()

    @Test
    fun daytimeShowsTodaysHigh() {
        val highLow = forecast.highLow(Fixtures.NOON)!!
        assertTrue(highLow.isHigh)
        assertEquals(90, highLow.temperature)
    }

    @Test
    fun eveningShowsTonightsLow() {
        val highLow = forecast.highLow(Fixtures.EVENING)!!
        assertFalse(highLow.isHigh)
        assertEquals(75, highLow.temperature)
    }

    @Test
    fun aRecentStationReadingIsTheCurrentTemperature() =
        assertEquals(82, forecast.currentTemperature(Fixtures.NOON))

    @Test
    fun anOldReadingFallsBackToThisHoursForecast() {
        // 3 hours after the 11:20 reading: the 2 PM hourly forecast stands in.
        val later = Fixtures.NOON.plusHours(3)
        assertEquals(forecast.hourly.first { !later.isBefore(it.start) && later.isBefore(it.end) }.temperature,
            forecast.currentTemperature(later))
    }

    @Test
    fun hourlyStripStartsAtTheCurrentHour() {
        val hours = forecast.hoursFrom(Fixtures.NOON)
        assertEquals(11, hours.first().start.hour)
    }

    @Test
    fun daysPairDayAndNight() {
        val days = forecast.days()
        assertEquals(7, days.size)
        assertEquals("Today", days[0].name)
        assertEquals(90, days[0].day!!.temperature)
        assertEquals(75, days[0].night!!.temperature)
    }

    @Test
    fun anEveningListStartsWithTonightAlone() {
        val evening = forecast.copy(periods = forecast.periods.drop(1))
        val days = evening.days()
        assertEquals("Tonight", days[0].name)
        assertEquals(null, days[0].day)
    }
}

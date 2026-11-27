package io.github.secnewt.weather.ui

import java.time.LocalDate
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** An old forecast must show its age, not just a time that looks like today. */
class UpdatedTextTest {

    private val at = OffsetDateTime.parse("2026-11-25T09:05:00-06:00")

    @Test
    fun todayIsJustTheTime() = assertEquals("9:05 AM", updatedText(at, LocalDate.parse("2026-11-25")))

    @Test
    fun anEarlierDayHasTheDate() = assertEquals("Nov 25 at 9:05 AM", updatedText(at, LocalDate.parse("2026-11-27")))

    @Test
    fun anotherYearHasTheYear() = assertEquals("Nov 25, 2026 at 9:05 AM", updatedText(at, LocalDate.parse("2027-11-25")))
}

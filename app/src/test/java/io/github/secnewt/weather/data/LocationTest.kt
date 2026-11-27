package io.github.secnewt.weather.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTest {

    private val zipOnly = Location("", "73102", Coordinates("35.4707", "-97.5191"))
    private val named = zipOnly.copy(name = "Oklahoma City, OK")
    private val city = Location("Paris, TX", null, Coordinates("33.67", "-95.55"))

    @Test
    fun labels() {
        assertEquals("ZIP 73102", zipOnly.label)
        assertEquals("ZIP 73102", zipOnly.fullLabel)
        assertEquals("Oklahoma City, OK · ZIP 73102", named.fullLabel)
        assertEquals("Paris, TX", city.fullLabel)
    }

    @Test
    fun theSameSpotIsTheSamePlace() = assertEquals(zipOnly.key, named.key)

    @Test
    fun savedAndReadBack() {
        val all = listOf(zipOnly, named, city, city.copy(name = "A | B"))
        assertEquals(
            listOf(zipOnly, named, city, city.copy(name = "A   B")),
            Location.decodeAll(Location.encodeAll(all)),
        )
    }
}

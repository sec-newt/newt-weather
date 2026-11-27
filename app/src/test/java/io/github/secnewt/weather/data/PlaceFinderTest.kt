package io.github.secnewt.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceFinderTest {

    private val okc = Coordinates("35.4707", "-97.5191")
    private val paris = Location("Paris, TX", null, Coordinates("33.67", "-95.55"))
    private var cityAsked: String? = null

    private fun finder(cities: List<Location> = emptyList()) = PlaceFinder(
        zip = { if (it == "73102") okc else null },
        city = { cityAsked = it; cities },
    )

    @Test
    fun digitsAreAZip() =
        assertEquals(Lookup.Found(listOf(Location("", "73102", okc))), finder().find(" 73102 "))

    @Test
    fun zipPlusFourUsesTheFirstFive() {
        assertEquals(Lookup.Found(listOf(Location("", "73102", okc))), finder().find("73102-1234"))
        assertEquals(Lookup.Found(listOf(Location("", "73102", okc))), finder().find("731021234"))
    }

    @Test
    fun zipProblems() {
        assertEquals(Lookup.Problem(PlaceFinder.SHORT_ZIP), finder().find("7310"))
        assertEquals(Lookup.Problem(PlaceFinder.UNKNOWN_ZIP), finder().find("00000"))
        assertEquals(Lookup.Problem(PlaceFinder.EMPTY), finder().find("   "))
    }

    @Test
    fun wordsAreACity() {
        assertEquals(Lookup.Found(listOf(paris)), finder(listOf(paris)).find("Paris, TX"))
        assertEquals("Paris, TX", cityAsked)
    }

    @Test
    fun unknownCitySaysWhatToTry() {
        val found = finder().find("Gotham")
        assertTrue(found is Lookup.Problem && "Gotham" in found.message && "ZIP" in found.message)
    }

    @Test
    fun tooManyAreCutAndFlagged() {
        val many = List(CitySearch.LIMIT + 1) { paris.copy(name = "Springfield $it") }
        val found = finder(many).find("Springfield") as Lookup.Found
        assertEquals(CitySearch.LIMIT, found.places.size)
        assertTrue(found.more)
    }
}

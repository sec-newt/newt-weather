package io.github.secnewt.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The matching rules, on a small made-up table in the real file's shape. */
class CitySearchTest {

    private val rows = listOf(
        "# name|state|latitude|longitude",
        "Paris|AR|33.3610|-93.7142",
        "Paris|TX|33.6689|-95.5466",
        "Parisville|MS|32.2000|-89.1000",
        "Nashville|GA|31.2057|-83.2464",
        "Nashville-Davidson|TN|36.1700|-86.7800",
        "Urban Honolulu|HI|21.3243|-157.8476",
        "St. Louis|MO|38.6300|-90.2400",
        "Bel Air|MD|39.5300|-76.3500",
        "Utqiaġvik|AK|71.2644|-156.7537",
    )

    private fun names(query: String) = CitySearch.search(rows, query).map { it.name }

    @Test
    fun wholeNameBeatsStartOfName() = assertEquals(listOf("Paris, AR", "Paris, TX"), names("paris"))

    @Test
    fun stateNarrowsIt() {
        assertEquals(listOf("Paris, TX"), names("Paris, TX"))
        assertEquals(listOf("Paris, TX"), names("paris tx"))
        assertEquals(listOf("Paris, TX"), names("Paris, Texas"))
    }

    @Test
    fun coordinatesAreTrimmedForNoaa() =
        assertEquals(Coordinates("33.6689", "-95.5466"), CitySearch.search(rows, "Paris, TX").single().coordinates)

    @Test
    fun partOfAJoinedNameCountsAsTheWholeName() =
        assertEquals(listOf("Nashville, GA", "Nashville-Davidson, TN"), names("Nashville"))

    @Test
    fun startOfAWordFindsUrbanHonolulu() = assertEquals(listOf("Urban Honolulu, HI"), names("honolulu"))

    @Test
    fun startOfTheName() = assertEquals(listOf("Parisville, MS"), names("parisv"))

    @Test
    fun saintAndPeriodsDontMatter() {
        assertEquals(listOf("St. Louis, MO"), names("Saint Louis"))
        assertEquals(listOf("St. Louis, MO"), names("st louis mo"))
    }

    @Test
    fun accentsDontMatter() = assertEquals(listOf("Utqiaġvik, AK"), names("utqiagvik"))

    @Test
    fun aNameEndingInAStateCodeStillWorks() {
        // "Bel Air": "AIR" isn't a state, but "Bel Ar" would read as Bel in Arkansas; nothing there, so it's a name.
        assertEquals(listOf("Bel Air, MD"), names("Bel Air"))
    }

    @Test
    fun nothingFound() = assertTrue(names("Gotham").isEmpty())

    @Test
    fun oneLetterOnlyMatchesAWholeName() = assertTrue(names("g").isEmpty())

    @Test
    fun stateCodes() {
        assertEquals("TX", CitySearch.stateCode("tx"))
        assertEquals("ND", CitySearch.stateCode("North Dakota"))
        assertEquals(null, CitySearch.stateCode("XX"))
        assertEquals("fargo" to "ND", CitySearch.split("Fargo North Dakota"))
        assertEquals("new york" to null, CitySearch.split("New York"))
    }

    @Test
    fun longListsSayThereAreMore() {
        val many = (1..40).map { "Springfield|S$it|39.0|-90.0" }
        assertEquals(CitySearch.LIMIT + 1, CitySearch.search(many, "Springfield").size)
    }
}

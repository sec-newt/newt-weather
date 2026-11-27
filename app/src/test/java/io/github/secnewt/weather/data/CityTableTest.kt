package io.github.secnewt.weather.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The real table built into the app. */
@RunWith(AndroidJUnit4::class)
class CityTableTest {

    private val table = CityTable(ApplicationProvider.getApplicationContext())

    @Test
    fun paris() {
        val found = table.search("Paris, TX").single()
        assertEquals("Paris, TX", found.name)
        assertTrue(found.coordinates.latitude.startsWith("33.6"))
    }

    @Test
    fun theKindOfPlaceIsLeftOffTheName() {
        // The Census calls them "Oklahoma City city" and "Nashville-Davidson metropolitan government (balance)".
        assertEquals(listOf("Oklahoma City, OK"), table.search("oklahoma city").map { it.name })
        assertTrue("Nashville-Davidson, TN" in table.search("Nashville").map { it.name })
    }

    @Test
    fun springfieldNeedsAState() = assertTrue(table.search("Springfield").size > 5)
}

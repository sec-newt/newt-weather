package io.github.secnewt.weather.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZipTableTest {

    private val table = ZipTable(ApplicationProvider.getApplicationContext())

    @Test
    fun knownZip() = assertEquals(Coordinates("35.4707", "-97.5191"), table.lookup("73102"))

    @Test
    fun unknownZip() = assertNull(table.lookup("00000"))

    @Test
    fun onlyFiveDigitsCount() {
        assertFalse(ZipTable.isValid("7310"))
        assertFalse(ZipTable.isValid("7310a"))
        assertNull(table.lookup("731020"))
    }

    @Test
    fun trailingZerosAreDropped() {
        assertEquals("-66.75", ZipTable.trim("-66.7500"))
        assertEquals("18", ZipTable.trim("18.0000"))
    }
}

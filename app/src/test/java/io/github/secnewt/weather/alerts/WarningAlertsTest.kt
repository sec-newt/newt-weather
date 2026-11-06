package io.github.secnewt.weather.alerts

import org.junit.Assert.assertEquals
import org.junit.Test

class WarningAlertsTest {

    @Test
    fun `untouched, it follows the permission`() {
        assertEquals(true, WarningAlerts.switchOn(stored = null, granted = true))
        assertEquals(false, WarningAlerts.switchOn(stored = null, granted = false))
    }

    @Test
    fun `the switch shows what was chosen, even if Android says no`() {
        assertEquals(true, WarningAlerts.switchOn(stored = true, granted = false))
        assertEquals(false, WarningAlerts.switchOn(stored = false, granted = true))
    }

    @Test
    fun `notifies only when on and allowed`() {
        assertEquals(true, WarningAlerts.shouldNotify(stored = true, granted = true))
        assertEquals(true, WarningAlerts.shouldNotify(stored = null, granted = true))
        assertEquals(false, WarningAlerts.shouldNotify(stored = false, granted = true))
        assertEquals(false, WarningAlerts.shouldNotify(stored = true, granted = false))
    }
}

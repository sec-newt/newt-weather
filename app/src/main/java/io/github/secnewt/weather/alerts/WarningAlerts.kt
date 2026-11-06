package io.github.secnewt.weather.alerts

/**
 * The Warning alerts setting. Until it's touched it follows Android's notification permission;
 * after that it's what was chosen. Warnings notify only when it's on and Android allows it.
 */
object WarningAlerts {

    fun switchOn(stored: Boolean?, granted: Boolean): Boolean = stored ?: granted

    fun shouldNotify(stored: Boolean?, granted: Boolean): Boolean = switchOn(stored, granted) && granted
}

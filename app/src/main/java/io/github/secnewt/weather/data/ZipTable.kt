package io.github.secnewt.weather.data

import android.content.Context

/** Map coordinates, at the 4 decimal places NOAA accepts (written exactly as NOAA wants them). */
data class Coordinates(val latitude: String, val longitude: String)

/**
 * ZIP → coordinates, from the Census Bureau table built into the app
 * (assets/zip-coordinates.csv; rebuild with scripts/update-zip-table.sh). Looking a ZIP up
 * never goes online, so the ZIP itself never leaves the phone.
 */
class ZipTable(private val context: Context) {

    fun lookup(zip: String): Coordinates? {
        if (!isValid(zip)) return null
        val prefix = "$zip,"
        context.assets.open(ASSET).bufferedReader().useLines { lines ->
            val line = lines.firstOrNull { it.startsWith(prefix) } ?: return null
            val parts = line.split(',')
            if (parts.size < 3) return null
            return Coordinates(trim(parts[1]), trim(parts[2]))
        }
    }

    companion object {
        private const val ASSET = "zip-coordinates.csv"

        fun isValid(zip: String): Boolean = zip.length == 5 && zip.all { it.isDigit() }

        /** "35.4700" → "35.47": NOAA redirects requests with extra digits, so keep it minimal. */
        internal fun trim(number: String): String =
            if ('.' in number) number.trimEnd('0').trimEnd('.') else number
    }
}

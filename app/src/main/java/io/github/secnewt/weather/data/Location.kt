package io.github.secnewt.weather.data

/**
 * A saved place the forecast can be for. Added by city ("Paris, TX") or by ZIP; a ZIP's
 * [name] starts empty and is filled in with NOAA's name for it after the first forecast.
 */
data class Location(
    val name: String,
    val zip: String?,
    val coordinates: Coordinates,
) {
    /** Two saves of the same spot are the same place; also names its forecast folder. */
    val key: String get() = "${coordinates.latitude},${coordinates.longitude}"

    /** What the place is called on screen: its name, or its ZIP until NOAA names it. */
    val label: String get() = name.ifEmpty { "ZIP $zip" }

    /** The label plus the ZIP when there's a name too, like "Paris, TX · ZIP 75460". */
    val fullLabel: String get() = if (name.isNotEmpty() && zip != null) "$name · ZIP $zip" else label

    internal fun encode(): String =
        listOf(name.replace(SEPARATOR, " "), zip.orEmpty(), coordinates.latitude, coordinates.longitude)
            .joinToString(SEPARATOR)

    internal companion object {
        const val SEPARATOR = "|"

        fun decode(line: String): Location? {
            val parts = line.split(SEPARATOR)
            if (parts.size != 4 || parts[2].isEmpty() || parts[3].isEmpty()) return null
            return Location(parts[0], parts[1].ifEmpty { null }, Coordinates(parts[2], parts[3]))
        }

        fun encodeAll(locations: List<Location>): String = locations.joinToString("\n") { it.encode() }

        fun decodeAll(text: String): List<Location> = text.lines().mapNotNull { decode(it) }
    }
}

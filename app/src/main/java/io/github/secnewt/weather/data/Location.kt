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
        // The separator or a line break in a name would split the saved line; neither belongs in a name.
        listOf(name.replace(Regex("[|\\p{Cntrl}]"), " "), zip.orEmpty(), coordinates.latitude, coordinates.longitude)
            .joinToString(SEPARATOR)

    internal companion object {
        const val SEPARATOR = "|"
        private val NUMBER = Regex("-?\\d{1,3}(\\.\\d+)?")

        fun decode(line: String): Location? {
            val parts = line.split(SEPARATOR)
            // Coordinates also name the place's folder, so anything but a plain number is refused.
            if (parts.size != 4 || !parts[2].matches(NUMBER) || !parts[3].matches(NUMBER)) return null
            return Location(parts[0], parts[1].ifEmpty { null }, Coordinates(parts[2], parts[3]))
        }

        fun encodeAll(locations: List<Location>): String = locations.joinToString("\n") { it.encode() }

        fun decodeAll(text: String): List<Location> = text.lines().mapNotNull { decode(it) }
    }
}

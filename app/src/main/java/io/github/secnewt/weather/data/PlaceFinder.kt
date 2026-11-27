package io.github.secnewt.weather.data

/** What typing a city or ZIP found. */
sealed interface Lookup {
    /** One or more places; more than one means asking which. [more] means the list was cut short. */
    data class Found(val places: List<Location>, val more: Boolean = false) : Lookup

    /** Nothing usable, with what to do about it in words for the screen. */
    data class Problem(val message: String) : Lookup
}

/** Turns what was typed in the place box into places: digits are a ZIP, anything else a city. */
class PlaceFinder(
    private val zip: (String) -> Coordinates?,
    private val city: (String) -> List<Location>,
) {

    fun find(query: String): Lookup {
        val text = query.trim()
        if (text.isEmpty()) return Lookup.Problem(EMPTY)
        // ZIP+4 ("75460-1234" or "754601234") counts as its first five digits.
        if (text.all { it.isDigit() || it == '-' } && text.any { it.isDigit() }) {
            val first = text.substringBefore('-')
            val digits = if (first.length == 9) first.take(5) else first
            if (!ZipTable.isValid(digits)) return Lookup.Problem(SHORT_ZIP)
            val at = zip(digits) ?: return Lookup.Problem(UNKNOWN_ZIP)
            return Lookup.Found(listOf(Location("", digits, at)))
        }
        val found = city(text)
        if (found.isEmpty()) return Lookup.Problem(unknownCity(text))
        return Lookup.Found(found.take(CitySearch.LIMIT), more = found.size > CitySearch.LIMIT)
    }

    companion object {
        const val EMPTY = "Type a city or a ZIP code."
        const val SHORT_ZIP = "Enter all five digits of the ZIP."
        const val UNKNOWN_ZIP = "That ZIP isn't one we know. Check the five digits."

        fun unknownCity(typed: String) =
            "No US city or town called \"$typed\" in the list. Check the spelling, add the state " +
                "(like Paris, TX), or use a ZIP code."
    }
}

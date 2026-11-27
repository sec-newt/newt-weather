package io.github.secnewt.weather.data

import android.content.Context
import java.text.Normalizer

/**
 * City name → places, from the Census Bureau table built into the app
 * (assets/city-coordinates.csv; rebuild with scripts/update-city-table.sh). Like ZIPs,
 * looking a city up never goes online.
 */
class CityTable(private val context: Context) {

    fun search(query: String): List<Location> =
        CitySearch.search(context.assets.open(ASSET).bufferedReader().use { it.readLines() }, query)

    private companion object {
        const val ASSET = "city-coordinates.csv"
    }
}

/**
 * The matching itself, kept apart from the file so it's easy to test. Rows are
 * `name|ST|latitude|longitude`. Typed text can end with a state ("Paris, TX",
 * "Paris tx", "Paris, Texas"). The best kind of match wins: the whole name, or part of
 * a joined name ("Nashville" in "Nashville-Davidson", so it's listed with the other
 * Nashvilles); then names starting with what was typed; then any word in the name starting
 * with it ("Honolulu" in "Urban Honolulu").
 */
object CitySearch {

    /** Shown at most; more than this means the name needs its state (one extra is returned to tell). */
    const val LIMIT = 30

    fun search(rows: List<String>, query: String): List<Location> {
        val (name, state) = split(query)
        if (state != null) {
            val found = matches(rows, name, state)
            // "Bel Air" isn't "Bel" in Arizona: if the state reading finds nothing, try it as a name.
            if (found.isNotEmpty()) return found
        }
        return matches(rows, normalize(query), null)
    }

    private fun matches(rows: List<String>, name: String, state: String?): List<Location> {
        if (name.isEmpty()) return emptyList()
        val tiers = List(3) { mutableListOf<Location>() }
        for (row in rows) {
            if (row.startsWith("#")) continue
            val parts = row.split('|')
            if (parts.size != 4) continue
            if (state != null && parts[1] != state) continue
            val tier = tier(normalize(parts[0]), name) ?: continue
            // Only the best tier is ever shown, so worse ones stop collecting once full.
            if (tiers[tier].size <= LIMIT) {
                tiers[tier] += Location(
                    "${parts[0]}, ${parts[1]}", null,
                    Coordinates(ZipTable.trim(parts[2]), ZipTable.trim(parts[3])),
                )
            }
        }
        // Up to LIMIT + 1, so the caller can tell "exactly LIMIT" from "more than LIMIT".
        return tiers.firstOrNull { it.isNotEmpty() }.orEmpty()
    }

    /** 0 = the whole name, or part of a joined one; 1 = start of the name; 2 = start of a word. */
    private fun tier(candidate: String, typed: String): Int? = when {
        candidate == typed || candidate.split('-', '/').any { it.trim() == typed } -> 0
        typed.length < 2 -> null
        candidate.startsWith(typed) -> 1
        typed.length >= 3 && candidate.split(' ', '-', '/').any { it.startsWith(typed) } -> 2
        else -> null
    }

    /** "Paris, TX" → ("paris", "TX"); with no state, the state is null. */
    internal fun split(query: String): Pair<String, String?> {
        val text = query.trim()
        val comma = text.lastIndexOf(',')
        if (comma >= 0) {
            val state = stateCode(text.substring(comma + 1))
            if (state != null) return normalize(text.substring(0, comma)) to state
        }
        // No comma: the last one or two words may be a state ("Paris TX", "Fargo North Dakota").
        val words = text.split(Regex("\\s+"))
        for (n in 2 downTo 1) {
            if (words.size <= n) continue
            val state = stateCode(words.takeLast(n).joinToString(" ")) ?: continue
            return normalize(words.dropLast(n).joinToString(" ")) to state
        }
        return normalize(text) to null
    }

    /** "tx", "Texas" or "TX." → "TX"; null if it isn't a state. */
    internal fun stateCode(text: String): String? {
        val key = text.trim().trimEnd('.').lowercase()
        if (key.length == 2) return key.uppercase().takeIf { it in STATES.values }
        return STATES[key]
    }

    /** Lowercase, no accents or periods, "Saint" / "Mount" / "Fort" the same as "St" / "Mt" / "Ft". */
    internal fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(".", "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { SHORT[it] ?: it }

    private val SHORT = mapOf("saint" to "st", "sainte" to "ste", "mount" to "mt", "fort" to "ft")

    private val STATES = mapOf(
        "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR", "california" to "CA",
        "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE", "district of columbia" to "DC",
        "washington dc" to "DC", "florida" to "FL", "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID",
        "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS", "kentucky" to "KY",
        "louisiana" to "LA", "maine" to "ME", "maryland" to "MD", "massachusetts" to "MA", "michigan" to "MI",
        "minnesota" to "MN", "mississippi" to "MS", "missouri" to "MO", "montana" to "MT", "nebraska" to "NE",
        "nevada" to "NV", "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
        "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK", "oregon" to "OR",
        "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC", "south dakota" to "SD",
        "tennessee" to "TN", "texas" to "TX", "utah" to "UT", "vermont" to "VT", "virginia" to "VA",
        "washington" to "WA", "west virginia" to "WV", "wisconsin" to "WI", "wyoming" to "WY",
        "puerto rico" to "PR",
    )
}

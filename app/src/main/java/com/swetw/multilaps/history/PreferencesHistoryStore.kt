package com.swetw.multilaps.history

import android.content.SharedPreferences
import androidx.core.content.edit
import com.swetw.multilaps.timing.StopwatchColour
import com.swetw.multilaps.timing.StopwatchLabel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Keeps the results as one JSON value in [preferences], apart from the timing and the labels. A
 * season of results is a few hundred kilobytes at most, which preferences hold comfortably.
 */
class PreferencesHistoryStore(private val preferences: SharedPreferences) : HistoryStore {

    override fun load(): List<Result> {
        val json = preferences.getString(KEY, null) ?: return emptyList()
        return try {
            decode(JSONObject(json))
        } catch (_: JSONException) {
            emptyList()
        }
    }

    override fun save(results: List<Result>) {
        preferences.edit { putString(KEY, encode(results).toString()) }
    }

    private fun encode(results: List<Result>) = JSONObject()
        .put(VERSION, CURRENT_VERSION)
        .put(RESULTS, JSONArray(results.map(::encode)))

    private fun encode(result: Result) = JSONObject()
        .put(ID, result.id)
        .put(NAME, result.name)
        .put(COLOUR, result.colour.name)
        .put(LAPS, JSONArray(result.laps.map { it.inWholeNanoseconds }))
        .put(TOTAL, result.totalTime.inWholeNanoseconds)
        .put(RESET_AT, result.resetAt.inWholeNanoseconds)

    private fun decode(json: JSONObject): List<Result> {
        if (json.getInt(VERSION) != CURRENT_VERSION) return emptyList()
        val results = json.getJSONArray(RESULTS)
        return (0 until results.length()).mapNotNull { decodeResult(results.opt(it)) }
    }

    /** One result that cannot be read is left out, and the rest are kept. */
    private fun decodeResult(entry: Any?): Result? {
        if (entry !is JSONObject) return null
        return try {
            val laps = entry.getJSONArray(LAPS)
            val colourName = entry.getString(COLOUR)
            Result(
                id = entry.getLong(ID),
                name = entry.getString(NAME),
                // A colour this version does not know, as after the palette changes, costs only
                // that colour.
                colour = StopwatchColour.entries.find { it.name == colourName } ?: UNKNOWN_COLOUR,
                laps = List(laps.length()) { laps.getLong(it).nanoseconds },
                totalTime = entry.getLong(TOTAL).nanoseconds,
                resetAt = entry.getLong(RESET_AT).nanoseconds,
            )
        } catch (_: JSONException) {
            null
        } catch (_: IllegalArgumentException) {
            // Figures no stopwatch could have had.
            null
        }
    }

    companion object {
        const val FILE_NAME = "history"
        const val KEY = "results"

        private val UNKNOWN_COLOUR = StopwatchLabel.default(0).colour

        private const val CURRENT_VERSION = 1
        private const val VERSION = "version"
        private const val RESULTS = "results"
        private const val ID = "id"
        private const val NAME = "name"
        private const val COLOUR = "colour"
        private const val LAPS = "lapsNanos"
        private const val TOTAL = "totalNanos"
        private const val RESET_AT = "resetAtNanos"
    }
}

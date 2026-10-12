package com.brothersbaran.multilaps.timing

import android.content.SharedPreferences
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Keeps the timing as one JSON value in [preferences]. Preferences are read synchronously, so the
 * stopwatches are already there when the screen first draws.
 */
class PreferencesTimingStore(private val preferences: SharedPreferences) : TimingStore {

    override fun load(): SavedTiming? {
        val json = preferences.getString(KEY, null) ?: return null
        return try {
            decode(JSONObject(json))
        } catch (_: JSONException) {
            null
        } catch (_: IllegalArgumentException) {
            // A state this version does not know, or figures no stopwatch could have.
            null
        }
    }

    override fun save(timing: SavedTiming) {
        preferences.edit().putString(KEY, encode(timing).toString()).apply()
    }

    private fun encode(timing: SavedTiming) = JSONObject()
        .put(VERSION, CURRENT_VERSION)
        .put(BOOT_COUNT, timing.bootCount)
        .put(BOOTED_AT, timing.bootedAt.inWholeNanoseconds)
        .put(STOPWATCHES, JSONArray(timing.stopwatches.map(::encode)))

    private fun encode(stopwatch: Stopwatch) = JSONObject()
        .put(STATE, stopwatch.state.name)
        .put(LAPS, JSONArray(stopwatch.laps.map { it.inWholeNanoseconds }))
        .put(ACCUMULATED, stopwatch.accumulated.inWholeNanoseconds)
        .putOpt(COUNTING_FROM, stopwatch.countingFrom?.inWholeNanoseconds)

    private fun decode(json: JSONObject): SavedTiming? {
        if (json.getInt(VERSION) != CURRENT_VERSION) return null
        val stopwatches = json.getJSONArray(STOPWATCHES)
        return SavedTiming(
            stopwatches = List(stopwatches.length()) { decodeStopwatch(stopwatches.getJSONObject(it)) },
            bootCount = json.getInt(BOOT_COUNT),
            bootedAt = json.getLong(BOOTED_AT).nanoseconds,
        )
    }

    private fun decodeStopwatch(json: JSONObject): Stopwatch {
        val laps = json.getJSONArray(LAPS)
        return Stopwatch(
            state = StopwatchState.valueOf(json.getString(STATE)),
            laps = List(laps.length()) { laps.getLong(it).nanoseconds },
            accumulated = json.getLong(ACCUMULATED).nanoseconds,
            countingFrom = if (json.has(COUNTING_FROM)) json.getLong(COUNTING_FROM).nanoseconds else null,
        )
    }

    companion object {
        const val KEY = "timing"

        private const val CURRENT_VERSION = 1
        private const val VERSION = "version"
        private const val BOOT_COUNT = "bootCount"
        private const val BOOTED_AT = "bootedAtNanos"
        private const val STOPWATCHES = "stopwatches"
        private const val STATE = "state"
        private const val LAPS = "lapsNanos"
        private const val ACCUMULATED = "accumulatedNanos"
        private const val COUNTING_FROM = "countingFromNanos"
    }
}

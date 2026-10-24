package com.swetw.multilaps.history

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.timing.StopwatchColour
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PreferencesHistoryStoreTest {

    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("history-test", Context.MODE_PRIVATE)
    private val store = PreferencesHistoryStore(preferences)

    private val results = listOf(
        Result(
            id = 1,
            name = "Zoë \"Zed\" O'Neil, 🏃",
            colour = StopwatchColour.Yellow,
            laps = listOf(83.seconds + 123_456_789.nanoseconds, 82.seconds),
            totalTime = 170.seconds + 1.nanoseconds,
            resetAt = 20_000.days + 7.nanoseconds,
            note = "Spikes on, \"new\" ones 🏃\nWindy back straight",
        ),
        Result(7, "2", StopwatchColour.Red, listOf(61.seconds), 61.seconds, 20_001.days),
    )

    private fun reloaded() = PreferencesHistoryStore(preferences).load()

    private fun stored() = JSONObject(preferences.getString(PreferencesHistoryStore.KEY, null)!!)

    private fun store(json: JSONObject) = preferences.edit { putString(PreferencesHistoryStore.KEY, json.toString()) }

    private fun entries(json: JSONObject): JSONArray = json.getJSONArray("results")

    @Test
    fun `nothing is loaded before anything is saved`() {
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `results come back exactly, to the nanosecond and with awkward characters`() {
        store.save(results)

        assertEquals(results, reloaded())
    }

    @Test
    fun `a later save replaces an earlier one`() {
        store.save(results)
        store.save(results.take(1))

        assertEquals(results.take(1), reloaded())
    }

    @Test
    fun `saving none leaves none`() {
        store.save(results)
        store.save(emptyList())

        assertTrue(reloaded().isEmpty())
    }

    @Test
    fun `what cannot be read at all loads as no results`() {
        preferences.edit { putString(PreferencesHistoryStore.KEY, "{ not json") }

        assertTrue(reloaded().isEmpty())
    }

    @Test
    fun `a value of the wrong shape loads as no results`() {
        preferences.edit { putString(PreferencesHistoryStore.KEY, "[1, 2]") }

        assertTrue(reloaded().isEmpty())
    }

    @Test
    fun `what a later version wrote loads as no results`() {
        store.save(results)
        store(stored().put("version", 99))

        assertTrue(reloaded().isEmpty())
    }

    @Test
    fun `one result that cannot be read costs only that result`() {
        store.save(results)
        val json = stored()
        entries(json).getJSONObject(0).remove("lapsNanos")
        store(json)

        assertEquals(results.drop(1), reloaded())
    }

    @Test
    fun `a result with figures no stopwatch could have had is left out`() {
        store.save(results)
        val json = stored()
        entries(json).getJSONObject(1).put("totalNanos", 1)
        store(json)

        assertEquals(results.take(1), reloaded())
    }

    @Test
    fun `an entry that is not a result at all is left out`() {
        store.save(results)
        val json = stored()
        entries(json).put("stray")
        store(json)

        assertEquals(results, reloaded())
    }

    @Test
    fun `a value that is not text at all loads as no results`() {
        preferences.edit { putInt(PreferencesHistoryStore.KEY, 7) }

        assertTrue(reloaded().isEmpty())
    }

    @Test
    fun `of two results under one identifier only the first is kept`() {
        store.save(results)
        val json = stored()
        entries(json).getJSONObject(1).put("id", results[0].id)
        store(json)

        assertEquals(results.take(1), reloaded())
    }

    @Test
    fun `a colour this version does not know costs only the colour`() {
        store.save(results)
        val json = stored()
        entries(json).getJSONObject(1).put("colour", "Chartreuse")
        store(json)

        assertEquals(listOf(results[0], results[1].copy(colour = StopwatchColour.Blue)), reloaded())
    }

    @Test
    fun `results kept before they had notes load with none`() {
        store.save(results)
        val json = stored()
        entries(json).getJSONObject(0).remove("note")
        store(json)

        assertEquals(listOf(results[0].copy(note = ""), results[1]), reloaded())
    }
}

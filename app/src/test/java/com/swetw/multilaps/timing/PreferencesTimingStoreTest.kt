package com.swetw.multilaps.timing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class PreferencesTimingStoreTest {

    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("timing-test", Context.MODE_PRIVATE)
    private val store = PreferencesTimingStore(preferences)
    private val clock = FakeClock()

    @Test
    fun `nothing is loaded before anything is saved`() {
        assertNull(store.load())
    }

    @Test
    fun `stopwatches in every state come back exactly, to the nanosecond`() {
        val start = clock.now() + 1.nanoseconds
        val running = Stopwatch().start(start, clock.wallNow()).lap(start + 30.seconds + 7.nanoseconds)
        val stopped = Stopwatch().start(start, clock.wallNow())
            .lap(start + 20.seconds)
            .lap(start + 45.seconds + 123.milliseconds)
            .stop(start + 50.seconds)
        val timing = SavedTiming.of(listOf(Stopwatch(), running, stopped, Stopwatch()), clock)

        store.save(timing)

        assertEquals(timing, PreferencesTimingStore(preferences).load())
    }

    @Test
    fun `readings too large for a floating point number, and negative ones, come back exactly`() {
        val huge = (Long.MAX_VALUE / 3 + 1).nanoseconds
        val carriedOver = Stopwatch(
            state = StopwatchState.Running,
            laps = listOf(30.seconds + 1.nanoseconds),
            accumulated = 30.seconds + 1.nanoseconds,
            countingFrom = (-987_654_321_987_654_321).nanoseconds,
        )
        val timing = SavedTiming(listOf(carriedOver), bootCount = 7, bootedAt = huge)

        store.save(timing)

        assertEquals(timing, store.load())
    }

    @Test
    fun `a later save replaces an earlier one`() {
        store.save(SavedTiming.of(listOf(Stopwatch().start(clock.now(), clock.wallNow())), clock))
        val later = SavedTiming.of(listOf(Stopwatch()), clock)
        store.save(later)

        assertEquals(later, store.load())
    }

    @Test
    fun `timing saved before start times were kept loads, with no start time`() {
        val earlier = """{"version":1,"bootCount":1,"bootedAtNanos":0,"stopwatches":[""" +
            """{"state":"Stopped","lapsNanos":[30000000000],"accumulatedNanos":40000000000}]}"""
        preferences.edit().putString(PreferencesTimingStore.KEY, earlier).commit()

        val stopwatch = store.load()!!.stopwatches.single()
        assertEquals(StopwatchState.Stopped, stopwatch.state)
        assertEquals(listOf(30.seconds), stopwatch.laps)
        assertNull(stopwatch.startedAt)
    }

    @Test
    fun `unreadable data is treated as nothing saved`() {
        for (garbage in listOf("", "not json", "{}", """{"version":1,"stopwatches":"oops"}""")) {
            preferences.edit().putString(PreferencesTimingStore.KEY, garbage).commit()

            assertNull("for $garbage", store.load())
        }
    }

    @Test
    fun `data describing an impossible stopwatch is treated as nothing saved`() {
        store.save(SavedTiming.of(listOf(Stopwatch().start(clock.now(), clock.wallNow())), clock))
        val tampered = preferences.getString(PreferencesTimingStore.KEY, null)!!
            .replace("\"Running\"", "\"Idle\"")
        preferences.edit().putString(PreferencesTimingStore.KEY, tampered).commit()

        assertNull(store.load())
    }

    @Test
    fun `data from a format this version does not know is treated as nothing saved`() {
        store.save(SavedTiming.of(listOf(Stopwatch()), clock))
        val future = preferences.getString(PreferencesTimingStore.KEY, null)!!
            .replace("\"version\":1", "\"version\":99")
        preferences.edit().putString(PreferencesTimingStore.KEY, future).commit()

        assertNull(store.load())
    }
}

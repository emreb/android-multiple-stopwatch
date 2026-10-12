package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.FakeClock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The app being killed is modelled as a new view model over the same store. */
class TimingPersistenceTest {

    private val clock = FakeClock()
    private val store = FakeTimingStore()

    private fun reopened() = TimingViewModel(clock, store)

    @Test
    fun `with nothing saved there are four idle stopwatches`() {
        assertEquals(List(4) { Stopwatch() }, reopened().stopwatches.value)
    }

    @Test
    fun `every press is saved`() {
        val viewModel = reopened()

        viewModel.onStartPressed(0)
        assertEquals(1, store.saveCount)
        clock.advanceBy(5.seconds)
        viewModel.onStartPressed(0)
        assertEquals(2, store.saveCount)
        viewModel.onStopPressed(0)
        assertEquals(3, store.saveCount)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(0)
        assertEquals(4, store.saveCount)

        assertEquals(viewModel.stopwatches.value, store.saved?.stopwatches)
    }

    @Test
    fun `after the app is killed every stopwatch returns with its state, time and laps`() {
        val before = reopened()
        before.onStartPressed(0)
        before.onStartPressed(1)
        clock.advanceBy(30.seconds)
        before.onStartPressed(0)
        before.onStartPressed(1)
        clock.advanceBy(8.seconds)
        before.onStopPressed(1)

        clock.advanceBy(10.minutes)
        val after = reopened().stopwatches.value

        assertEquals(before.stopwatches.value, after)
        val now = clock.now()
        // Running while away: it shows the true time, with the time away in its current lap.
        assertEquals(StopwatchState.Running, after[0].state)
        assertEquals(10.minutes + 38.seconds, after[0].totalTime(now))
        assertEquals(10.minutes + 8.seconds, after[0].currentLap(now))
        assertEquals(listOf(30.seconds), after[0].laps)
        // Stopped while away: its held time is unchanged.
        assertEquals(StopwatchState.Stopped, after[1].state)
        assertEquals(38.seconds, after[1].totalTime(now))
        assertEquals(listOf(30.seconds), after[1].laps)
        assertEquals(Stopwatch(), after[2])
        assertEquals(Stopwatch(), after[3])
    }

    @Test
    fun `after a phone restart a running stopwatch continues, and keeps doing so if killed again`() {
        val before = reopened()
        before.onStartPressed(2)
        clock.advanceBy(1.minutes)

        clock.restart(downtime = 3.minutes, uptime = 20.seconds)
        val afterRestart = reopened()
        assertEquals(4.minutes + 20.seconds, afterRestart.stopwatches.value[2].totalTime(clock.now()))

        // The translated reading must be what is saved, or a second return would translate twice.
        clock.advanceBy(40.seconds)
        val afterSecondKill = reopened()
        assertEquals(5.minutes, afterSecondKill.stopwatches.value[2].totalTime(clock.now()))
        assertEquals(StopwatchState.Running, afterSecondKill.stopwatches.value[2].state)
    }

    @Test
    fun `saved data for a different number of stopwatches is ignored`() {
        store.saved = SavedTiming.of(List(3) { Stopwatch() }, clock)

        assertEquals(List(4) { Stopwatch() }, reopened().stopwatches.value)
    }
}

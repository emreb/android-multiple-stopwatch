package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.FakeClock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SavedTimingTest {

    private val clock = FakeClock()

    private fun runningWithOneLap(): Stopwatch {
        val started = Stopwatch().start(clock.now())
        clock.advanceBy(30.seconds)
        return started.lap(clock.now())
    }

    @Test
    fun `within the same boot stopwatches come back exactly as they were saved`() {
        val running = runningWithOneLap()
        val stopped = Stopwatch().start(clock.now()).stop(clock.now() + 7.seconds)
        val saved = SavedTiming.of(listOf(running, stopped, Stopwatch()), clock)

        clock.advanceBy(10.minutes)

        assertEquals(listOf(running, stopped, Stopwatch()), saved.restoredAt(clock))
    }

    @Test
    fun `within the same boot a running stopwatch has counted the time the app was away`() {
        val saved = SavedTiming.of(listOf(runningWithOneLap()), clock)

        clock.advanceBy(10.minutes)

        val restored = saved.restoredAt(clock).single()
        assertEquals(10.minutes + 30.seconds, restored.totalTime(clock.now()))
        assertEquals(10.minutes, restored.currentLap(clock.now()))
    }

    @Test
    fun `within the same boot a change to the wall clock does not disturb the time`() {
        val saved = SavedTiming.of(listOf(runningWithOneLap()), clock)

        clock.advanceBy(10.minutes)
        clock.adjustWallClockBy(3.hours)

        assertEquals(10.minutes + 30.seconds, saved.restoredAt(clock).single().totalTime(clock.now()))
    }

    @Test
    fun `after a restart a running stopwatch continues from the wall clock`() {
        val saved = SavedTiming.of(listOf(runningWithOneLap()), clock)

        clock.advanceBy(2.minutes)
        clock.restart(downtime = 5.minutes, uptime = 40.seconds)

        val restored = saved.restoredAt(clock).single()
        val away = 2.minutes + 5.minutes + 40.seconds
        assertEquals(StopwatchState.Running, restored.state)
        assertEquals(30.seconds + away, restored.totalTime(clock.now()))
        assertEquals(away, restored.currentLap(clock.now()))
        assertEquals(listOf(30.seconds), restored.laps)

        clock.advanceBy(10.seconds)
        assertEquals(30.seconds + away + 10.seconds, restored.totalTime(clock.now()))
    }

    @Test
    fun `after a restart stopped and idle stopwatches are unchanged`() {
        val stopped = runningWithOneLap().stop(clock.now() + 7.seconds)
        val saved = SavedTiming.of(listOf(stopped, Stopwatch()), clock)

        clock.restart(downtime = 5.minutes)

        assertEquals(listOf(stopped, Stopwatch()), saved.restoredAt(clock))
    }

    @Test
    fun `after a restart with the wall clock set back a running stopwatch keeps what it had gathered`() {
        val saved = SavedTiming.of(listOf(runningWithOneLap()), clock)

        clock.restart(downtime = 5.minutes)
        clock.adjustWallClockBy(-2.hours)

        val restored = saved.restoredAt(clock).single()
        assertEquals(StopwatchState.Running, restored.state)
        assertEquals(30.seconds, restored.totalTime(clock.now()))
        assertEquals(Duration.ZERO, restored.currentLap(clock.now()))
        assertEquals(35.seconds, restored.totalTime(clock.now() + 5.seconds))
    }

    @Test
    fun `a restart the device failed to count is still recognised by its impossible reading`() {
        val saved = SavedTiming.of(listOf(runningWithOneLap()), clock)

        clock.advanceBy(2.minutes)
        clock.restart(downtime = 5.minutes, uptime = 40.seconds, countsBoot = false)

        val restored = saved.restoredAt(clock).single()
        val away = 2.minutes + 5.minutes + 40.seconds
        assertEquals(30.seconds + away, restored.totalTime(clock.now()))
    }
}

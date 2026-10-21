package com.swetw.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StopwatchLapsTest {

    private val start = 100.seconds
    private val running = Stopwatch().start(now = start)

    @Test
    fun `a stopwatch without completed laps has no last or best lap`() {
        assertEquals(0, running.lapCount)
        assertNull(running.lastLap)
        assertNull(running.bestLap)
    }

    @Test
    fun `before the first lap the current lap equals the total time`() {
        assertEquals(7.seconds, running.currentLap(now = start + 7.seconds))
    }

    @Test
    fun `a lap press completes the current lap and begins a new one`() {
        val stopwatch = running.lap(now = start + 30.seconds)

        assertEquals(1, stopwatch.lapCount)
        assertEquals(30.seconds, stopwatch.lastLap)
        assertEquals(Duration.ZERO, stopwatch.currentLap(now = start + 30.seconds))
        assertEquals(4.seconds, stopwatch.currentLap(now = start + 34.seconds))
        assertEquals(34.seconds, stopwatch.totalTime(now = start + 34.seconds))
    }

    @Test
    fun `laps are kept in the order they were completed`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .lap(now = start + 95.seconds)

        assertEquals(listOf(30.seconds, 25.seconds, 40.seconds), stopwatch.laps)
        assertEquals(40.seconds, stopwatch.lastLap)
    }

    @Test
    fun `the best lap is the shortest completed lap`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .lap(now = start + 95.seconds)

        assertEquals(25.seconds, stopwatch.bestLap)
    }

    @Test
    fun `time spent stopped is not part of a lap and resuming continues the same current lap`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .stop(now = start + 40.seconds)
            .start(now = start + 500.seconds)

        assertEquals(1, stopwatch.lapCount)
        assertEquals(10.seconds, stopwatch.currentLap(now = start + 500.seconds))

        val lapped = stopwatch.lap(now = start + 505.seconds)
        assertEquals(15.seconds, lapped.lastLap)
    }

    @Test
    fun `stopping does not complete the current lap`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .stop(now = start + 35.seconds)

        assertEquals(1, stopwatch.lapCount)
        assertEquals(30.seconds, stopwatch.lastLap)
        assertEquals(30.seconds, stopwatch.bestLap)
        assertEquals(5.seconds, stopwatch.currentLap(now = start + 900.seconds))
    }

    @Test
    fun `a lap press straight after the previous one counts`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 30.seconds + 200.milliseconds)

        assertEquals(listOf(30.seconds, 200.milliseconds), stopwatch.laps)
        assertEquals(200.milliseconds, stopwatch.bestLap)
    }

    @Test
    fun `a lap press straight after starting counts`() {
        assertEquals(listOf(300.milliseconds), running.lap(now = start + 300.milliseconds).laps)
    }

    @Test
    fun `a lap press straight after resuming counts`() {
        val resumed = running
            .stop(now = start + 20.seconds)
            .start(now = start + 60.seconds)

        val lapped = resumed.lap(now = start + 60.seconds + 300.milliseconds)

        assertEquals(listOf(20.seconds + 300.milliseconds), lapped.laps)
    }

    @Test
    fun `a lap pressed at the same instant as the start counts as a lap of no time`() {
        assertEquals(listOf(Duration.ZERO), running.lap(now = start).laps)
    }

    @Test
    fun `only a running stopwatch records laps`() {
        val idle = Stopwatch()
        val stopped = running.stop(now = start + 20.seconds)

        assertEquals(idle, idle.lap(now = start + 50.seconds))
        assertEquals(stopped, stopped.lap(now = start + 50.seconds))
    }

    @Test
    fun `reset discards all laps`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .stop(now = start + 60.seconds)
            .reset()

        assertEquals(Stopwatch(), stopwatch)
        assertEquals(0, stopwatch.lapCount)
        assertNull(stopwatch.bestLap)
        assertEquals(Duration.ZERO, stopwatch.currentLap(now = start + 900.seconds))
    }

    @Test
    fun `a stopwatch cannot be built idle with laps or time, or with laps longer than its total time`() {
        assertThrows(IllegalArgumentException::class.java) {
            Stopwatch(laps = listOf(30.seconds))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Stopwatch(accumulated = 30.seconds)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Stopwatch(
                state = StopwatchState.Stopped,
                laps = listOf(30.seconds, 40.seconds),
                accumulated = 60.seconds,
            )
        }
    }
}

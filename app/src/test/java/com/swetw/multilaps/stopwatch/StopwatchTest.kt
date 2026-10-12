package com.swetw.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StopwatchTest {

    @Test
    fun `a new stopwatch is idle and shows zero`() {
        val stopwatch = Stopwatch()

        assertEquals(StopwatchState.Idle, stopwatch.state)
        assertEquals(Duration.ZERO, stopwatch.totalTime(now = 500.seconds))
    }

    @Test
    fun `starting an idle stopwatch makes it running`() {
        val stopwatch = Stopwatch().start(now = 100.seconds)

        assertEquals(StopwatchState.Running, stopwatch.state)
    }

    @Test
    fun `total time is the time since the stopwatch was started`() {
        val stopwatch = Stopwatch().start(now = 100.seconds)

        assertEquals(Duration.ZERO, stopwatch.totalTime(now = 100.seconds))
        assertEquals(1234.milliseconds, stopwatch.totalTime(now = 100.seconds + 1234.milliseconds))
    }

    @Test
    fun `starting a running stopwatch does not restart it`() {
        val stopwatch = Stopwatch().start(now = 100.seconds).start(now = 130.seconds)

        assertEquals(40.seconds, stopwatch.totalTime(now = 140.seconds))
    }

    @Test
    fun `stopping a running stopwatch holds its total time`() {
        val stopwatch = Stopwatch().start(now = 100.seconds).stop(now = 112.seconds)

        assertEquals(StopwatchState.Stopped, stopwatch.state)
        assertEquals(12.seconds, stopwatch.totalTime(now = 112.seconds))
        assertEquals(12.seconds, stopwatch.totalTime(now = 900.seconds))
    }

    @Test
    fun `starting a stopped stopwatch resumes without counting the time spent stopped`() {
        val stopwatch = Stopwatch()
            .start(now = 100.seconds)
            .stop(now = 112.seconds)
            .start(now = 200.seconds)

        assertEquals(StopwatchState.Running, stopwatch.state)
        assertEquals(12.seconds, stopwatch.totalTime(now = 200.seconds))
        assertEquals(17.seconds, stopwatch.totalTime(now = 205.seconds))
    }

    @Test
    fun `total time accumulates across repeated stops and resumes`() {
        val stopwatch = Stopwatch()
            .start(now = 100.seconds).stop(now = 101.seconds)
            .start(now = 200.seconds).stop(now = 202.seconds)
            .start(now = 300.seconds).stop(now = 304.seconds)

        assertEquals(7.seconds, stopwatch.totalTime(now = 999.seconds))
    }

    @Test
    fun `stopping an idle or already stopped stopwatch changes nothing`() {
        val idle = Stopwatch()
        val stopped = Stopwatch().start(now = 100.seconds).stop(now = 112.seconds)

        assertEquals(idle, idle.stop(now = 500.seconds))
        assertEquals(stopped, stopped.stop(now = 500.seconds))
    }

    @Test
    fun `resetting a stopped stopwatch returns it to idle at zero`() {
        val stopwatch = Stopwatch().start(now = 100.seconds).stop(now = 112.seconds).reset()

        assertEquals(Stopwatch(), stopwatch)
        assertEquals(StopwatchState.Idle, stopwatch.state)
        assertEquals(Duration.ZERO, stopwatch.totalTime(now = 500.seconds))
    }

    @Test
    fun `a running stopwatch cannot be reset`() {
        val running = Stopwatch().start(now = 100.seconds)

        assertEquals(running, running.reset())
    }

    @Test
    fun `a stopwatch stopped the instant it started is stopped, not idle`() {
        val stopwatch = Stopwatch().start(now = 100.seconds).stop(now = 100.seconds)

        assertEquals(StopwatchState.Stopped, stopwatch.state)
    }

    @Test
    fun `a stopwatch cannot be built running without a reading to count from, or holding one while not running`() {
        assertThrows(IllegalArgumentException::class.java) {
            Stopwatch(state = StopwatchState.Running)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Stopwatch(state = StopwatchState.Stopped, countingFrom = 100.seconds)
        }
    }
}

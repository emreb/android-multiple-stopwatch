package com.brothersbaran.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StopwatchTest {

    @Test
    fun `an idle stopwatch is not running and shows zero`() {
        val stopwatch = Stopwatch()

        assertFalse(stopwatch.isRunning)
        assertEquals(Duration.ZERO, stopwatch.totalTime(now = 500.seconds))
    }

    @Test
    fun `a started stopwatch is running`() {
        val stopwatch = Stopwatch().start(now = 100.seconds)

        assertTrue(stopwatch.isRunning)
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
}

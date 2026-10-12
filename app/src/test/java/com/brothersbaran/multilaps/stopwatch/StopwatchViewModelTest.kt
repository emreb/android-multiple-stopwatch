package com.brothersbaran.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class StopwatchViewModelTest {

    private val clock = FakeClock()
    private val viewModel = StopwatchViewModel(clock)

    @Test
    fun `the stopwatch starts out idle at zero`() {
        clock.advanceBy(750.milliseconds)

        assertFalse(viewModel.stopwatch.value.isRunning)
        assertEquals(Duration.ZERO, viewModel.totalTime())
    }

    @Test
    fun `pressing start makes total time advance with the clock`() {
        viewModel.onStartPressed()
        clock.advanceBy(2500.milliseconds)

        assertTrue(viewModel.stopwatch.value.isRunning)
        assertEquals(2500.milliseconds, viewModel.totalTime())
    }
}

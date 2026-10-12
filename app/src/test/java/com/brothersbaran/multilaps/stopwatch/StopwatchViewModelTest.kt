package com.brothersbaran.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StopwatchViewModelTest {

    private val clock = FakeClock()
    private val viewModel = StopwatchViewModel(clock)

    private val state get() = viewModel.stopwatch.value.state

    @Test
    fun `the stopwatch starts out idle at zero`() {
        clock.advanceBy(750.milliseconds)

        assertEquals(StopwatchState.Idle, state)
        assertEquals(Duration.ZERO, viewModel.totalTime())
    }

    @Test
    fun `pressing start makes total time advance with the clock`() {
        viewModel.onStartPressed()
        clock.advanceBy(2500.milliseconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(2500.milliseconds, viewModel.totalTime())
    }

    @Test
    fun `pressing stop while running holds the total time`() {
        viewModel.onStartPressed()
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed()
        clock.advanceBy(60.seconds)

        assertEquals(StopwatchState.Stopped, state)
        assertEquals(3.seconds, viewModel.totalTime())
    }

    @Test
    fun `pressing start while stopped resumes`() {
        viewModel.onStartPressed()
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed()
        clock.advanceBy(60.seconds)
        viewModel.onStartPressed()
        clock.advanceBy(2.seconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(5.seconds, viewModel.totalTime())
    }

    @Test
    fun `pressing stop while stopped resets`() {
        viewModel.onStartPressed()
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed()
        viewModel.onStopPressed()

        assertEquals(StopwatchState.Idle, state)
        assertEquals(Duration.ZERO, viewModel.totalTime())
    }

    @Test
    fun `pressing stop while idle does nothing`() {
        viewModel.onStopPressed()

        assertEquals(StopwatchState.Idle, state)
    }
}

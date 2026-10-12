package com.brothersbaran.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StopwatchViewModelTest {

    private val clock = FakeClock()
    private val viewModel = StopwatchViewModel(clock)

    private val stopwatch get() = viewModel.stopwatch.value
    private val state get() = stopwatch.state
    private val totalTime get() = stopwatch.totalTime(clock.now())

    @Test
    fun `the stopwatch starts out idle at zero`() {
        clock.advanceBy(750.milliseconds)

        assertEquals(StopwatchState.Idle, state)
        assertEquals(Duration.ZERO, totalTime)
    }

    @Test
    fun `pressing start makes total time advance with the clock`() {
        viewModel.onStartPressed()
        clock.advanceBy(2500.milliseconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(2500.milliseconds, totalTime)
    }

    @Test
    fun `pressing stop while running holds the total time`() {
        viewModel.onStartPressed()
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed()
        clock.advanceBy(60.seconds)

        assertEquals(StopwatchState.Stopped, state)
        assertEquals(3.seconds, totalTime)
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
        assertEquals(5.seconds, totalTime)
    }

    @Test
    fun `pressing stop while stopped resets`() {
        viewModel.onStartPressed()
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed()
        viewModel.onStopPressed()

        assertEquals(StopwatchState.Idle, state)
        assertEquals(Duration.ZERO, totalTime)
    }

    @Test
    fun `pressing stop while idle does nothing`() {
        viewModel.onStopPressed()

        assertEquals(StopwatchState.Idle, state)
    }

    @Test
    fun `pressing start while running records a lap`() {
        viewModel.onStartPressed()
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed()
        clock.advanceBy(4.seconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(listOf(30.seconds), stopwatch.laps)
        assertEquals(4.seconds, stopwatch.currentLap(clock.now()))
        assertEquals(34.seconds, totalTime)
    }

    @Test
    fun `a bounced press on start does not record a lap`() {
        viewModel.onStartPressed()
        clock.advanceBy(200.milliseconds)
        viewModel.onStartPressed()

        assertEquals(0, stopwatch.lapCount)
    }
}

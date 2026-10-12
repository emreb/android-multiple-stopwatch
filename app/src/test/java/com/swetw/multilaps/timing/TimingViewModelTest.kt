package com.swetw.multilaps.timing

import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimingViewModelTest {

    private val clock = FakeClock()
    private val viewModel = TimingViewModel(clock, FakeTimingStore(), FakeLabelStore())

    private val stopwatches get() = viewModel.stopwatches.value
    /** The first stopwatch, for the tests that need only one. */
    private val stopwatch get() = stopwatches[0]
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
        viewModel.onStartPressed(0)
        clock.advanceBy(2500.milliseconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(2500.milliseconds, totalTime)
    }

    @Test
    fun `pressing stop while running holds the total time`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(60.seconds)

        assertEquals(StopwatchState.Stopped, state)
        assertEquals(3.seconds, totalTime)
    }

    @Test
    fun `pressing start while stopped resumes`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(60.seconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(2.seconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(5.seconds, totalTime)
    }

    @Test
    fun `pressing stop while stopped resets`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(3.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(1.seconds)
        viewModel.onStopPressed(0)

        assertEquals(StopwatchState.Idle, state)
        assertEquals(Duration.ZERO, totalTime)
    }

    @Test
    fun `pressing stop while idle does nothing`() {
        viewModel.onStopPressed(0)

        assertEquals(StopwatchState.Idle, state)
    }

    @Test
    fun `pressing start while running records a lap`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(4.seconds)

        assertEquals(StopwatchState.Running, state)
        assertEquals(listOf(30.seconds), stopwatch.laps)
        assertEquals(4.seconds, stopwatch.currentLap(clock.now()))
        assertEquals(34.seconds, totalTime)
    }

    @Test
    fun `a bounced press on start does not record a lap`() {
        viewModel.onStartPressed(0)
        val started = stopwatch
        clock.advanceBy(200.milliseconds)
        viewModel.onStartPressed(0)

        assertEquals(started, stopwatch)
    }

    @Test
    fun `there are four stopwatches, all idle to begin with`() {
        assertEquals(List(4) { Stopwatch() }, stopwatches)
    }

    @Test
    fun `stopwatches started at different moments keep their own total times`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(10.seconds)
        viewModel.onStartPressed(2)
        clock.advanceBy(5.seconds)

        val now = clock.now()
        assertEquals(listOf(15.seconds, Duration.ZERO, 5.seconds, Duration.ZERO), stopwatches.map { it.totalTime(now) })
        assertEquals(
            listOf(StopwatchState.Running, StopwatchState.Idle, StopwatchState.Running, StopwatchState.Idle),
            stopwatches.map { it.state },
        )
    }

    @Test
    fun `laps, stops and resets touch only the stopwatch that was pressed`() {
        viewModel.onStartPressed(1)
        viewModel.onStartPressed(3)
        clock.advanceBy(20.seconds)
        viewModel.onStartPressed(3)
        clock.advanceBy(4.seconds)
        viewModel.onStopPressed(1)
        clock.advanceBy(1.seconds)
        viewModel.onStopPressed(1)

        assertEquals(Stopwatch(), stopwatches[1])
        assertEquals(StopwatchState.Running, stopwatches[3].state)
        assertEquals(listOf(20.seconds), stopwatches[3].laps)
        assertEquals(25.seconds, stopwatches[3].totalTime(clock.now()))
        assertEquals(Stopwatch(), stopwatches[0])
        assertEquals(Stopwatch(), stopwatches[2])
    }

    @Test
    fun `a press for a stopwatch that does not exist is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { viewModel.onStartPressed(4) }
        assertThrows(IllegalArgumentException::class.java) { viewModel.onStopPressed(-1) }
    }
}

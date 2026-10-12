package com.swetw.multilaps.timing

import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class StartAllTest {

    private val clock = FakeClock()
    private val store = FakeTimingStore()
    private val felt = mutableListOf<Feedback>()
    private val viewModel = TimingViewModel(clock, store, FakeLabelStore(), FakeHistoryStore(), haptics = { felt += it })

    private val stopwatches get() = viewModel.stopwatches.value

    @Test
    fun `start all sets every idle stopwatch off at the same instant`() {
        viewModel.onStartAllPressed()
        clock.advanceBy(12.seconds)

        assertTrue(stopwatches.all { it.state == StopwatchState.Running })
        assertEquals(List(4) { 12.seconds }, stopwatches.map { it.totalTime(clock.now()) })
        assertEquals(1, stopwatches.distinct().size)
    }

    @Test
    fun `start all leaves a running stopwatch as it is`() {
        viewModel.onStartPressed(1)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(1)
        val running = stopwatches[1]

        clock.advanceBy(5.seconds)
        viewModel.onStartAllPressed()

        assertEquals(running, stopwatches[1])
        assertEquals(listOf(30.seconds), stopwatches[1].laps)
        assertEquals(StopwatchState.Running, stopwatches[0].state)
    }

    @Test
    fun `start all does not resume a stopped stopwatch`() {
        viewModel.onStartPressed(1)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(1)
        val stopped = stopwatches[1]

        clock.advanceBy(5.seconds)
        viewModel.onStartAllPressed()

        assertEquals(stopped, stopwatches[1])
        assertEquals(
            listOf(StopwatchState.Running, StopwatchState.Stopped, StopwatchState.Running, StopwatchState.Running),
            stopwatches.map { it.state },
        )
    }

    @Test
    fun `start all can be pressed only while some stopwatch is idle`() {
        assertTrue(canStartAll(stopwatches))

        viewModel.onStartPressed(0)
        viewModel.onStartPressed(1)
        viewModel.onStartPressed(2)
        assertTrue(canStartAll(stopwatches))

        viewModel.onStartPressed(3)
        assertFalse(canStartAll(stopwatches))
    }

    @Test
    fun `start all with nothing idle does nothing, saves nothing and is not felt`() {
        viewModel.onStartAllPressed()
        clock.advanceBy(30.seconds)
        val before = stopwatches
        val saves = store.saveCount
        felt.clear()

        viewModel.onStartAllPressed()

        assertEquals(before, stopwatches)
        assertEquals(saves, store.saveCount)
        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `start all is saved once and felt once, as a start`() {
        viewModel.onStartAllPressed()

        assertEquals(1, store.saveCount)
        assertEquals(stopwatches, store.saved?.stopwatches)
        assertEquals(listOf(Feedback.Start), felt)
    }

    @Test
    fun `start all gives up the undo for any stopwatch it sets off again`() {
        viewModel.onStartPressed(0)
        viewModel.onStartPressed(1)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        viewModel.onStopPressed(1)
        viewModel.onResetAllPressed()

        viewModel.onStartAllPressed()

        assertNull(viewModel.undoOffer.value)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(List(4) { 2.seconds }, stopwatches.map { it.totalTime(clock.now()) })
    }

    @Test
    fun `start all gives up the undo for a single stopwatch it sets off again`() {
        viewModel.onStartPressed(2)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(2)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(2)

        viewModel.onStartAllPressed()

        assertNull(viewModel.undoOffer.value)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(StopwatchState.Running, stopwatches[2].state)
        assertEquals(2.seconds, stopwatches[2].totalTime(clock.now()))
    }

    @Test
    fun `a lap press within a second of start all is taken for a bounce`() {
        viewModel.onStartAllPressed()
        val started = stopwatches
        val saves = store.saveCount
        felt.clear()
        clock.advanceBy(999.milliseconds)
        viewModel.onStartPressed(1)

        assertEquals(started, stopwatches)
        assertEquals(saves, store.saveCount)
        assertEquals(emptyList<Feedback>(), felt)

        clock.advanceBy(1.milliseconds)
        viewModel.onStartPressed(1)
        assertEquals(listOf(1.seconds), stopwatches[1].laps)
    }

    @Test
    fun `stopwatches started together can be lapped, stopped and reset each on their own`() {
        viewModel.onStartAllPressed()
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(2)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(0)

        assertEquals(listOf(30.seconds), stopwatches[2].laps)
        assertEquals(StopwatchState.Stopped, stopwatches[0].state)
        assertEquals(35.seconds, stopwatches[0].totalTime(clock.now() + 60.seconds))
        assertEquals(StopwatchState.Running, stopwatches[1].state)
        assertEquals(emptyList<Duration>(), stopwatches[1].laps)
        assertEquals(35.seconds, stopwatches[1].totalTime(clock.now()))
    }
}

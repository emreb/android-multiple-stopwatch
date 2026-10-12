package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.FakeClock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ResetTest {

    private val clock = FakeClock()
    private val store = FakeTimingStore()
    private val viewModel = TimingViewModel(clock, store)

    private val stopwatches get() = viewModel.stopwatches.value
    private val offer get() = viewModel.undoOffer.value

    /** Starts, laps once and stops a stopwatch, leaving the clock two seconds past the stop. */
    private fun timeAndStop(index: Int): Stopwatch {
        viewModel.onStartPressed(index)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(index)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(index)
        clock.advanceBy(2.seconds)
        return stopwatches[index]
    }

    @Test
    fun `a reset press within a second of stopping is ignored`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        val stopped = stopwatches[0]

        clock.advanceBy(200.milliseconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(799.milliseconds)
        viewModel.onStopPressed(0)

        assertEquals(stopped, stopwatches[0])
        assertNull(offer)
    }

    @Test
    fun `a reset press a full second after stopping resets`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(1.seconds)
        viewModel.onStopPressed(0)

        assertEquals(Stopwatch(), stopwatches[0])
    }

    @Test
    fun `a reset offers an undo for that stopwatch alone`() {
        val stopped = timeAndStop(1)
        viewModel.onStopPressed(1)

        assertEquals(mapOf(1 to stopped), offer?.stopwatches)
        assertFalse(offer!!.coversAll)
    }

    @Test
    fun `undo restores the stopwatch exactly, laps included`() {
        val stopped = timeAndStop(1)
        viewModel.onStopPressed(1)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(stopped, stopwatches[1])
        assertEquals(StopwatchState.Stopped, stopwatches[1].state)
        assertEquals(listOf(30.seconds), stopwatches[1].laps)
        assertEquals(35.seconds, stopwatches[1].totalTime(clock.now()))
        assertNull(offer)
    }

    @Test
    fun `an undo press within a second of the reset is ignored`() {
        timeAndStop(1)
        viewModel.onStopPressed(1)
        clock.advanceBy(300.milliseconds)
        viewModel.onUndoPressed()

        assertEquals(Stopwatch(), stopwatches[1])
        assertTrue(offer != null)
    }

    @Test
    fun `a reset press within a second of an undo is ignored`() {
        val stopped = timeAndStop(1)
        viewModel.onStopPressed(1)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        clock.advanceBy(300.milliseconds)
        viewModel.onStopPressed(1)

        assertEquals(stopped, stopwatches[1])
    }

    @Test
    fun `undo leaves alone whatever the other stopwatches did meanwhile`() {
        timeAndStop(1)
        viewModel.onStartPressed(3)
        viewModel.onStopPressed(1)
        clock.advanceBy(2.seconds)
        viewModel.onStartPressed(3)
        val lapped = stopwatches[3]
        viewModel.onUndoPressed()

        assertEquals(lapped, stopwatches[3])
        assertEquals(listOf(2.seconds), stopwatches[3].laps)
    }

    @Test
    fun `once the offer has expired there is nothing to undo`() {
        timeAndStop(1)
        viewModel.onStopPressed(1)
        viewModel.onUndoExpired(offer!!.id)

        assertNull(offer)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(Stopwatch(), stopwatches[1])
    }

    @Test
    fun `a later reset replaces the earlier offer, whose expiry then changes nothing`() {
        timeAndStop(0)
        val second = timeAndStop(1)
        viewModel.onStopPressed(0)
        val firstOffer = offer!!.id
        viewModel.onStopPressed(1)

        viewModel.onUndoExpired(firstOffer)
        assertEquals(mapOf(1 to second), offer?.stopwatches)

        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(Stopwatch(), stopwatches[0])
        assertEquals(second, stopwatches[1])
    }

    @Test
    fun `starting a stopwatch again withdraws the offer to undo its reset`() {
        timeAndStop(1)
        viewModel.onStopPressed(1)
        viewModel.onStartPressed(1)

        assertNull(offer)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(StopwatchState.Running, stopwatches[1].state)
        assertEquals(2.seconds, stopwatches[1].totalTime(clock.now()))
    }

    @Test
    fun `starting a different stopwatch keeps the offer`() {
        timeAndStop(1)
        viewModel.onStopPressed(1)
        viewModel.onStartPressed(2)

        assertTrue(offer != null)
    }

    @Test
    fun `reset and undo are both saved`() {
        val stopped = timeAndStop(1)
        viewModel.onStopPressed(1)
        assertEquals(Stopwatch(), store.saved?.stopwatches?.get(1))

        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(stopped, store.saved?.stopwatches?.get(1))
    }

    @Test
    fun `a stopwatch that came back stopped after the app was killed can be reset straight away`() {
        timeAndStop(1)
        val reopened = TimingViewModel(clock, store)
        reopened.onStopPressed(1)

        assertEquals(Stopwatch(), reopened.stopwatches.value[1])
    }

    @Test
    fun `reset all resets every stopped stopwatch and offers one undo for all of them`() {
        val first = timeAndStop(0)
        val third = timeAndStop(2)
        viewModel.onResetAllPressed()

        assertEquals(List(4) { Stopwatch() }, stopwatches)
        assertEquals(mapOf(0 to first, 2 to third), offer?.stopwatches)
        assertTrue(offer!!.coversAll)
    }

    @Test
    fun `undo after reset all restores every stopwatch it reset`() {
        val first = timeAndStop(0)
        val third = timeAndStop(2)
        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(listOf(first, Stopwatch(), third, Stopwatch()), stopwatches)
        assertNull(offer)
    }

    @Test
    fun `an undo press within a second of reset all is ignored`() {
        timeAndStop(0)
        viewModel.onResetAllPressed()
        clock.advanceBy(300.milliseconds)
        viewModel.onUndoPressed()

        assertEquals(List(4) { Stopwatch() }, stopwatches)
    }

    @Test
    fun `reset all does nothing while any stopwatch is running`() {
        val stopped = timeAndStop(0)
        viewModel.onStartPressed(3)
        viewModel.onResetAllPressed()

        assertEquals(stopped, stopwatches[0])
        assertEquals(StopwatchState.Running, stopwatches[3].state)
        assertNull(offer)
        assertFalse(viewModel.canResetAll(stopwatches))
    }

    @Test
    fun `reset all does nothing when every stopwatch is already idle`() {
        viewModel.onResetAllPressed()

        assertNull(offer)
        assertEquals(0, store.saveCount)
        assertFalse(viewModel.canResetAll(stopwatches))
    }

    @Test
    fun `reset all is possible once nothing is running and something is stopped`() {
        timeAndStop(0)

        assertTrue(viewModel.canResetAll(stopwatches))
    }

    @Test
    fun `starting any stopwatch after reset all withdraws the offer`() {
        timeAndStop(0)
        timeAndStop(2)
        viewModel.onResetAllPressed()
        viewModel.onStartPressed(2)

        assertNull(offer)
        assertEquals(Duration.ZERO, stopwatches[0].totalTime(clock.now()))
    }
}

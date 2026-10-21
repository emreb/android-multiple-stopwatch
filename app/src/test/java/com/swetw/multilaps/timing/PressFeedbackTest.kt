package com.swetw.multilaps.timing

import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PressFeedbackTest {

    private val clock = FakeClock()
    private val felt = mutableListOf<Feedback>()
    private val viewModel =
        TimingViewModel(clock, FakeTimingStore(), FakeLabelStore(), FakeHistoryStore(), haptics = { felt += it })

    @Test
    fun `nothing is felt until something is pressed`() {
        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `each press that takes effect is felt as what it did`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(
            listOf(
                Feedback.Start,
                Feedback.Lap,
                Feedback.Stop,
                Feedback.Start,
                Feedback.Stop,
                Feedback.Reset,
                Feedback.Undo,
            ),
            felt,
        )
    }

    @Test
    fun `reset all and its undo are each felt once, however many stopwatches they cover`() {
        viewModel.onStartPressed(0)
        viewModel.onStartPressed(2)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        viewModel.onStopPressed(2)
        felt.clear()

        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(listOf(Feedback.Reset, Feedback.Undo), felt)
    }

    @Test
    fun `every lap press is felt, however soon after the last`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(300.milliseconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(100.milliseconds)
        viewModel.onStartPressed(0)

        assertEquals(listOf(Feedback.Start, Feedback.Lap, Feedback.Lap), felt)
    }

    @Test
    fun `a reset press taken for a bounce is not felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(300.milliseconds)
        viewModel.onStopPressed(0)

        assertEquals(listOf(Feedback.Start, Feedback.Stop), felt)
    }

    @Test
    fun `an undo press taken for a bounce, or made too late, is not felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        felt.clear()

        clock.advanceBy(300.milliseconds)
        viewModel.onUndoPressed()
        clock.advanceBy(10.seconds)
        viewModel.onUndoPressed()

        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `a reset all press taken for a bounce is not felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        felt.clear()

        clock.advanceBy(300.milliseconds)
        viewModel.onResetAllPressed()

        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `presses with nothing to do are not felt`() {
        viewModel.onStopPressed(0)
        viewModel.onResetAllPressed()
        viewModel.onUndoPressed()

        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `renaming and recolouring are not felt`() {
        viewModel.onNameChanged(0, "Ada")
        viewModel.onColourChosen(0, StopwatchColour.Pink)

        assertEquals(emptyList<Feedback>(), felt)
    }
}

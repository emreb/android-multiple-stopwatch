package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PressFeedbackTest {

    private val clock = FakeClock()
    private val felt = mutableListOf<Feedback>()
    private val viewModel =
        TimingViewModel(clock, FakeTimingStore(), FakeLabelStore(), haptics = { felt += it })

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
    fun `a lap is felt differently from a stop`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)

        assertEquals(listOf(Feedback.Lap, Feedback.Stop), felt.takeLast(2))
    }

    @Test
    fun `reset all and its undo are felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        felt.clear()

        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(listOf(Feedback.Reset, Feedback.Undo), felt)
    }

    @Test
    fun `a lap press taken for a bounce is not felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(300.milliseconds)
        viewModel.onStartPressed(0)

        assertEquals(listOf(Feedback.Start), felt)
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

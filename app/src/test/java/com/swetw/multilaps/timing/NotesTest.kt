package com.swetw.multilaps.timing

import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class NotesTest {

    private val clock = FakeClock()
    private val timingStore = FakeTimingStore()
    private val historyStore = FakeHistoryStore()
    private val felt = mutableListOf<Feedback>()
    private val viewModel = reopened()

    private fun reopened() =
        TimingViewModel(clock, timingStore, FakeLabelStore(), historyStore, haptics = { felt += it })

    private val stopwatches get() = viewModel.stopwatches.value
    private val results get() = viewModel.results.value

    /** Starts, laps once and stops a stopwatch, leaving the clock two seconds past the stop. */
    private fun timeAndStop(index: Int) {
        viewModel.onStartPressed(index)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(index)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(index)
        clock.advanceBy(2.seconds)
    }

    @Test
    fun `stopwatches begin with no note`() {
        assertEquals(listOf("", "", "", ""), stopwatches.map { it.note })
    }

    @Test
    fun `a note can be written whether the stopwatch is idle, running or stopped`() {
        viewModel.onNoteChanged(0, "Spikes on")
        viewModel.onStartPressed(1)
        viewModel.onNoteChanged(1, "Windy back straight")
        timeAndStop(2)
        viewModel.onNoteChanged(2, "Tired")

        assertEquals(listOf("Spikes on", "Windy back straight", "Tired", ""), stopwatches.map { it.note })
        assertEquals(StopwatchState.Running, stopwatches[1].state)
        assertEquals(StopwatchState.Stopped, stopwatches[2].state)
    }

    @Test
    fun `writing a note leaves the timing as it was, and is not felt`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(0)
        val before = stopwatches[0]
        felt.clear()

        viewModel.onNoteChanged(0, "Spikes on")

        assertEquals(before.copy(note = "Spikes on"), stopwatches[0])
        assertEquals(emptyList<Feedback>(), felt)
    }

    @Test
    fun `space around a note is dropped, and a blank note is no note`() {
        viewModel.onNoteChanged(0, "  Spikes on\n\n")
        assertEquals("Spikes on", stopwatches[0].note)

        viewModel.onNoteChanged(0, " \n\t ")
        assertEquals("", stopwatches[0].note)
    }

    @Test
    fun `a note keeps the lines it was written in`() {
        viewModel.onNoteChanged(0, "Spikes on\nWindy")

        assertEquals("Spikes on\nWindy", stopwatches[0].note)
    }

    @Test
    fun `a very long note is cut to five hundred characters, without splitting an emoji`() {
        viewModel.onNoteChanged(0, "a".repeat(600))
        assertEquals("a".repeat(500), stopwatches[0].note)

        viewModel.onNoteChanged(0, "a".repeat(499) + "🏃")
        assertEquals("a".repeat(499), stopwatches[0].note)
    }

    @Test
    fun `a note is still there when the app is opened again`() {
        viewModel.onStartPressed(0)
        viewModel.onNoteChanged(0, "Spikes on")

        assertEquals("Spikes on", reopened().stopwatches.value[0].note)
    }

    @Test
    fun `a reset keeps the note with the result and leaves the stopwatch without one`() {
        timeAndStop(0)
        viewModel.onNoteChanged(0, "Spikes on")
        viewModel.onStopPressed(0)

        assertEquals(listOf("Spikes on"), results.map { it.note })
        assertEquals(Stopwatch(), stopwatches[0])
        assertEquals(listOf("Spikes on"), reopened().results.value.map { it.note })
    }

    @Test
    fun `a result from a stopwatch without a note has none`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)

        assertEquals(listOf(""), results.map { it.note })
    }

    @Test
    fun `reset all keeps each stopwatch's own note with its result`() {
        timeAndStop(0)
        timeAndStop(1)
        viewModel.onNoteChanged(0, "Ada's note")
        viewModel.onNoteChanged(1, "Leo's note")
        viewModel.onResetAllPressed()

        assertEquals(listOf("Ada's note", "Leo's note"), results.map { it.note })
        assertEquals(listOf("", ""), stopwatches.take(2).map { it.note })
    }

    @Test
    fun `a reset without a completed lap keeps no result, and the note goes with the timing`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        viewModel.onNoteChanged(0, "Spikes on")
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)

        assertTrue(results.isEmpty())
        assertEquals("", stopwatches[0].note)
    }

    @Test
    fun `undo brings the note back and takes the result away`() {
        timeAndStop(0)
        viewModel.onNoteChanged(0, "Spikes on")
        val stopped = stopwatches[0]
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(stopped, stopwatches[0])
        assertEquals("Spikes on", stopwatches[0].note)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `there is no note to write on a stopwatch that does not exist`() {
        assertThrows(IllegalArgumentException::class.java) { viewModel.onNoteChanged(4, "Spikes on") }
    }

    @Test
    fun `undoing reset all brings back each stopwatch's own note`() {
        timeAndStop(0)
        timeAndStop(1)
        viewModel.onNoteChanged(0, "Ada's note")
        viewModel.onNoteChanged(1, "Leo's note")
        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(listOf("Ada's note", "Leo's note", "", ""), stopwatches.map { it.note })
        assertTrue(results.isEmpty())
    }
}

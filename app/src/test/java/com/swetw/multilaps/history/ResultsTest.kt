package com.swetw.multilaps.history

import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.timing.FakeLabelStore
import com.swetw.multilaps.timing.FakeTimingStore
import com.swetw.multilaps.timing.StopwatchColour
import com.swetw.multilaps.timing.TimingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ResultsTest {

    private val clock = FakeClock()
    private val timingStore = FakeTimingStore()
    private val labelStore = FakeLabelStore()
    private val historyStore = FakeHistoryStore()
    private var viewModel = reopened()

    private val results get() = viewModel.results.value

    private fun reopened() = TimingViewModel(clock, timingStore, labelStore, historyStore)

    /** Starts, laps after each of [laps] seconds, runs five seconds more and stops, two seconds ago. */
    private fun timeAndStop(index: Int, vararg laps: Int = intArrayOf(30)) {
        viewModel.onStartPressed(index)
        laps.forEach {
            clock.advanceBy(it.seconds)
            viewModel.onStartPressed(index)
        }
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(index)
        clock.advanceBy(2.seconds)
    }

    private fun letUndoExpire() {
        val offer = viewModel.undoOffer.value!!
        clock.advanceBy(TimingViewModel.UNDO_WINDOW)
        viewModel.onUndoExpired(offer.id)
    }

    @Test
    fun `there are no results before anything is reset`() {
        timeAndStop(0)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `a reset left to stand becomes one result, with what the stopwatch had timed`() {
        viewModel.onNameChanged(1, "Ada")
        viewModel.onColourChosen(1, StopwatchColour.Purple)
        timeAndStop(1, 30, 28)
        val resetAt = clock.wallNow()
        viewModel.onStopPressed(1)
        letUndoExpire()

        val result = results.single()
        assertEquals("Ada", result.name)
        assertEquals(StopwatchColour.Purple, result.colour)
        assertEquals(listOf(30.seconds, 28.seconds), result.laps)
        assertEquals(28.seconds, result.bestLap)
        assertEquals(2, result.lapCount)
        assertEquals(63.seconds, result.totalTime)
        assertEquals(resetAt, result.resetAt)
    }

    @Test
    fun `undoing the reset leaves no result`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertTrue(results.isEmpty())
        assertTrue(historyStore.saved.isEmpty())
    }

    @Test
    fun `a reset undone and then made again gives one result`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        letUndoExpire()

        assertEquals(1, results.size)
    }

    @Test
    fun `an undo pressed too late to count leaves the result`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        clock.advanceBy(TimingViewModel.UNDO_WINDOW)
        viewModel.onUndoPressed()

        assertEquals(1, results.size)
    }

    @Test
    fun `an undo press taken for a bounce leaves the result`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        clock.advanceBy(500.milliseconds)
        viewModel.onUndoPressed()

        assertEquals(1, results.size)
    }

    @Test
    fun `resetting a stopwatch that completed no lap gives no result`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(40.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        letUndoExpire()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `reset all gives a result for each stopwatch with laps, all at the same time`() {
        timeAndStop(0)
        timeAndStop(2, 20, 21)
        viewModel.onStartPressed(3)
        clock.advanceBy(9.seconds)
        viewModel.onStopPressed(3)
        clock.advanceBy(2.seconds)
        viewModel.onResetAllPressed()
        letUndoExpire()

        assertEquals(setOf("1", "3"), results.map { it.name }.toSet())
        assertEquals(1, results.map { it.resetAt }.toSet().size)
    }

    @Test
    fun `undoing reset all leaves no result`() {
        timeAndStop(0)
        timeAndStop(2)
        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `a stopwatch set off again before the undo keeps its result when the others are undone`() {
        timeAndStop(0)
        timeAndStop(2)
        viewModel.onResetAllPressed()
        clock.advanceBy(2.seconds)
        viewModel.onStartPressed(0)
        viewModel.onUndoPressed()

        assertEquals(listOf("1"), results.map { it.name })
    }

    @Test
    fun `undo takes back only the reset on offer, not an earlier one`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        timeAndStop(1)
        viewModel.onStopPressed(1)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(listOf("1"), results.map { it.name })
    }

    @Test
    fun `the app closing while undo is still on offer neither loses the result nor doubles it`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)

        viewModel = reopened()

        assertEquals(1, results.size)
    }

    @Test
    fun `results are still there when the app is reopened`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        letUndoExpire()
        val before = results

        viewModel = reopened()

        assertEquals(before, results)
    }

    @Test
    fun `a name and colour changed while the stopwatch is being timed are the ones its result carries`() {
        viewModel.onNameChanged(0, "Ada")
        viewModel.onColourChosen(0, StopwatchColour.Purple)
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onNameChanged(0, "Leo")
        viewModel.onStartPressed(0)
        clock.advanceBy(5.seconds)
        viewModel.onStopPressed(0)
        viewModel.onColourChosen(0, StopwatchColour.Teal)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)

        assertEquals("Leo", results.single().name)
        assertEquals(StopwatchColour.Teal, results.single().colour)
        assertEquals("Leo", historyStore.saved.single().name)
        assertEquals(StopwatchColour.Teal, historyStore.saved.single().colour)
    }

    @Test
    fun `each result carries the name and colour its stopwatch had at its own reset`() {
        viewModel.onNameChanged(0, "Ada")
        timeAndStop(0)
        viewModel.onStopPressed(0)
        letUndoExpire()
        viewModel.onNameChanged(0, "Leo")
        viewModel.onColourChosen(0, StopwatchColour.Pink)
        timeAndStop(0)
        viewModel.onStopPressed(0)

        assertEquals(listOf("Leo" to StopwatchColour.Pink, "Ada" to StopwatchColour.Blue), results.map { it.name to it.colour })
    }

    @Test
    fun `a stopwatch whose name is cleared is recorded under its number`() {
        viewModel.onNameChanged(2, "Mia")
        viewModel.onNameChanged(2, "  ")
        timeAndStop(2)
        viewModel.onStopPressed(2)

        assertEquals("3", results.single().name)
    }

    @Test
    fun `renaming or recolouring a stopwatch afterwards leaves its result as it was`() {
        viewModel.onNameChanged(0, "Ada")
        timeAndStop(0)
        viewModel.onStopPressed(0)
        letUndoExpire()

        viewModel.onNameChanged(0, "Leo")
        viewModel.onColourChosen(0, StopwatchColour.Pink)

        assertEquals("Ada", results.single().name)
        assertEquals(StopwatchColour.Blue, results.single().colour)
    }

    @Test
    fun `results are listed most recently reset first`() {
        listOf("Ada", "Leo", "Mia").forEachIndexed { index, name ->
            viewModel.onNameChanged(index, name)
            timeAndStop(index)
            viewModel.onStopPressed(index)
            letUndoExpire()
        }

        assertEquals(listOf("Mia", "Leo", "Ada"), results.map { it.name })
    }

    @Test
    fun `results reset at the same moment keep the order of their stopwatches`() {
        timeAndStop(0)
        timeAndStop(1)
        timeAndStop(2)
        viewModel.onResetAllPressed()

        assertEquals(listOf("1", "2", "3"), results.map { it.name })
    }

    @Test
    fun `every result has an identifier of its own, even after others are deleted`() {
        repeat(3) {
            timeAndStop(0)
            viewModel.onStopPressed(0)
            letUndoExpire()
        }
        viewModel.onResultDeleted(results.first().id)
        val remaining = results.map { it.id }
        timeAndStop(0)
        viewModel.onStopPressed(0)

        assertEquals(3, results.map { it.id }.toSet().size)
        assertTrue(results.first().id !in remaining)
    }

    @Test
    fun `deleting a result removes only that one, for good`() {
        timeAndStop(0)
        timeAndStop(1)
        viewModel.onResetAllPressed()
        letUndoExpire()

        viewModel.onResultDeleted(results.first { it.name == "1" }.id)

        assertEquals(listOf("2"), results.map { it.name })
        assertEquals(listOf("2"), reopened().results.value.map { it.name })
    }

    @Test
    fun `deleting a result that is not there changes nothing`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)

        viewModel.onResultDeleted(987L)

        assertEquals(1, results.size)
    }

    @Test
    fun `deleting all removes every result, for good`() {
        timeAndStop(0)
        timeAndStop(1)
        viewModel.onResetAllPressed()
        letUndoExpire()

        viewModel.onAllResultsDeleted()

        assertTrue(results.isEmpty())
        assertTrue(reopened().results.value.isEmpty())
    }

    @Test
    fun `an undo does not bring back a result that was deleted meanwhile, nor remove another`() {
        timeAndStop(0)
        viewModel.onStopPressed(0)
        letUndoExpire()
        timeAndStop(1)
        viewModel.onStopPressed(1)
        viewModel.onAllResultsDeleted()
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `a result is dated by the wall clock, which a correction to the time carries forward`() {
        clock.adjustWallClockBy(3.days)
        timeAndStop(0)
        val resetAt = clock.wallNow()
        viewModel.onStopPressed(0)

        assertEquals(resetAt, results.single().resetAt)
    }

    @Test
    fun `resetting and deleting leave the timing itself alone`() {
        timeAndStop(0)
        timeAndStop(1)
        val running = viewModel.stopwatches.value
        viewModel.onStopPressed(0)
        viewModel.onAllResultsDeleted()

        assertEquals(running[1], viewModel.stopwatches.value[1])
    }
}

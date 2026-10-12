package com.swetw.multilaps.timing

import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.Stopwatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class LabelsTest {

    private val clock = FakeClock()
    private val timingStore = FakeTimingStore()
    private val labelStore = FakeLabelStore()
    private val viewModel = reopened()

    private fun reopened() = TimingViewModel(clock, timingStore, labelStore)

    private val labels get() = viewModel.labels.value

    @Test
    fun `stopwatches are named one to four to begin with`() {
        assertEquals(listOf("1", "2", "3", "4"), labels.map { it.name })
    }

    @Test
    fun `each stopwatch begins with a colour of its own, the same every time`() {
        assertEquals(4, labels.map { it.colour }.distinct().size)
        assertEquals(
            listOf(StopwatchColour.Blue, StopwatchColour.Red, StopwatchColour.Green, StopwatchColour.Orange),
            labels.map { it.colour },
        )
        assertEquals(labels, TimingViewModel(clock, FakeTimingStore(), FakeLabelStore()).labels.value)
    }

    @Test
    fun `there are eight colours to choose from`() {
        assertEquals(8, StopwatchColour.entries.size)
    }

    @Test
    fun `a stopwatch can be renamed and recoloured without touching the others`() {
        viewModel.onNameChanged(1, "Ada")
        viewModel.onColourChosen(1, StopwatchColour.Purple)

        assertEquals(StopwatchLabel("Ada", StopwatchColour.Purple), labels[1])
        assertEquals(listOf("1", "Ada", "3", "4"), labels.map { it.name })
        assertEquals(StopwatchColour.Blue, labels[0].colour)
    }

    @Test
    fun `two stopwatches may share a colour`() {
        viewModel.onColourChosen(1, StopwatchColour.Blue)

        assertEquals(StopwatchColour.Blue, labels[0].colour)
        assertEquals(StopwatchColour.Blue, labels[1].colour)
    }

    @Test
    fun `space around a name is dropped`() {
        viewModel.onNameChanged(0, "  Ada Lovelace \n")

        assertEquals("Ada Lovelace", labels[0].name)
    }

    @Test
    fun `an empty or blank name falls back to the stopwatch's number`() {
        viewModel.onNameChanged(2, "Ada")
        viewModel.onNameChanged(2, "")
        assertEquals("3", labels[2].name)

        viewModel.onNameChanged(2, "Ada")
        viewModel.onNameChanged(2, "   ")
        assertEquals("3", labels[2].name)
    }

    @Test
    fun `a very long name is cut to twenty characters`() {
        viewModel.onNameChanged(0, "Bartholomew the Third of Somewhere")

        assertEquals("Bartholomew the Thir", labels[0].name)
        assertEquals(20, labels[0].name.length)
    }

    @Test
    fun `the cut never leaves half of a character that takes two code units`() {
        viewModel.onNameChanged(0, "Nineteen letters ab" + "🏃" + "tail")

        assertEquals("Nineteen letters ab", labels[0].name)
    }

    @Test
    fun `a character that takes two code units is kept whole when it fits`() {
        viewModel.onNameChanged(0, "Ada 🏃")

        assertEquals("Ada 🏃", labels[0].name)
    }

    @Test
    fun `saved names are tidied on the way back in`() {
        labelStore.saved = listOf(
            StopwatchLabel("", StopwatchColour.Pink),
            StopwatchLabel("  Ada  ", StopwatchColour.Pink),
            StopwatchLabel("x".repeat(500), StopwatchColour.Pink),
            StopwatchLabel("4", StopwatchColour.Pink),
        )

        assertEquals(listOf("1", "Ada", "x".repeat(20), "4"), reopened().labels.value.map { it.name })
    }

    @Test
    fun `names and colours come back after the app is killed`() {
        viewModel.onNameChanged(1, "Ada")
        viewModel.onColourChosen(3, StopwatchColour.Pink)

        val after = reopened().labels.value

        assertEquals(labels, after)
        assertEquals("Ada", after[1].name)
        assertEquals(StopwatchColour.Pink, after[3].colour)
    }

    @Test
    fun `saved labels for a different number of stopwatches are ignored`() {
        labelStore.saved = List(3) { StopwatchLabel("x", StopwatchColour.Pink) }

        assertEquals(listOf("1", "2", "3", "4"), reopened().labels.value.map { it.name })
    }

    @Test
    fun `reset, reset all and undo leave names and colours alone`() {
        viewModel.onNameChanged(0, "Ada")
        viewModel.onColourChosen(0, StopwatchColour.Teal)
        val expected = labels

        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        assertEquals(expected, labels)

        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()
        assertEquals(expected, labels)

        clock.advanceBy(2.seconds)
        viewModel.onResetAllPressed()
        assertEquals(expected, labels)
        assertEquals(Stopwatch(), viewModel.stopwatches.value[0])
    }

    @Test
    fun `undo does not take back a rename made while its offer was open`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        viewModel.onStopPressed(0)
        clock.advanceBy(2.seconds)
        viewModel.onStopPressed(0)
        viewModel.onNameChanged(0, "Ada")
        viewModel.onColourChosen(0, StopwatchColour.Yellow)
        clock.advanceBy(2.seconds)
        viewModel.onUndoPressed()

        assertEquals(StopwatchLabel("Ada", StopwatchColour.Yellow), labels[0])
        assertEquals(30.seconds, viewModel.stopwatches.value[0].totalTime(clock.now()))
    }

    @Test
    fun `renaming a stopwatch does not disturb its timing`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(30.seconds)
        val before = viewModel.stopwatches.value
        val savesBefore = timingStore.saveCount

        viewModel.onNameChanged(0, "Ada")

        assertEquals(before, viewModel.stopwatches.value)
        assertEquals(savesBefore, timingStore.saveCount)
    }

    @Test
    fun `a name or colour for a stopwatch that does not exist is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { viewModel.onNameChanged(4, "Ada") }
        assertThrows(IllegalArgumentException::class.java) {
            viewModel.onColourChosen(-1, StopwatchColour.Red)
        }
    }
}

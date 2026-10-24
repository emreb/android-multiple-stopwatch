package com.swetw.multilaps.timing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.LANDSCAPE_PHONE
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.StopwatchState
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class NotesOnTimingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clock = FakeClock()
    private val viewModel = TimingViewModel(clock, FakeTimingStore(), FakeLabelStore(), FakeHistoryStore())

    @Before
    fun setUp() {
        // A running stopwatch redraws every frame, so the test drives frames by hand.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { MultiLapsTheme { TimingScreen(viewModel) } }
    }

    @Test
    fun `a note typed in the lap sheet is written on that stopwatch alone`() {
        openLapSheet(tile = 2)
        noteField().performTextReplacement("Spikes on\nWindy")
        letSheetSettle()

        assertEquals(listOf("", "Spikes on\nWindy", "", ""), viewModel.stopwatches.value.map { it.note })
    }

    @Test
    fun `the note field is labelled, and shows the note the stopwatch already has`() {
        viewModel.onNoteChanged(1, "Spikes on")
        openLapSheet(tile = 2)

        inSheet(hasText("Note")).assertExists()
        noteField().assertEditableText("Spikes on")
    }

    @Test
    fun `a note can be written while the stopwatch runs, and timing carries on`() {
        press("Start", tile = 1)
        openLapSheet(tile = 1)
        noteField().performTextReplacement("Spikes on")
        clock.advanceBy(5.seconds)
        letSheetSettle()

        assertEquals("Spikes on", viewModel.stopwatches.value[0].note)
        assertEquals(StopwatchState.Running, viewModel.stopwatches.value[0].state)
    }

    @Test
    fun `the note field keeps what is typed, space and all`() {
        openLapSheet(tile = 1)
        noteField().performTextReplacement("Spikes ")
        letSheetSettle()

        noteField().assertEditableText("Spikes ")
        assertEquals("Spikes", viewModel.stopwatches.value[0].note)
    }

    @Test
    fun `the note field takes no more than five hundred characters`() {
        openLapSheet(tile = 1)
        noteField().performTextReplacement("a".repeat(520))
        letSheetSettle()

        noteField().assertEditableText("a".repeat(500))
    }

    @Test
    fun `a stopwatch with a note shows a mark beside its name, and one without shows none`() {
        noteMark(tile = 1).assertDoesNotExist()

        viewModel.onNoteChanged(0, "Spikes on")
        composeRule.mainClock.advanceTimeByFrame()

        noteMark(tile = 1).assertIsDisplayed()
        noteMark(tile = 2).assertDoesNotExist()
        composeRule.onNode(hasTestTag("stopwatch-name") and inTile(1)).assert(hasContentDescription("Has a note"))
    }

    @Test
    fun `the mark goes once the note is cleared, or the stopwatch is reset`() {
        viewModel.onNoteChanged(0, "Spikes on")
        viewModel.onNoteChanged(0, "  ")
        composeRule.mainClock.advanceTimeByFrame()
        noteMark(tile = 1).assertDoesNotExist()

        viewModel.onNoteChanged(1, "Spikes on")
        press("Start", tile = 2)
        clock.advanceBy(30.seconds)
        press("Lap", tile = 2)
        press("Stop", tile = 2)
        clock.advanceBy(2.seconds)
        composeRule.mainClock.advanceTimeBy(2_000)
        press("Reset", tile = 2)

        noteMark(tile = 2).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on its side a stopwatch with a note still shows the mark`() {
        viewModel.onNoteChanged(3, "Spikes on")
        composeRule.mainClock.advanceTimeByFrame()

        noteMark(tile = 4).assertIsDisplayed()
    }

    private fun noteField() = composeRule.onNodeWithTag("note-field")

    private fun noteMark(tile: Int) = composeRule.onNode(hasContentDescription("Has a note") and inTile(tile))

    private fun inTile(tile: Int): SemanticsMatcher = hasAnyAncestor(hasTestTag("stopwatch-$tile"))

    private fun inSheet(matcher: SemanticsMatcher) =
        composeRule.onNode(matcher and hasAnyAncestor(hasTestTag("lap-sheet")))

    private fun press(label: String, tile: Int) {
        composeRule.onNode(hasText(label) and inTile(tile)).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun openLapSheet(tile: Int) {
        composeRule.onNode(hasTestTag("stopwatch-name") and inTile(tile)).performClick()
        letSheetSettle()
        composeRule.onNodeWithTag("lap-sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(NOTE_ITEM)
        letSheetSettle()
    }

    /** The sheet slides in over frames and idle passes in turn; see the timing screen's tests. */
    private fun letSheetSettle() {
        repeat(60) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertEditableText(text: String) =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text)))
}

/** Where the note sits in the sheet's list: after the palette. */
private const val NOTE_ITEM = 1

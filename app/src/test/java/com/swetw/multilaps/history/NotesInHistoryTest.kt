package com.swetw.multilaps.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.Devices.SMALL_PHONE
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class NotesInHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val ada = result(1, "2026-10-12T16:42", lisbon, name = "Ada").copy(note = "Spikes on\nWindy back straight")
    private val leo = result(2, "2026-10-12T16:40", lisbon, name = "Leo")

    private var results by mutableStateOf(listOf(ada, leo))
    private val edits = mutableListOf<Pair<Long, String>>()

    private fun show(shown: List<Result> = listOf(ada, leo)) {
        results = shown
        // The note's field blinks its cursor for as long as the dialog is open, so the test drives
        // frames by hand rather than wait for a stillness that never comes.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Screen() }
        settle()
    }

    /** Stands in for the view model: what is saved is tidied as it would be, and kept. */
    @androidx.compose.runtime.Composable
    private fun Screen() {
        MultiLapsTheme {
            HistoryScreen(
                results = results,
                onNoteEdited = { id, note ->
                    edits += id to note
                    results = results.map { if (it.id == id) it.copy(note = note.trim()) else it }
                },
                onResultDeleted = {},
                onAllResultsDeleted = {},
                onBack = {},
                zone = lisbon,
            )
        }
    }

    /** Lets swipes spring back and dialogs come and go. */
    private fun settle() = repeat(SETTLE_FRAMES) {
        composeRule.mainClock.advanceTimeByFrame()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    private fun row(name: String) = composeRule.onNode(
        hasTestTag("result-row") and hasContentDescription(name, substring = true),
    )

    private fun noteMark(name: String) = composeRule.onNode(
        hasContentDescription("Has a note") and hasAnyAncestor(hasTestTag("result-row") and hasContentDescription(name, substring = true)),
        useUnmergedTree = true,
    )

    @Test
    fun `a result with a note shows a mark in the list, and one without shows none`() {
        show()

        noteMark("Ada").assertIsDisplayed()
        noteMark("Leo").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `the mark stays in sight beside the longest name on a small phone`() {
        show(listOf(ada.copy(name = "W".repeat(20))))

        noteMark("W".repeat(20)).assertIsDisplayed()
    }

    @Test
    fun `a result's row is read out as having a note`() {
        show()

        row("Ada").assert(hasContentDescription("Has a note"))
        row("Leo").assert(hasContentDescription("Has a note").not())
    }

    @Test
    fun `the list shows only the mark, not the note itself`() {
        show()

        composeRule.onNode(androidx.compose.ui.test.hasText("Spikes on", substring = true)).assertDoesNotExist()
    }

    @Test
    fun `a result's page shows its note in full`() {
        show()
        row("Ada").performClick()
        settle()

        composeRule.onNodeWithTag("result-note").assertTextEquals("Spikes on\nWindy back straight")
    }

    @Test
    fun `a result without a note shows none on its page`() {
        show()
        row("Leo").performClick()
        settle()

        composeRule.onNodeWithTag("result-note").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `a long note leaves the figures and laps in sight, and can be scrolled to its end`() {
        val lines = (1..40).joinToString("\n") { "Line $it" }
        show(listOf(ada.copy(note = lines)))
        row("Ada").performClick()
        settle()

        composeRule.onNodeWithTag("result-figures").assertIsDisplayed()
        composeRule.onAllNodesWithTag("lap-row")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("result-note").assertIsDisplayed().assert(hasScrollAction())
    }

    private fun noteDialog() = composeRule.onNodeWithTag("note-dialog")
    private fun noteDialogField() = composeRule.onNodeWithTag("note-dialog-field")

    private fun assertDialogText(text: String) = noteDialogField().assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text)),
    )

    @Test
    fun `swiping a result to the right opens its note to edit, filled in, and leaves the row where it was`() {
        show()
        val before = row("Ada").getUnclippedBoundsInRoot()
        row("Ada").performTouchInput { swipeRight() }
        settle()

        noteDialog().assertIsDisplayed()
        assertDialogText("Spikes on\nWindy back straight")
        assertEquals(before, row("Ada").getUnclippedBoundsInRoot())
        composeRule.onNodeWithText("Delete 1 result?").assertDoesNotExist()
    }

    @Test
    fun `saving hands the note to keep for that result`() {
        show()
        row("Ada").performTouchInput { swipeRight() }
        settle()
        composeRule.onNodeWithTag("save-note").performClick()
        settle()

        noteDialog().assertDoesNotExist()
        assertEquals(listOf(1L to "Spikes on\nWindy back straight"), edits)
    }

    @Test
    fun `cancelling changes nothing`() {
        show()
        row("Ada").performTouchInput { swipeRight() }
        settle()
        composeRule.onNodeWithTag("cancel-note").performClick()
        settle()

        noteDialog().assertDoesNotExist()
        assertEquals(emptyList<Pair<Long, String>>(), edits)
    }

    @Test
    fun `a result with no note opens an empty one to write`() {
        show()
        row("Leo").performTouchInput { swipeRight() }
        settle()

        assertDialogText("")
    }

    @Test
    fun `swiping to the left still asks to delete, not to edit`() {
        show()
        row("Leo").performTouchInput { swipeLeft() }
        settle()

        composeRule.onNodeWithText("Delete 1 result?").assertIsDisplayed()
        noteDialog().assertDoesNotExist()
    }

    @Test
    fun `while results are being selected a swipe to the right edits nothing`() {
        show()
        row("Ada").performTouchInput { longClick() }
        settle()
        row("Leo").performTouchInput { swipeRight() }
        settle()

        noteDialog().assertDoesNotExist()
    }

    @Test
    fun `a result's page offers to edit its note, opening the same dialog`() {
        show()
        row("Ada").performClick()
        settle()
        composeRule.onNodeWithTag("edit-note").performClick()
        settle()

        assertDialogText("Spikes on\nWindy back straight")
        composeRule.onNodeWithTag("save-note").performClick()
        settle()

        assertEquals(listOf(1L to "Spikes on\nWindy back straight"), edits)
        noteDialog().assertDoesNotExist()
        results = results.map { if (it.id == 1L) it.copy(note = "Spikes off") else it }
        settle()
        composeRule.onNodeWithTag("result-note").assertTextEquals("Spikes off")
    }

    @Test
    fun `a result without a note shows only the button to write one on its page`() {
        show()
        row("Leo").performClick()
        settle()

        composeRule.onNodeWithTag("edit-note").assertIsDisplayed()
        composeRule.onNodeWithTag("result-note").assertDoesNotExist()
    }

    @Test
    fun `a screen reader is offered to edit the note on a result, beside delete`() {
        show()
        val actions = row("Leo").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Edit note", "Delete"), actions.map { it.label })

        composeRule.runOnUiThread { actions.first().action() }
        settle()

        noteDialog().assertIsDisplayed()
    }
}

private const val SETTLE_FRAMES = 60

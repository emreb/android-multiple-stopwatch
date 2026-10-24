package com.swetw.multilaps.history

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What is typed into a result's note. The dialog around it is tested from History; typing is
 * tested here, out of the dialog's window, which Robolectric cannot keep up with while its field
 * has focus.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class NoteEditorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val saved = mutableListOf<String>()
    private var cancelled = 0

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MultiLapsTheme {
                NoteEditor(id = 1, note = "Spikes on", onSaved = { saved += it }, onCancelled = { cancelled++ })
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun field() = composeRule.onNodeWithTag("note-dialog-field")

    private fun assertFieldText(text: String) =
        field().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text)))

    @Test
    fun `it is titled as a note and starts from the note there is`() {
        composeRule.onNode(hasText("Note")).assertExists()
        assertFieldText("Spikes on")
    }

    @Test
    fun `the note is a box for a short paragraph, not a single line`() {
        val box = field().getUnclippedBoundsInRoot()

        assertTrue("note box only ${box.bottom - box.top} tall", box.bottom - box.top >= 112.dp)
    }

    @Test
    fun `save hands over what was typed, on as many lines as it was typed`() {
        field().performTextReplacement("Spikes off\nWindy")
        composeRule.onNodeWithTag("save-note").performClick()

        assertEquals(listOf("Spikes off\nWindy"), saved)
        assertEquals(0, cancelled)
    }

    @Test
    fun `cancel hands over nothing`() {
        field().performTextReplacement("Something else")
        composeRule.onNodeWithTag("cancel-note").performClick()

        assertEquals(emptyList<String>(), saved)
        assertEquals(1, cancelled)
    }

    @Test
    fun `it takes no more than five hundred characters`() {
        field().performTextReplacement("a".repeat(520))
        composeRule.mainClock.advanceTimeByFrame()

        assertFieldText("a".repeat(500))
    }
}

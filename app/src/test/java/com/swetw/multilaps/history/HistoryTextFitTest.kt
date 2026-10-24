package com.swetw.multilaps.history

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.SMALL_PHONE
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/**
 * Whether what History shows fits where it is put. Text is measured for real here, which the
 * other tests' simulated graphics do not do.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryTextFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lisbon = ZoneId.of("Europe/Lisbon")

    /** Over an hour, with twelve laps: the widest figures a result is likely to show. */
    private val long = result(1, "2026-10-12T23:42", lisbon, name = "Charlotte", laps = List(11) { 300 } + 425)

    private fun show() = composeRule.setContent {
        MultiLapsTheme {
            HistoryScreen(listOf(long), onNoteEdited = { _, _ -> }, onResultDeleted = {}, onAllResultsDeleted = {}, onBack = {}, zone = lisbon)
        }
    }

    private fun text(text: String, within: String) = composeRule.onNode(
        hasText(text, substring = true) and hasAnyAncestor(hasTestTag(within)),
        useUnmergedTree = true,
    )

    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        return layouts.single()
    }

    /** Every character is on a line that is drawn, and no line runs past the edge or is shortened. */
    private fun SemanticsNodeInteraction.assertShownWhole() {
        val layout = layout()
        val text = layout.layoutInput.text
        val lines = 0 until layout.lineCount
        assertEquals("$text is cut off", text.length, layout.getLineEnd(lines.last, visibleEnd = true))
        assertFalse("$text is shortened", lines.any(layout::isLineEllipsized))
        assertTrue("$text runs past its edge", lines.all { layout.getLineRight(it) <= layout.size.width })
    }

    private fun assertDetailFiguresShownWhole() {
        show()
        composeRule.onNode(hasTestTag("result-row")).performClick()

        text("1:02:05.00", within = "result-figures").assertShownWhole()
        text("5:00.00", within = "result-figures").assertShownWhole()
        text("12", within = "result-figures").assertShownWhole()
    }

    private fun assertRowShownWhole() {
        show()

        text("Charlotte", within = "result-row").assertShownWhole()
        text("1:02:05.00", within = "result-row").assertShownWhole()
        text("12 laps, best 5:00.00", within = "result-row").assertShownWhole()
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    fun `a total of over an hour is shown whole on a result's page, on an ordinary phone`() =
        assertDetailFiguresShownWhole()

    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun `a total of over an hour is shown whole on a result's page, on a small phone`() =
        assertDetailFiguresShownWhole()

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `a total of over an hour is shown whole on a result's page, on a small phone with enlarged text`() =
        assertDetailFiguresShownWhole()

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `a result in the list shows its whole name and figures, on a small phone with enlarged text`() =
        assertRowShownWhole()

    @Test
    @Config(qualifiers = "w360dp-h740dp-port")
    fun `a result in the list shows its whole name and figures, on an ordinary phone`() = assertRowShownWhole()
}

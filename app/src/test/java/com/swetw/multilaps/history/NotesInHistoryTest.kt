package com.swetw.multilaps.history

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

    private fun show(results: List<Result> = listOf(ada, leo)) = composeRule.setContent {
        MultiLapsTheme {
            HistoryScreen(
                results = results,
                onResultDeleted = {},
                onAllResultsDeleted = {},
                onBack = {},
                zone = lisbon,
            )
        }
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

        composeRule.onNodeWithTag("result-note").assertTextEquals("Spikes on\nWindy back straight")
    }

    @Test
    fun `a result without a note shows none on its page`() {
        show()
        row("Leo").performClick()

        composeRule.onNodeWithTag("result-note").assertDoesNotExist()
    }
}

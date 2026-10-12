package com.swetw.multilaps.history

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.LANDSCAPE_PHONE
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.Devices.SMALL_PHONE
import com.swetw.multilaps.theme.MultiLapsTheme
import com.swetw.multilaps.timing.StopwatchColour
import com.swetw.multilaps.timing.StopwatchColourKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val ada = result(
        1, "2026-10-12T16:42", lisbon, name = "Ada", colour = StopwatchColour.Purple, laps = listOf(83, 82, 85), extra = 34,
    )
    private val leo = result(2, "2026-10-12T16:40", lisbon, name = "Leo", colour = StopwatchColour.Red)
    private val mia = result(3, "2026-10-10T09:05", lisbon, name = "Mia", colour = StopwatchColour.Green)

    private var results by mutableStateOf(listOf(ada, leo, mia))
    private var wentBack = false

    private fun show() = composeRule.setContent { Screen() }

    @androidx.compose.runtime.Composable
    private fun Screen() {
        MultiLapsTheme {
            HistoryScreen(
                results = results,
                onResultDeleted = { id -> results = results.filterNot { it.id == id } },
                onAllResultsDeleted = { results = emptyList() },
                onBack = { wentBack = true },
                zone = lisbon,
            )
        }
    }

    private fun rows() = composeRule.onAllNodesWithTag("result-row")
    private fun row(name: String) = composeRule.onNode(
        androidx.compose.ui.test.hasTestTag("result-row") and hasContentDescription(name, substring = true),
    )
    private fun headings() = composeRule.onAllNodesWithTag("day-heading")
    private fun lapRows() = composeRule.onAllNodesWithTag("lap-row")
    private fun back() = composeRule.onNodeWithTag("history-back")
    private fun deleteAll() = composeRule.onNodeWithTag("delete-all")

    @Test
    fun `with no results the screen says so, and there is nothing to delete`() {
        results = emptyList()
        show()

        composeRule.onNodeWithTag("no-results").assertIsDisplayed()
        rows().assertCountEquals(0)
        deleteAll().assertIsNotEnabled()
    }

    @Test
    fun `results are listed under their days, the most recent day first`() {
        show()

        headings().assertCountEquals(2)
        headings()[0].assertTextEquals("Monday, October 12, 2026")
        headings()[1].assertTextEquals("Saturday, October 10, 2026")
        rows().assertCountEquals(3)
        val tops = listOf(headings()[0], row("Ada"), row("Leo"), headings()[1], row("Mia"))
            .map { it.getUnclippedBoundsInRoot().top }
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.toSet().size, tops.size)
    }

    @Test
    fun `a result is read out whole, with its time of day, total, laps and best lap`() {
        show()

        val spoken = row("Ada").fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
        assertEquals("Ada, 4:42 PM, total time 4:44.00, 3 laps, best lap 1:22.00", spoken.replace(' ', ' '))
        row("Leo").assertContentDescriptionContains("1 lap,")
    }

    @Test
    fun `a result is drawn in the colour its stopwatch had`() {
        show()

        assertEquals(StopwatchColour.Purple, row("Ada").fetchSemanticsNode().config[StopwatchColourKey])
        assertEquals(StopwatchColour.Green, row("Mia").fetchSemanticsNode().config[StopwatchColourKey])
    }

    @Test
    fun `tapping a result shows every lap, newest first, with the best one marked`() {
        show()
        row("Ada").performClick()

        composeRule.onNodeWithTag("result-detail").assertIsDisplayed()
        rows().assertCountEquals(0)
        lapRows().assertCountEquals(3)
        lapRows()[0].assertContentDescriptionEquals("Lap 3, 1:25.00, total time 4:10.00")
        lapRows()[1].assertContentDescriptionEquals("Lap 2, 1:22.00, total time 2:45.00, best lap")
        lapRows()[2].assertContentDescriptionEquals("Lap 1, 1:23.00, total time 1:23.00")
        composeRule.onNode(hasContentDescription("Total time 4:44.00")).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Best lap 1:22.00")).assertIsDisplayed()
    }

    @Test
    fun `back from a result returns to the list, and back from the list leaves history`() {
        show()
        row("Ada").performClick()
        back().performClick()

        rows().assertCountEquals(3)
        assertFalse(wentBack)

        back().performClick()

        assertTrue(wentBack)
    }

    @Test
    fun `a result can be deleted from its own page, after being asked, and only it goes`() {
        show()
        row("Leo").performClick()
        composeRule.onNodeWithTag("delete-result").performClick()

        composeRule.onNodeWithText("Delete 1 result?").assertIsDisplayed()
        assertEquals(3, results.size)

        composeRule.onNodeWithTag("confirm-delete").performClick()

        assertEquals(listOf(ada, mia), results)
        rows().assertCountEquals(2)
        composeRule.onNodeWithTag("result-detail").assertDoesNotExist()
    }

    @Test
    fun `deciding against deleting a result keeps it and stays on its page`() {
        show()
        row("Leo").performClick()
        composeRule.onNodeWithTag("delete-result").performClick()
        composeRule.onNodeWithTag("cancel-delete").performClick()

        assertEquals(3, results.size)
        composeRule.onNodeWithTag("result-detail").assertIsDisplayed()
        composeRule.onNodeWithText("Delete 1 result?").assertDoesNotExist()
    }

    @Test
    fun `delete all asks first, saying how many will go`() {
        show()
        deleteAll().assertIsEnabled().performClick()

        composeRule.onNodeWithText("Delete all 3 results?").assertIsDisplayed()
        composeRule.onNodeWithText("This cannot be undone.").assertIsDisplayed()
        assertEquals(3, results.size)
    }

    @Test
    fun `deciding against delete all keeps every result`() {
        show()
        deleteAll().performClick()
        composeRule.onNodeWithTag("cancel-delete").performClick()

        assertEquals(3, results.size)
        composeRule.onNodeWithText("Delete all 3 results?").assertDoesNotExist()
    }

    @Test
    fun `confirming delete all removes every result and leaves the empty message`() {
        show()
        deleteAll().performClick()
        composeRule.onNodeWithTag("confirm-delete").performClick()

        assertTrue(results.isEmpty())
        composeRule.onNodeWithTag("no-results").assertIsDisplayed()
    }

    @Test
    fun `sharing a result hands that one result, and no other, to the phone's share sheet as plain text`() {
        show()
        row("Ada").performClick()
        composeRule.onNodeWithTag("share-result").performClick()

        val chooser = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        val text = send.getStringExtra(Intent.EXTRA_TEXT)!!
        assertEquals(shareText(ada, lisbon, java.util.Locale.US, RuntimeEnvironment.getApplication().resources), text)
        assertTrue("Lap 3: 1:25.00" in text)
        assertFalse("Leo" in text)
        assertFalse("Mia" in text)
    }

    @Test
    fun `the list offers no sharing of a whole day or of everything`() {
        show()

        composeRule.onAllNodesWithTag("share-result").assertCountEquals(0)
        composeRule.onAllNodes(hasContentDescription("Share", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(androidx.compose.ui.test.hasText("Share")).assertCountEquals(0)
    }

    @Test
    fun `nothing is shared by opening history or a result`() {
        show()
        row("Ada").performClick()

        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity)
    }

    @Test
    fun `the result being looked at is still open after the screen is rebuilt, as on turning the phone`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Screen() }
        row("Ada").performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("result-detail").assertIsDisplayed()
        lapRows().assertCountEquals(3)
    }

    @Test
    fun `a result that is deleted while open gives way to the list`() {
        show()
        row("Ada").performClick()

        results = listOf(leo, mia)

        composeRule.onNodeWithTag("result-detail").assertDoesNotExist()
        rows().assertCountEquals(2)
    }

    @Test
    fun `coming back from a result leaves the list where it had been scrolled to`() {
        results = (1..60L).map { result(it, "2026-10-12T10:00", lisbon, name = "Runner $it") }
        show()
        composeRule.onNodeWithTag("result-list").performScrollToIndex(50)
        row("Runner 50,").assertIsDisplayed()

        row("Runner 50,").performClick()
        back().performClick()

        row("Runner 50,").assertIsDisplayed()
        row("Runner 1,").assertDoesNotExist()
    }

    @Test
    fun `the controls are comfortably large touch targets`() = assertControlsAreLarge()

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `the controls keep their size and stay on screen on a small phone with enlarged text`() =
        assertControlsAreLarge()

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `history works with the phone on its side`() {
        show()

        rows().assertCountEquals(3)
        row("Ada").performClick()
        lapRows().assertCountEquals(3)
    }

    private fun assertControlsAreLarge() {
        show()
        assertLargeAndOnScreen(back(), deleteAll())
        assertTrue(back().getUnclippedBoundsInRoot().right <= deleteAll().getUnclippedBoundsInRoot().left)

        row("Ada").performClick()
        val share = composeRule.onNodeWithTag("share-result")
        val delete = composeRule.onNodeWithTag("delete-result")
        assertLargeAndOnScreen(back(), share, delete)
        val shareBounds = share.getUnclippedBoundsInRoot()
        val deleteBounds = delete.getUnclippedBoundsInRoot()
        assertTrue(
            "share and delete stand clear of each other: $shareBounds, $deleteBounds",
            shareBounds.top >= deleteBounds.bottom || shareBounds.right <= deleteBounds.left,
        )
    }

    private fun assertLargeAndOnScreen(vararg controls: androidx.compose.ui.test.SemanticsNodeInteraction) {
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()
        controls.forEach {
            val bounds = it.getUnclippedBoundsInRoot()
            assertTrue("$bounds", bounds.width >= 48.dp && bounds.height >= 48.dp)
            assertTrue("$bounds in $screen", bounds.left >= 0.dp && bounds.right <= screen.right)
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertContentDescriptionContains(text: String) {
        val description = fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        assertTrue("$description", description.any { text in it })
    }
}

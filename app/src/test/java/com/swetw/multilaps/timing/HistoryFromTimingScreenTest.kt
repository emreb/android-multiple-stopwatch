package com.swetw.multilaps.timing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.BARELY_WIDE_ENOUGH
import com.swetw.multilaps.Devices.LANDSCAPE_PHONE
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.Devices.SHORT_WIDE_WINDOW
import com.swetw.multilaps.Devices.SMALL_PHONE
import com.swetw.multilaps.Devices.SMALL_PHONE_LANDSCAPE
import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.stopwatch.StopwatchState
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class HistoryFromTimingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clock = FakeClock()
    private val historyStore = FakeHistoryStore()
    private val viewModel = TimingViewModel(clock, FakeTimingStore(), FakeLabelStore(), historyStore)
    private val restoration = StateRestorationTester(composeRule)

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
        restoration.setContent { MultiLapsTheme { TimingScreen(viewModel) } }
    }

    private fun history() = composeRule.onNodeWithTag("open-history")
    private fun startAll() = composeRule.onNodeWithTag("start-all")
    private fun resetAll() = composeRule.onNodeWithTag("reset-all")
    private fun tiles() = composeRule.onAllNodesWithTag("stopwatch-1")
    private fun rows() = composeRule.onAllNodesWithTag("result-row")
    private fun inTile(tile: Int) = hasAnyAncestor(hasTestTag("stopwatch-$tile"))
    private fun text(text: String, tile: Int) = composeRule.onNode(hasText(text) and inTile(tile))

    private fun frames(count: Int = 3) = repeat(count) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun advance(duration: Duration) {
        clock.advanceBy(duration)
        frames()
    }

    private fun press(label: String, tile: Int) {
        text(label, tile).performClick()
        frames()
    }

    private fun openHistory() {
        history().performClick()
        frames()
    }

    private fun back() {
        composeRule.onNodeWithTag("history-back").performClick()
        frames()
    }

    /** Times a lap on a stopwatch, stops it and resets it. */
    private fun timeAndReset(tile: Int) {
        press("Start", tile)
        advance(30.seconds)
        press("Lap", tile)
        advance(5.seconds)
        press("Stop", tile)
        advance(2.seconds)
        press("Reset", tile)
    }

    @Test
    fun `upright, history sits between start all and reset all, above the stopwatches`() {
        val start = startAll().getUnclippedBoundsInRoot()
        val middle = history().assertIsDisplayed().assertIsEnabled().getUnclippedBoundsInRoot()
        val reset = resetAll().getUnclippedBoundsInRoot()

        assertEquals(start.top, middle.top)
        assertTrue("$start, $middle, $reset", start.right <= middle.left && middle.right <= reset.left)
        assertTrue(middle.height >= 48.dp && middle.width >= 48.dp)
        assertTrue(middle.bottom <= tiles()[0].getUnclippedBoundsInRoot().top)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on its side, history shares the column at the left, between start all and reset all`() {
        val start = startAll().getUnclippedBoundsInRoot()
        val middle = history().assertIsDisplayed().getUnclippedBoundsInRoot()
        val reset = resetAll().getUnclippedBoundsInRoot()

        assertEquals(start.left, middle.left)
        assertEquals(start.right, middle.right)
        assertTrue("$start, $middle, $reset", start.bottom < middle.top && middle.bottom < reset.top)
        assertTrue(middle.height >= 48.dp)
    }

    @Test
    @Config(qualifiers = SHORT_WIDE_WINDOW)
    fun `in a short window on its side the three controls still stand clear of one another`() =
        assertColumnHasRoom()

    @Test
    @Config(qualifiers = BARELY_WIDE_ENOUGH)
    fun `at the narrowest the column appears the three controls still stand clear of one another`() =
        assertColumnHasRoom()

    private fun assertColumnHasRoom() {
        val start = startAll().getUnclippedBoundsInRoot()
        val middle = history().getUnclippedBoundsInRoot()
        val reset = resetAll().getUnclippedBoundsInRoot()

        assertEquals(start.left, middle.left)
        assertTrue("$start, $middle, $reset", middle.top - start.bottom >= 16.dp && reset.top - middle.bottom >= 16.dp)
    }

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `on a small phone with enlarged text the three controls fit side by side on the screen`() =
        assertRowFits()

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE, fontScale = 1.5f)
    fun `on a small phone on its side with enlarged text the three controls fit above the stopwatches`() =
        assertRowFits()

    private fun assertRowFits() {
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()
        val controls = listOf(startAll(), history(), resetAll()).map { it.getUnclippedBoundsInRoot() }

        controls.forEach { assertTrue("$it", it.width >= 48.dp && it.height >= 48.dp) }
        assertTrue("$controls in $screen", controls.first().left >= 0.dp && controls.last().right <= screen.right)
        controls.zipWithNext { left, right -> assertTrue("$left, $right", left.right <= right.left) }
    }

    @Test
    fun `history opens in place of the stopwatches, and back returns to them`() {
        openHistory()

        composeRule.onNodeWithTag("history").assertIsDisplayed()
        composeRule.onNodeWithTag("no-results").assertIsDisplayed()
        tiles().assertCountEquals(0)

        back()

        composeRule.onNodeWithTag("history").assertDoesNotExist()
        tiles().assertCountEquals(1)
    }

    @Test
    fun `a stopwatch that is reset shows up in history`() {
        timeAndReset(2)
        openHistory()

        rows().assertCountEquals(1)
        composeRule.onNode(hasTestTag("result-row") and hasContentDescription("2, ", substring = true))
            .assertIsDisplayed()
    }

    @Test
    fun `timing carries on while history is open`() {
        press("Start", 1)
        advance(10.seconds)
        openHistory()
        advance(20.seconds)
        back()

        assertEquals(StopwatchState.Running, viewModel.stopwatches.value[0].state)
        composeRule.onNode(hasContentDescription("Total time 0:30.00") and inTile(1)).assertIsDisplayed()
    }

    @Test
    fun `an offer to undo still runs out while history is open`() {
        timeAndReset(1)
        text("Undo", 1).assertIsDisplayed()
        openHistory()
        clock.advanceBy(TimingViewModel.UNDO_WINDOW)
        composeRule.mainClock.advanceTimeBy(TimingViewModel.UNDO_WINDOW.inWholeMilliseconds)
        frames()

        assertNull(viewModel.undoOffer.value)

        back()
        text("Undo", 1).assertDoesNotExist()
    }

    @Test
    fun `deleting in history deletes for good`() {
        timeAndReset(1)
        timeAndReset(2)
        openHistory()
        composeRule.onNodeWithTag("delete-all").performClick()
        frames()
        composeRule.onNodeWithTag("confirm-delete").performClick()
        frames()

        assertTrue(viewModel.results.value.isEmpty())
        assertTrue(historyStore.saved.isEmpty())
        composeRule.onNodeWithTag("no-results").assertIsDisplayed()
    }

    @Test
    fun `history is still open after the screen is rebuilt, as on turning the phone`() {
        timeAndReset(1)
        openHistory()

        restoration.emulateSavedInstanceStateRestore()
        frames()

        composeRule.onNodeWithTag("history").assertIsDisplayed()
        rows().assertCountEquals(1)
    }
}

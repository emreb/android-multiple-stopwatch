package com.swetw.multilaps.timing

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.BARELY_WIDE_ENOUGH
import com.swetw.multilaps.Devices.BARELY_WIDE_ENOUGH_TALL
import com.swetw.multilaps.Devices.JUST_TOO_NARROW
import com.swetw.multilaps.Devices.OLDER_PHONE_LANDSCAPE
import com.swetw.multilaps.Devices.COMMON_PHONE_LANDSCAPE
import com.swetw.multilaps.Devices.LANDSCAPE_PHONE
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.Devices.SHORT_WIDE_WINDOW
import com.swetw.multilaps.Devices.SMALL_PHONE
import com.swetw.multilaps.Devices.SMALL_PHONE_LANDSCAPE
import com.swetw.multilaps.Devices.SPLIT_SCREEN
import com.swetw.multilaps.Devices.TABLET_LANDSCAPE
import com.swetw.multilaps.Devices.TABLET_PORTRAIT
import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class TimingScreenTest {

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
    fun `a stopwatch starts out idle with a start button`() {
        described("Current lap 0:00.00").assertIsDisplayed()
        described("Total time 0:00.00").assertIsDisplayed()
        text("Start").assertIsDisplayed()
    }

    @Test
    fun `last and best lap show a placeholder until a lap is completed`() {
        described("Last lap none").assertIsDisplayed()
        described("Best lap none").assertIsDisplayed()
        described("Laps 0").assertIsDisplayed()
        allWithText("–:––.––").assertCountEquals(2)

        press("Start")
        advance(5.seconds)

        allWithText("–:––.––").assertCountEquals(2)

        described("Last lap none").assertIsDisplayed()
        described("Best lap none").assertIsDisplayed()
    }

    @Test
    fun `while running the start button becomes lap and records laps`() {
        press("Start")
        text("Start").assertDoesNotExist()
        text("Lap").assertIsEnabled()

        advance(30.seconds)
        press("Lap")
        advance(25.seconds)
        press("Lap")
        advance(4.seconds)

        described("Current lap 0:04.00").assertIsDisplayed()
        described("Last lap 0:25.00").assertIsDisplayed()
        described("Best lap 0:25.00").assertIsDisplayed()
        described("Total time 0:59.00").assertIsDisplayed()
        described("Laps 2").assertIsDisplayed()
    }

    @Test
    fun `best lap stays the shortest when a slower lap follows`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(45.seconds)
        press("Lap")

        described("Last lap 0:45.00").assertIsDisplayed()
        described("Best lap 0:20.00").assertIsDisplayed()
    }

    @Test
    fun `each figure is drawn beside its own caption`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(45.seconds)
        press("Lap")
        advance(3.seconds)

        described("Total time 1:08.00")
            .assertTextEquals("Total", "1:08.00")
        described("Last lap 0:45.00")
            .assertTextEquals("Last", "0:45.00")
        described("Best lap 0:20.00")
            .assertTextEquals("Best", "0:20.00")
        described("Laps 2")
            .assertTextEquals("Laps", "2")
        described("Current lap 0:03.00")
            .assertTextEquals("0:03.00")
    }

    @Test
    fun `stopping leaves the lap in progress out of last and best`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(3.seconds)
        press("Stop")

        described("Current lap 0:03.00").assertIsDisplayed()
        described("Last lap 0:20.00").assertIsDisplayed()
        described("Best lap 0:20.00").assertIsDisplayed()
        described("Laps 1").assertIsDisplayed()
        text("Start").assertIsEnabled()
    }

    @Test
    fun `reset discards the laps`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        press("Stop")
        advance(1.seconds)
        press("Reset")

        described("Last lap none").assertIsDisplayed()
        described("Best lap none").assertIsDisplayed()
        described("Laps 0").assertIsDisplayed()
    }

    @Test
    fun `total time stays at zero until start is pressed`() {
        clock.advanceBy(1234.milliseconds)
        composeRule.mainClock.advanceTimeByFrame()

        totalTime("0:00.00").assertIsDisplayed()
    }

    @Test
    fun `pressing start makes the displayed total time advance`() {
        press("Start")

        advance(1234.milliseconds)
        totalTime("0:01.23").assertIsDisplayed()

        advance(60.seconds)
        totalTime("1:01.23").assertIsDisplayed()
    }

    @Test
    fun `only start can be pressed while idle`() {
        text("Start").assertIsEnabled()
        text("Stop").assertIsNotEnabled()
    }

    @Test
    fun `stop holds the time and the stop button becomes reset`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        advance(5000.milliseconds)

        totalTime("0:01.23").assertIsDisplayed()
        text("Reset").assertIsEnabled()
        text("Stop").assertDoesNotExist()
        text("Start").assertIsEnabled()
    }

    @Test
    fun `start resumes a stopped stopwatch from its held time`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        advance(5000.milliseconds)
        press("Start")
        advance(1000.milliseconds)

        totalTime("0:02.23").assertIsDisplayed()
        text("Stop").assertIsEnabled()
    }

    @Test
    fun `reset returns the stopwatch to zero`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        totalTime("0:00.00").assertIsDisplayed()
        described("Current lap 0:00.00").assertIsDisplayed()
        text("Reset").assertDoesNotExist()
        text("Start").assertIsEnabled()
    }

    @Test
    fun `times of an hour or more show hours`() {
        press("Start")
        advance(3_723_450.milliseconds)

        totalTime("1:02:03.45").assertIsDisplayed()
    }

    @Test
    fun `four idle stopwatches are shown, numbered one to four`() {
        for (tile in 1..4) {
            composeRule.onNodeWithTag("stopwatch-$tile").assertIsDisplayed()
            text(tile.toString(), tile).assertIsDisplayed()
            described("Current lap 0:00.00", tile).assertIsDisplayed()
            text("Start", tile).assertIsEnabled()
            text("Stop", tile).assertIsNotEnabled()
        }
    }

    @Test
    fun `stopwatches started at different moments run independently`() {
        press("Start", tile = 1)
        advance(10.seconds)
        press("Start", tile = 3)
        advance(5.seconds)

        totalTime("0:15.00", tile = 1).assertIsDisplayed()
        totalTime("0:00.00", tile = 2).assertIsDisplayed()
        totalTime("0:05.00", tile = 3).assertIsDisplayed()
        totalTime("0:00.00", tile = 4).assertIsDisplayed()
        text("Start", tile = 2).assertIsEnabled()
        text("Lap", tile = 3).assertIsEnabled()
    }

    @Test
    fun `a lap, stop or reset on one stopwatch leaves the others alone`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(20.seconds)
        press("Lap", tile = 2)
        advance(4.seconds)
        press("Stop", tile = 1)
        advance(6.seconds)
        press("Reset", tile = 1)

        totalTime("0:00.00", tile = 1).assertIsDisplayed()
        described("Laps 0", tile = 1).assertIsDisplayed()
        totalTime("0:30.00", tile = 2).assertIsDisplayed()
        described("Last lap 0:20.00", tile = 2).assertIsDisplayed()
        described("Laps 1", tile = 2).assertIsDisplayed()
        text("Lap", tile = 2).assertIsEnabled()
    }

    @Test
    fun `in portrait the stopwatches are tiled two by two`() = assertTwoByTwo()

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `in landscape the stopwatches sit four side by side`() = assertFourAcross()

    @Test
    @Config(qualifiers = TABLET_PORTRAIT)
    fun `an upright tablet is tiled two by two`() = assertTwoByTwo()

    @Test
    @Config(qualifiers = TABLET_LANDSCAPE)
    fun `a tablet on its side sits four side by side`() = assertFourAcross()

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE)
    fun `a small phone on its side still sits four side by side`() = assertFourAcross()

    @Test
    @Config(qualifiers = SPLIT_SCREEN)
    fun `a window wider than tall but too narrow for four tiles stays two by two`() {
        assertTwoByTwo()
        assertButtonsAreLarge()
    }

    @Test
    fun `a reset pressed straight after stop does nothing`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(300.milliseconds)
        press("Reset")

        totalTime("0:20.00").assertIsDisplayed()
        text("Reset").assertIsEnabled()
    }

    @Test
    fun `after a reset the stop button offers undo, which brings the stopwatch back`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(5.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        totalTime("0:00.00").assertIsDisplayed()
        text("Undo").assertIsEnabled()
        text("Stop").assertDoesNotExist()

        advance(1.seconds)
        press("Undo")

        totalTime("0:25.00").assertIsDisplayed()
        described("Last lap 0:20.00").assertIsDisplayed()
        described("Laps 1").assertIsDisplayed()
        text("Reset").assertIsEnabled()
        text("Undo").assertDoesNotExist()
    }

    @Test
    fun `the offer to undo goes away after a few seconds`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        composeRule.mainClock.advanceTimeBy(4_000)
        text("Undo").assertIsEnabled()

        composeRule.mainClock.advanceTimeBy(1_500)
        text("Undo").assertDoesNotExist()
        text("Stop").assertIsNotEnabled()
        totalTime("0:00.00").assertIsDisplayed()
    }

    @Test
    fun `a second reset gets its own few seconds, counted from that reset`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(20.seconds)
        press("Stop", tile = 1)
        press("Stop", tile = 2)
        advance(1.seconds)
        press("Reset", tile = 1)
        advanceBoth(3.seconds)
        press("Reset", tile = 2)

        text("Undo", tile = 1).assertDoesNotExist()
        advanceBoth(3.seconds)
        text("Undo", tile = 2).assertIsEnabled()

        advanceBoth(2500.milliseconds)
        text("Undo", tile = 2).assertDoesNotExist()
    }

    @Test
    fun `only the stopwatch that was reset offers undo`() {
        press("Start", tile = 2)
        advance(20.seconds)
        press("Stop", tile = 2)
        advance(1.seconds)
        press("Reset", tile = 2)

        text("Undo", tile = 2).assertIsEnabled()
        for (tile in listOf(1, 3, 4)) text("Undo", tile).assertDoesNotExist()
        resetAll().assertTextEquals("Reset all")
    }

    @Test
    fun `reset all can be pressed only when something is stopped and nothing is running`() {
        resetAll().assertIsDisplayed().assertIsNotEnabled()

        press("Start", tile = 1)
        resetAll().assertIsNotEnabled()

        advance(20.seconds)
        press("Stop", tile = 1)
        resetAll().assertIsEnabled()

        press("Start", tile = 2)
        resetAll().assertIsNotEnabled()
    }

    @Test
    fun `reset all resets every stopwatch and turns into undo, which brings them all back`() {
        press("Start", tile = 1)
        press("Start", tile = 3)
        advance(20.seconds)
        press("Stop", tile = 1)
        advance(5.seconds)
        press("Stop", tile = 3)
        pressResetAll()

        totalTime("0:00.00", tile = 1).assertIsDisplayed()
        totalTime("0:00.00", tile = 3).assertIsDisplayed()
        resetAll().assertTextEquals("Undo").assertIsEnabled()
        for (tile in 1..4) text("Undo", tile).assertDoesNotExist()

        advance(1.seconds)
        pressResetAll()

        totalTime("0:20.00", tile = 1).assertIsDisplayed()
        totalTime("0:25.00", tile = 3).assertIsDisplayed()
        resetAll().assertTextEquals("Reset all").assertIsEnabled()
    }

    @Test
    fun `the offer to undo reset all goes away after a few seconds`() {
        press("Start", tile = 1)
        advance(20.seconds)
        press("Stop", tile = 1)
        pressResetAll()

        composeRule.mainClock.advanceTimeBy(5_500)

        resetAll().assertTextEquals("Reset all").assertIsNotEnabled()
        totalTime("0:00.00", tile = 1).assertIsDisplayed()
    }

    @Test
    fun `upright, start all is at the top left, across from reset all`() {
        val start = startAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val reset = resetAll().getUnclippedBoundsInRoot()
        val tiles = tileBounds()

        assertTrue(tiles.all { it.top >= start.bottom })
        assertEquals("flush with the left of the tiles", tiles.first().left, start.left)
        assertTrue(start.right <= reset.left)
        assertTrue("$start is large enough to hit", start.height >= 48.dp && start.width >= 48.dp)
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on its side, start all is at the top left, beside the stopwatches`() {
        val start = startAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val tiles = tileBounds()
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertTrue("$start is left of $tiles", tiles.all { it.left >= start.right })
        assertTrue("$start is at the top", start.top < screen.height / 4)
        assertTrue("$start is at the left edge", start.left < 24.dp)
        assertTrue("tiles keep nearly the full height", tiles.all { it.height.value >= 0.9f * screen.height.value })
        assertTrue(start.height >= 48.dp && start.width >= 48.dp)
        assertButtonsAreLarge()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on its side, the two controls share one column, start all at its top and reset all at its foot`() {
        val start = startAll().getUnclippedBoundsInRoot()
        val reset = resetAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertEquals(start.left, reset.left)
        assertEquals(start.right, reset.right)
        assertTrue("well apart, so one is not hit for the other", reset.top - start.bottom >= 48.dp)
        assertTrue("$reset is at the foot", reset.bottom > screen.height * 3 / 4)
    }

    @Test
    @Config(qualifiers = COMMON_PHONE_LANDSCAPE)
    fun `on a common phone on its side stop is still the same size as start`() = assertStopMatchesStart()

    @Test
    @Config(qualifiers = COMMON_PHONE_LANDSCAPE)
    fun `on a common phone on its side the controls leave the tiles their width`() {
        // 154dp each: the window less its margins and the controls' column, shared four ways.
        assertTrue(tileBounds().all { it.width >= 154.dp })
    }

    @Test
    @Config(qualifiers = BARELY_WIDE_ENOUGH)
    fun `at the narrowest the column appears, stop is still the same size as start`() {
        assertTrue(tileBounds().all { it.left >= startAll().getUnclippedBoundsInRoot().right })
        assertStopMatchesStart()
    }

    @Test
    @Config(qualifiers = BARELY_WIDE_ENOUGH_TALL)
    fun `at the narrowest the column appears, stop is still the same size as start on a full-height tile`() {
        assertTrue(tileBounds().all { it.left >= startAll().getUnclippedBoundsInRoot().right })
        assertStopMatchesStart()
    }

    @Test
    @Config(qualifiers = JUST_TOO_NARROW)
    fun `just too narrow for the column, the controls go above and stop is still the same size as start`() {
        assertTrue(tileBounds().all { it.top >= startAll().getUnclippedBoundsInRoot().bottom })
        assertStopMatchesStart()
    }

    @Test
    @Config(qualifiers = OLDER_PHONE_LANDSCAPE)
    fun `on an older phone on its side stop is still the same size as start`() = assertStopMatchesStart()

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE)
    fun `where there is no width to spare, start all stays above the stopwatches`() {
        val start = startAll().assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(tileBounds().all { it.top >= start.bottom })
        assertTrue(start.right <= resetAll().getUnclippedBoundsInRoot().left)
    }

    @Test
    fun `start all sets every idle stopwatch off together`() {
        startAll().assertIsEnabled().assertTextEquals("Start all")

        pressStartAll()
        advance(12.seconds)

        for (tile in 1..4) {
            totalTime("0:12.00", tile).assertIsDisplayed()
            text("Lap", tile).assertIsEnabled()
        }
        startAll().assertIsNotEnabled()
    }

    @Test
    fun `start all leaves running and stopped stopwatches alone`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(20.seconds)
        press("Stop", tile = 2)
        advance(5.seconds)

        pressStartAll()
        advance(3.seconds)

        totalTime("0:28.00", tile = 1).assertIsDisplayed()
        totalTime("0:20.00", tile = 2).assertIsDisplayed()
        text("Reset", tile = 2).assertIsEnabled()
        totalTime("0:03.00", tile = 3).assertIsDisplayed()
        totalTime("0:03.00", tile = 4).assertIsDisplayed()
    }

    @Test
    fun `start all can be pressed again once a stopwatch is idle again`() {
        pressStartAll()
        startAll().assertIsNotEnabled()

        advance(20.seconds)
        press("Stop", tile = 3)
        advance(1.seconds)
        press("Reset", tile = 3)

        startAll().assertIsEnabled()
    }

    private fun startAll() = composeRule.onNodeWithTag("start-all")

    private fun pressStartAll() {
        startAll().performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `upright, reset all sits above the stopwatches`() {
        val control = resetAll().assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(tileBounds().all { it.top >= control.bottom })
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on its side, reset all sits beside the stopwatches, leaving them the full height`() {
        val control = resetAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val tiles = tileBounds()
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()

        assertTrue("tiles $tiles begin after $control", tiles.all { it.left >= control.right })
        assertTrue(
            "tiles $tiles reach nearly the full height of $screen",
            tiles.all { it.height.value >= 0.9f * screen.height.value },
        )
        assertTrue("$control is large enough to hit", control.height >= 48.dp && control.width >= 48.dp)
    }

    @Test
    @Config(qualifiers = BARELY_WIDE_ENOUGH, fontScale = 1.5f)
    fun `beside the stopwatches, the two controls take no more than they were allowed, even with enlarged text`() {
        val control = resetAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val start = startAll().assertIsDisplayed().getUnclippedBoundsInRoot()
        val tiles = tileBounds()

        assertTrue("reset all is ${control.width} wide", control.width <= 96.dp)
        assertTrue("start all is ${start.width} wide", start.width <= 96.dp)
        assertTrue(tiles.all { it.left >= control.right })
        assertTrue("tiles are ${tiles.first().width} wide", tiles.all { it.width >= 144.dp })
        assertButtonsAreLarge()
    }

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE)
    fun `where there is no width to spare, reset all stays above the stopwatches`() {
        val control = resetAll().assertIsDisplayed().getUnclippedBoundsInRoot()

        assertTrue(tileBounds().all { it.top >= control.bottom })
    }

    @Test
    @Config(qualifiers = COMMON_PHONE_LANDSCAPE)
    fun `on a common phone on its side there is room for every figure above the buttons`() {
        // Name, current lap, last, best, and total with lap count: what a short tile draws.
        val needed = 48 + 48 + 30 + 30 + 22 + 6
        for (tile in tileBounds()) {
            val buttons = text("Start", 1).getUnclippedBoundsInRoot().height.value
            val room = tile.height.value - 20 - 8 - buttons
            assertTrue("room $room for $needed", room >= needed)
        }
    }

    /** Moves the stopwatches' clock and the screen's own timers on together. */
    private fun advanceBoth(duration: Duration) {
        clock.advanceBy(duration)
        composeRule.mainClock.advanceTimeBy(duration.inWholeMilliseconds)
    }

    @Test
    fun `tapping a stopwatch's name opens its lap sheet`() {
        lapSheet().assertDoesNotExist()

        openLapSheet(tile = 2)

        lapSheet().assertIsDisplayed()
        nameField().assertTextContains("2")
    }

    @Test
    fun `pressing start or stop does not open the lap sheet`() {
        press("Start", tile = 2)
        advance(20.seconds)
        press("Lap", tile = 2)
        press("Stop", tile = 2)
        composeRule.mainClock.advanceTimeBy(1_000)

        lapSheet().assertDoesNotExist()
    }

    @Test
    fun `the lap sheet of a stopwatch without completed laps says so`() {
        press("Start", tile = 1)
        advance(5.seconds)
        openLapSheet(tile = 1)

        inSheet(hasText("No laps yet")).assertIsDisplayed()
        lapRows().assertCountEquals(0)
    }

    @Test
    fun `the lap sheet lists each lap with its number, time and total, newest first`() {
        press("Start", tile = 1)
        advance(30.seconds)
        press("Lap", tile = 1)
        advance(25.seconds)
        press("Lap", tile = 1)
        advance(40.seconds)
        press("Lap", tile = 1)
        openLapSheet(tile = 1)

        lapRows().assertCountEquals(3)
        lapRows()[0].assertTextEquals("3", "0:40.00", "1:35.00")
        lapRows()[1].assertTextEquals("2", "0:25.00", "0:55.00", "Best")
        lapRows()[2].assertTextEquals("1", "0:30.00", "0:30.00")
        inSheet(hasText("No laps yet")).assertDoesNotExist()
    }

    @Test
    fun `lap rows are read out whole, and the best lap is named`() {
        press("Start", tile = 1)
        advance(30.seconds)
        press("Lap", tile = 1)
        advance(25.seconds)
        press("Lap", tile = 1)
        openLapSheet(tile = 1)

        inSheet(hasContentDescription("Lap 2, 0:25.00, total time 0:55.00, best lap")).assertIsDisplayed()
        inSheet(hasContentDescription("Lap 1, 0:30.00, total time 0:30.00")).assertIsDisplayed()
    }

    @Test
    fun `the lap sheet shows only its own stopwatch's laps`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(30.seconds)
        press("Lap", tile = 1)
        openLapSheet(tile = 2)

        lapRows().assertCountEquals(0)
    }

    @Test
    fun `timing continues behind the lap sheet, and a lap recorded meanwhile joins the list`() {
        press("Start", tile = 1)
        press("Start", tile = 3)
        advance(30.seconds)
        press("Lap", tile = 1)
        openLapSheet(tile = 1)
        lapRows().assertCountEquals(1)

        advance(12.seconds)
        viewModel.onStartPressed(0)
        composeRule.mainClock.advanceTimeByFrame()

        lapRows().assertCountEquals(2)
        lapRows()[0].assertTextEquals("2", "0:12.00", "0:42.00", "Best")
        // The tiles are still there behind the sheet, and still counting.
        described("Total time 0:42.00", tile = 3).assertExists()
        described("Current lap 0:00.00", tile = 1).assertExists()
    }

    @Test
    fun `a lap recorded while a long lap list is open appears at the top of it`() {
        viewModel.onStartPressed(0)
        repeat(40) {
            clock.advanceBy(30.seconds)
            viewModel.onStartPressed(0)
        }
        composeRule.mainClock.advanceTimeByFrame()
        openLapSheet(tile = 1)
        lapRows()[0].assertTextContains("40")

        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(0)
        letSheetSettle()

        lapRows()[0].assertTextContains("41").assertIsDisplayed()
    }

    @Test
    fun `a long lap list can be scrolled back to the first lap`() {
        viewModel.onStartPressed(0)
        repeat(40) {
            clock.advanceBy(30.seconds)
            viewModel.onStartPressed(0)
        }
        composeRule.mainClock.advanceTimeByFrame()
        openLapSheet(tile = 1)

        // Scrolled by position: scrolling to a node waits for frames the test is holding back.
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(LAST_OF_FORTY_LAPS)
        letSheetSettle()

        inSheet(hasContentDescription("Lap 1, 0:30.00, total time 0:30.00, best lap")).assertIsDisplayed()
    }

    @Test
    fun `a stopwatch reset while its lap sheet is open shows no laps`() {
        press("Start", tile = 1)
        advance(30.seconds)
        press("Lap", tile = 1)
        press("Stop", tile = 1)
        advance(1.seconds)
        openLapSheet(tile = 1)

        viewModel.onStopPressed(0)
        letSheetSettle()

        inSheet(hasText("No laps yet")).assertIsDisplayed()
        lapRows().assertCountEquals(0)
    }

    @Test
    fun `totals of an hour or more fit in the lap list`() {
        viewModel.onStartPressed(0)
        clock.advanceBy(3_723_450.milliseconds)
        viewModel.onStartPressed(0)
        composeRule.mainClock.advanceTimeByFrame()
        openLapSheet(tile = 1)

        lapRows()[0].assertTextEquals("1", "1:02:03.45", "1:02:03.45", "Best")
    }

    @Test
    fun `the name is announced as a stopwatch, and says what tapping it does`() {
        name(tile = 2)
            .assertContentDescriptionEquals("Stopwatch 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher("has click label 'show laps'") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "show laps"
                },
            )
    }

    @Test
    fun `the lap sheet can be closed, leaving the stopwatch as it was`() {
        press("Start", tile = 1)
        advance(30.seconds)
        press("Lap", tile = 1)
        openLapSheet(tile = 1)

        inSheet(hasText("Close")).performClick()
        letSheetSettle()

        lapSheet().assertDoesNotExist()
        described("Laps 1", tile = 1).assertIsDisplayed()
        text("Lap", tile = 1).assertIsEnabled()
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `the lap sheet works in landscape`() {
        press("Start", tile = 4)
        advance(30.seconds)
        press("Lap", tile = 4)
        openLapSheet(tile = 4)
        // On its side the note's box fills the first screenful; the laps are a scroll below it.
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(FIRST_LAP_ITEM)
        letSheetSettle()

        lapSheet().assertIsDisplayed()
        lapRows().assertCountEquals(1)
        lapRows()[0].assertIsDisplayed()
    }

    @Test
    fun `the name is a touch target of its own, large enough to hit`() {
        for (tile in 1..4) {
            val bounds = name(tile).getUnclippedBoundsInRoot()
            assertTrue(bounds.height >= 48.dp)
            assertTrue(bounds.width >= 48.dp)
        }
    }

    @Test
    fun `tiles show their default names and each its own colour`() {
        val colours = (1..4).map { tileColour(it) }

        assertEquals(
            listOf(StopwatchColour.Blue, StopwatchColour.Red, StopwatchColour.Green, StopwatchColour.Orange),
            colours,
        )
        for (tile in 1..4) text(tile.toString(), tile).assertIsDisplayed()
    }

    @Test
    fun `a name typed in the lap sheet shows on the tile`() {
        openLapSheet(tile = 2)
        nameField().performTextReplacement("Ada")
        letSheetSettle()

        text("Ada", tile = 2).assertExists()
        nameField().assertTextContains("Ada")
        text("1", tile = 1).assertExists()
    }

    @Test
    fun `clearing the name puts the stopwatch's number back on the tile, leaving the field empty to type in`() {
        openLapSheet(tile = 2)
        nameField().performTextReplacement("Ada")
        nameField().performTextReplacement("")
        letSheetSettle()

        text("2", tile = 2).assertExists()
        nameField().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
    }

    @Test
    fun `the name field takes no more than twenty characters`() {
        openLapSheet(tile = 2)
        nameField().performTextReplacement("Bartholomew the Third of Somewhere")
        letSheetSettle()

        nameField().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("Bartholomew the Thir"),
            ),
        )
        text("Bartholomew the Thir", tile = 2).assertExists()
    }

    @Test
    fun `the lap sheet announces whose it is`() {
        openLapSheet(tile = 2)
        nameField().performTextReplacement("Ada")
        letSheetSettle()

        lapSheet().assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Stopwatch Ada"))
    }

    @Test
    fun `the name, close and the column headings stay put while a long lap list scrolls`() {
        viewModel.onStartPressed(0)
        repeat(40) {
            clock.advanceBy(30.seconds)
            viewModel.onStartPressed(0)
        }
        composeRule.mainClock.advanceTimeByFrame()
        openLapSheet(tile = 1)

        composeRule.onNodeWithTag("lap-list").performScrollToIndex(LAST_OF_FORTY_LAPS)
        letSheetSettle()

        nameField().assertIsDisplayed()
        inSheet(hasText("Close")).assertIsDisplayed()
        inSheet(hasText("Time")).assertIsDisplayed()
        inSheet(hasText("Total")).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE)
    fun `on a small phone turned sideways the laps can still be reached`() {
        viewModel.onStartPressed(0)
        repeat(3) {
            clock.advanceBy(30.seconds)
            viewModel.onStartPressed(0)
        }
        composeRule.mainClock.advanceTimeByFrame()
        openLapSheet(tile = 1)

        composeRule.onNodeWithTag("lap-list").performScrollToIndex(FIRST_LAP_ITEM + 2)
        letSheetSettle()

        inSheet(hasContentDescription("Lap 1, 0:30.00, total time 0:30.00, best lap")).assertIsDisplayed()
        nameField().assertIsDisplayed()
    }

    @Test
    fun `the lap sheet offers the eight colours with the stopwatch's own selected`() {
        openLapSheet(tile = 2)

        swatches().assertCountEquals(8)
        swatch("Red").assertIsSelected()
        swatch("Blue").assertIsNotSelected()
    }

    @Test
    fun `a colour chosen in the lap sheet shows on the tile`() {
        openLapSheet(tile = 2)
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(PALETTE_ITEM)
        swatch("Purple").performClick()
        letSheetSettle()

        swatch("Purple").assertIsSelected()
        swatch("Red").assertIsNotSelected()
        assertEquals(StopwatchColour.Purple, tileColour(2))
        assertEquals(StopwatchColour.Blue, tileColour(1))
    }

    @Test
    fun `colour swatches are large enough to hit`() {
        openLapSheet(tile = 1)

        for (index in 0 until 8) {
            val bounds = swatches()[index].getUnclippedBoundsInRoot()
            assertTrue(bounds.width >= 48.dp && bounds.height >= 48.dp)
        }
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `name and colour can be changed in landscape`() {
        openLapSheet(tile = 3)
        nameField().performTextReplacement("Grace")
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(PALETTE_ITEM)
        swatch("Teal").performClick()
        letSheetSettle()

        text("Grace", tile = 3).assertExists()
        assertEquals(StopwatchColour.Teal, tileColour(3))
    }

    @Test
    fun `buttons and the name are announced with the stopwatch's name`() {
        openLapSheet(tile = 2)
        nameField().performTextReplacement("Ada")
        inSheet(hasText("Close")).performClick()
        letSheetSettle()

        described("Start, stopwatch Ada", tile = 2).assertIsEnabled()
        described("Stop, stopwatch Ada", tile = 2).assertIsNotEnabled()
        name(tile = 2).assertContentDescriptionEquals("Stopwatch Ada")
        described("Start, stopwatch 1", tile = 1).assertIsEnabled()

        press("Start", tile = 2)
        described("Lap, stopwatch Ada", tile = 2).assertIsEnabled()
    }

    private fun nameField() = composeRule.onNodeWithTag("name-field")

    private fun swatches() = composeRule.onAllNodesWithTag("colour-swatch")

    private fun swatch(colour: String) =
        composeRule.onNode(hasTestTag("colour-swatch") and hasContentDescription(colour))

    private fun tileColour(tile: Int): StopwatchColour =
        composeRule.onNodeWithTag("stopwatch-$tile").fetchSemanticsNode().config[StopwatchColourKey]

    private fun name(tile: Int) = composeRule.onNode(hasTestTag("stopwatch-name") and inTile(tile))

    private fun openLapSheet(tile: Int) {
        name(tile).performClick()
        letSheetSettle()
        lapSheet().assertIsDisplayed()
    }

    /**
     * The sheet is a window of its own that measures itself before it slides, so it needs frames
     * and idle passes in turn; advancing the clock in one step leaves it off screen. A second of
     * frames is several times what the slide takes.
     */
    private fun letSheetSettle() {
        repeat(SHEET_SETTLE_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }

    private fun lapSheet() = composeRule.onNodeWithTag("lap-sheet")

    private fun inSheet(matcher: SemanticsMatcher) =
        composeRule.onNode(matcher and hasAnyAncestor(hasTestTag("lap-sheet")))

    private fun lapRows() = composeRule.onAllNodesWithTag("lap-row")

    private fun resetAll() = composeRule.onNodeWithTag("reset-all")

    private fun pressResetAll() {
        resetAll().performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun assertTwoByTwo() {
        val (one, two, three, four) = tileBounds()

        assertEquals(one.top, two.top)
        assertEquals(three.top, four.top)
        assertEquals(one.left, three.left)
        assertEquals(two.left, four.left)
        assertTrue(two.left >= one.right)
        assertTrue(three.top >= one.bottom)
        assertSameSize(listOf(one, two, three, four))
        assertInsideScreen(listOf(one, two, three, four))
    }

    private fun assertFourAcross() {
        val bounds = tileBounds()

        assertTrue(bounds.all { it.top == bounds.first().top })
        assertSameSize(bounds)
        assertTrue(bounds.zipWithNext().all { (left, right) -> right.left >= left.right })
        assertInsideScreen(bounds)
    }

    private fun assertInsideScreen(bounds: List<DpRect>) {
        val screen = composeRule.onRoot().getUnclippedBoundsInRoot()
        for (rect in bounds) {
            assertTrue(rect.left >= screen.left && rect.right <= screen.right)
            assertTrue(rect.top >= screen.top && rect.bottom <= screen.bottom)
        }
    }

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `in landscape every stopwatch shows its figures and both buttons`() {
        for (tile in 1..4) {
            described("Current lap 0:00.00", tile).assertIsDisplayed()
            described("Total time 0:00.00", tile).assertIsDisplayed()
            described("Best lap none", tile).assertIsDisplayed()
            text("Start", tile).assertIsDisplayed()
            text("Stop", tile).assertIsDisplayed()
        }
    }

    @Test
    fun `buttons are comfortably large touch targets in portrait`() = assertButtonsAreLarge()

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `buttons are comfortably large touch targets in landscape`() = assertButtonsAreLarge()

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `buttons keep their size on a small phone with enlarged text`() {
        assertButtonsAreLarge()
        for (tile in 1..4) {
            text("Start", tile).assertIsDisplayed()
            text("Stop", tile).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = SMALL_PHONE_LANDSCAPE, fontScale = 1.5f)
    fun `buttons keep their size on a small phone turned sideways with enlarged text`() {
        assertButtonsAreLarge()
        for (tile in 1..4) {
            text("Start", tile).assertIsDisplayed()
            text("Stop", tile).assertIsDisplayed()
        }
    }

    @Test
    fun `stop is the same size as start, beside it, in portrait`() = assertStopMatchesStart()

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `stop is the same size as start, beside it, in landscape`() = assertStopMatchesStart()

    @Test
    @Config(qualifiers = SMALL_PHONE, fontScale = 1.5f)
    fun `stop is the same size as start, beside it, on a small phone`() = assertStopMatchesStart()

    @Test
    fun `the two buttons stay the same size whatever they read`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        assertMatched(left = text("Stop").getUnclippedBoundsInRoot(), right = text("Lap").getUnclippedBoundsInRoot())

        press("Stop")
        advance(1.seconds)
        assertMatched(left = text("Reset").getUnclippedBoundsInRoot(), right = text("Start").getUnclippedBoundsInRoot())

        press("Reset")
        assertMatched(left = text("Undo").getUnclippedBoundsInRoot(), right = text("Start").getUnclippedBoundsInRoot())
    }

    private fun assertStopMatchesStart() {
        for (tile in 1..4) {
            assertMatched(
                left = text("Stop", tile).getUnclippedBoundsInRoot(),
                right = text("Start", tile).getUnclippedBoundsInRoot(),
            )
        }
    }

    /** A tile's two buttons: the same size, side by side on one line, with a gap between. */
    private fun assertMatched(left: DpRect, right: DpRect) {
        // An odd width cannot be halved exactly, so the two may differ by a pixel.
        assertEquals("widths", right.width.value, left.width.value, 1f)
        assertEquals("heights", right.height, left.height)
        assertTrue("a clear gap between them", right.left - left.right >= 8.dp)
        assertEquals("they stand on the same line", left.bottom, right.bottom)
    }

    @Test
    fun `on a tall tile total time and lap count each have a line`() = assertTotalAndLapCount(share = false)

    @Test
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `on a phone on its side a tile is tall enough to give them a line each`() =
        assertTotalAndLapCount(share = false)

    @Test
    @Config(qualifiers = SHORT_WIDE_WINDOW)
    fun `on a short, wide tile total time and lap count share a line`() = assertTotalAndLapCount(share = true)

    @Test
    @Config(qualifiers = COMMON_PHONE_LANDSCAPE)
    fun `on a common phone on its side total time and lap count share a line`() =
        assertTotalAndLapCount(share = true)

    private fun assertTotalAndLapCount(share: Boolean) {
        for (tile in 1..4) {
            val total = totalTime("0:00.00", tile).getUnclippedBoundsInRoot()
            val laps = described("Laps 0", tile).getUnclippedBoundsInRoot()
            if (share) {
                assertEquals(total.top, laps.top)
                assertTrue(laps.left >= total.right)
            } else {
                assertTrue("lap count sits under total time", laps.top >= total.bottom)
            }
        }
    }

    @Test
    fun `a tile says whether its stopwatch is idle, running or stopped`() {
        tileState(1).assertEquals("Idle")

        press("Start", tile = 1)
        tileState(1).assertEquals("Running")
        tileState(2).assertEquals("Idle")

        advance(20.seconds)
        press("Stop", tile = 1)
        tileState(1).assertEquals("Stopped")
    }

    /** Spoken with the name, the one part of a tile a screen reader stops on to say what it is. */
    private fun tileState(tile: Int): String =
        name(tile).fetchSemanticsNode().config[SemanticsProperties.StateDescription]

    private fun String.assertEquals(expected: String) = assertEquals(expected, this)

    @Test
    fun `undo shows its time running out`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        val atFirst = undoProgress(text("Undo"))
        assertTrue("starts nearly full: $atFirst", atFirst > 0.9f)

        advanceBoth(2500.milliseconds)
        val halfway = undoProgress(text("Undo"))
        assertTrue("about half left: $halfway", halfway in 0.4f..0.6f)
    }

    @Test
    fun `undo's time runs down with the clock, not with an animation the phone may have switched off`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        // Only the stopwatches' clock moves, by half the window, then a single frame is drawn.
        advance(2500.milliseconds)

        val halfway = undoProgress(text("Undo"))
        assertTrue("about half left: $halfway", halfway in 0.45f..0.55f)
    }

    @Test
    fun `undo stays a button to a screen reader`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        text("Undo")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun `undo after reset all shows its time running out too`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        pressResetAll()

        advanceBoth(2500.milliseconds)
        val halfway = undoProgress(resetAll())
        assertTrue("about half left: $halfway", halfway in 0.4f..0.6f)
    }

    @Test
    fun `the appearance of undo is announced to a screen reader`() {
        press("Start")
        advance(20.seconds)
        press("Stop")
        advance(1.seconds)
        press("Reset")

        text("Undo").assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    private fun undoProgress(node: SemanticsNodeInteraction): Float =
        node.fetchSemanticsNode().config[UndoTimeLeftKey]

    private fun assertButtonsAreLarge() {
        for (tile in 1..4) {
            for (label in listOf("Start", "Stop")) {
                val bounds = text(label, tile).getUnclippedBoundsInRoot()
                assertTrue("$label on $tile is ${bounds.width} wide", bounds.width >= 48.dp)
                assertTrue("$label on $tile is ${bounds.height} high", bounds.height >= 56.dp)
            }
        }
    }

    /** Sharing out the screen can leave tiles a pixel apart in size. */
    private fun assertSameSize(bounds: List<DpRect>) {
        val first = bounds.first()
        for (rect in bounds) {
            assertEquals(first.width.value, rect.width.value, 1f)
            assertEquals(first.height.value, rect.height.value, 1f)
        }
    }

    private fun tileBounds() =
        (1..4).map { composeRule.onNodeWithTag("stopwatch-$it").getUnclippedBoundsInRoot() }

    private fun inTile(tile: Int): SemanticsMatcher = hasAnyAncestor(hasTestTag("stopwatch-$tile"))

    private fun text(text: String, tile: Int = 1) = composeRule.onNode(hasText(text) and inTile(tile))

    private fun allWithText(text: String, tile: Int = 1) =
        composeRule.onAllNodes(hasText(text) and inTile(tile))

    private fun described(description: String, tile: Int = 1) =
        composeRule.onNode(hasContentDescription(description) and inTile(tile))

    private fun totalTime(formatted: String, tile: Int = 1) = described("Total time $formatted", tile)

    private fun press(label: String, tile: Int = 1) {
        text(label, tile).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(duration: Duration) {
        clock.advanceBy(duration)
        composeRule.mainClock.advanceTimeByFrame()
    }
}

private const val SHEET_SETTLE_FRAMES = 60

/** Where things sit in the sheet's list: the palette, the note, the column headings, then the laps. */
private const val PALETTE_ITEM = 0
private const val FIRST_LAP_ITEM = 3
private const val LAST_OF_FORTY_LAPS = FIRST_LAP_ITEM + 39

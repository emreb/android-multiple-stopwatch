package com.brothersbaran.multilaps.timing

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
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
import com.brothersbaran.multilaps.Devices.LANDSCAPE_PHONE
import com.brothersbaran.multilaps.Devices.PORTRAIT_PHONE
import com.brothersbaran.multilaps.Devices.SMALL_PHONE
import com.brothersbaran.multilaps.Devices.SMALL_PHONE_LANDSCAPE
import com.brothersbaran.multilaps.Devices.SPLIT_SCREEN
import com.brothersbaran.multilaps.Devices.TABLET_LANDSCAPE
import com.brothersbaran.multilaps.Devices.TABLET_PORTRAIT
import com.brothersbaran.multilaps.stopwatch.FakeClock
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
    private val viewModel = TimingViewModel(clock, FakeTimingStore(), FakeLabelStore())

    @Before
    fun setUp() {
        // A running stopwatch redraws every frame, so the test drives frames by hand.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TimingScreen(viewModel) }
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
    @Config(qualifiers = LANDSCAPE_PHONE)
    fun `reset all is on screen in landscape too`() {
        resetAll().assertIsDisplayed()
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

/** Where things sit in the sheet's list: the palette, the column headings, then the laps. */
private const val PALETTE_ITEM = 0
private const val FIRST_LAP_ITEM = 2
private const val LAST_OF_FORTY_LAPS = FIRST_LAP_ITEM + 39

package com.brothersbaran.multilaps.timing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

    @Before
    fun setUp() {
        // A running stopwatch redraws every frame, so the test drives frames by hand.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TimingScreen(TimingViewModel(clock, FakeTimingStore())) }
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
        press("Reset")

        totalTime("0:00.00").assertIsDisplayed()
        described("Current lap 0:00.00").assertIsDisplayed()
        text("Stop").assertIsNotEnabled()
        text("Reset").assertDoesNotExist()
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


package com.brothersbaran.multilaps.stopwatch

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class StopwatchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clock = FakeClock()

    @Before
    fun setUp() {
        // A running stopwatch redraws every frame, so the test drives frames by hand.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { StopwatchScreen(StopwatchViewModel(clock)) }
    }

    @Test
    fun `shows one idle stopwatch with a start button`() {
        composeRule.onNodeWithContentDescription("Current lap 0:00.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Total time 0:00.00").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `last and best lap show a placeholder until a lap is completed`() {
        composeRule.onNodeWithContentDescription("Last lap none").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap none").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Laps 0").assertIsDisplayed()
        composeRule.onAllNodesWithText("–:––.––").assertCountEquals(2)

        press("Start")
        advance(5.seconds)

        composeRule.onAllNodesWithText("–:––.––").assertCountEquals(2)

        composeRule.onNodeWithContentDescription("Last lap none").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap none").assertIsDisplayed()
    }

    @Test
    fun `while running the start button becomes lap and records laps`() {
        press("Start")
        composeRule.onNodeWithText("Start").assertDoesNotExist()
        composeRule.onNodeWithText("Lap").assertIsEnabled()

        advance(30.seconds)
        press("Lap")
        advance(25.seconds)
        press("Lap")
        advance(4.seconds)

        composeRule.onNodeWithContentDescription("Current lap 0:04.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Last lap 0:25.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap 0:25.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Total time 0:59.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Laps 2").assertIsDisplayed()
    }

    @Test
    fun `best lap stays the shortest when a slower lap follows`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(45.seconds)
        press("Lap")

        composeRule.onNodeWithContentDescription("Last lap 0:45.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap 0:20.00").assertIsDisplayed()
    }

    @Test
    fun `each figure is drawn beside its own caption`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(45.seconds)
        press("Lap")
        advance(3.seconds)

        composeRule.onNodeWithContentDescription("Total time 1:08.00")
            .assertTextEquals("Total", "1:08.00")
        composeRule.onNodeWithContentDescription("Last lap 0:45.00")
            .assertTextEquals("Last", "0:45.00")
        composeRule.onNodeWithContentDescription("Best lap 0:20.00")
            .assertTextEquals("Best", "0:20.00")
        composeRule.onNodeWithContentDescription("Laps 2")
            .assertTextEquals("Laps", "2")
        composeRule.onNodeWithContentDescription("Current lap 0:03.00")
            .assertTextEquals("0:03.00")
    }

    @Test
    fun `stopping leaves the lap in progress out of last and best`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        advance(3.seconds)
        press("Stop")

        composeRule.onNodeWithContentDescription("Current lap 0:03.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Last lap 0:20.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap 0:20.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Laps 1").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsEnabled()
    }

    @Test
    fun `reset clears the laps`() {
        press("Start")
        advance(20.seconds)
        press("Lap")
        press("Stop")
        press("Reset")

        composeRule.onNodeWithContentDescription("Last lap none").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Best lap none").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Laps 0").assertIsDisplayed()
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
        composeRule.onNodeWithText("Start").assertIsEnabled()
        composeRule.onNodeWithText("Stop").assertIsNotEnabled()
    }

    @Test
    fun `stop holds the time and the stop button becomes reset`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        advance(5000.milliseconds)

        totalTime("0:01.23").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").assertIsEnabled()
        composeRule.onNodeWithText("Stop").assertDoesNotExist()
        composeRule.onNodeWithText("Start").assertIsEnabled()
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
        composeRule.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test
    fun `reset returns the stopwatch to zero`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        press("Reset")

        totalTime("0:00.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Current lap 0:00.00").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsNotEnabled()
        composeRule.onNodeWithText("Reset").assertDoesNotExist()
    }

    @Test
    fun `times of an hour or more show hours`() {
        press("Start")
        advance(3_723_450.milliseconds)

        totalTime("1:02:03.45").assertIsDisplayed()
    }

    private fun totalTime(formatted: String) =
        composeRule.onNodeWithContentDescription("Total time $formatted")

    private fun press(label: String) {
        composeRule.onNodeWithText(label).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(duration: Duration) {
        clock.advanceBy(duration)
        composeRule.mainClock.advanceTimeByFrame()
    }
}

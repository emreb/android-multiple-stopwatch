package com.brothersbaran.multilaps.stopwatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
    fun `shows one stopwatch at zero with a start button`() {
        composeRule.onNodeWithText("0:00.00").assertIsDisplayed()
        composeRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun `total time stays at zero until start is pressed`() {
        clock.advanceBy(1234.milliseconds)
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("0:00.00").assertIsDisplayed()
    }

    @Test
    fun `pressing start makes the displayed total time advance`() {
        composeRule.onNodeWithText("Start").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        clock.advanceBy(1234.milliseconds)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("0:01.23").assertIsDisplayed()

        clock.advanceBy(60_000.milliseconds)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("1:01.23").assertIsDisplayed()
    }

    @Test
    fun `only start can be pressed while idle`() {
        composeRule.onNodeWithText("Start").assertIsEnabled()
        composeRule.onNodeWithText("Stop").assertIsNotEnabled()
    }

    @Test
    fun `only stop can be pressed while running`() {
        press("Start")

        composeRule.onNodeWithText("Start").assertIsNotEnabled()
        composeRule.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test
    fun `stop holds the time and the stop button becomes reset`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        advance(5000.milliseconds)

        composeRule.onNodeWithText("0:01.23").assertIsDisplayed()
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

        composeRule.onNodeWithText("0:02.23").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test
    fun `reset returns the stopwatch to zero`() {
        press("Start")
        advance(1234.milliseconds)
        press("Stop")
        press("Reset")

        composeRule.onNodeWithText("0:00.00").assertIsDisplayed()
        composeRule.onNodeWithText("Stop").assertIsNotEnabled()
        composeRule.onNodeWithText("Reset").assertDoesNotExist()
    }

    @Test
    fun `times of an hour or more show hours`() {
        press("Start")
        advance(3_723_450.milliseconds)

        composeRule.onNodeWithText("1:02:03.45").assertIsDisplayed()
    }

    @Test
    fun `a screen reader hears the total time and its value`() {
        composeRule.onNodeWithContentDescription("Total time 0:00.00").assertIsDisplayed()

        composeRule.onNodeWithText("Start").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        clock.advanceBy(1234.milliseconds)
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithContentDescription("Total time 0:01.23").assertIsDisplayed()
    }

    private fun press(label: String) {
        composeRule.onNodeWithText(label).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(duration: Duration) {
        clock.advanceBy(duration)
        composeRule.mainClock.advanceTimeByFrame()
    }
}

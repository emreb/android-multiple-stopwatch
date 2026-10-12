package com.brothersbaran.multilaps.stopwatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        composeRule.onNodeWithContentDescription("Total time").assertIsDisplayed()
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
}

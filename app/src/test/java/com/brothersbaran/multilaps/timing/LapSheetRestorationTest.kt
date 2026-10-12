package com.brothersbaran.multilaps.timing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brothersbaran.multilaps.Devices.PORTRAIT_PHONE
import com.brothersbaran.multilaps.stopwatch.FakeClock
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class LapSheetRestorationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clock = FakeClock()
    private val viewModel = TimingViewModel(clock, FakeTimingStore())

    @Test
    fun `the open lap sheet comes back after the screen is recreated, as on rotation`() {
        composeRule.mainClock.autoAdvance = false
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { TimingScreen(viewModel) }
        viewModel.onStartPressed(2)
        clock.advanceBy(30.seconds)
        viewModel.onStartPressed(2)
        clock.advanceBy(25.seconds)
        viewModel.onStartPressed(2)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNode(hasTestTag("stopwatch-name") and hasAnyAncestor(hasTestTag("stopwatch-3")))
            .performClick()
        settle()

        restorationTester.emulateSavedInstanceStateRestore()
        settle()

        composeRule.onNode(hasText("Stopwatch 3") and hasAnyAncestor(hasTestTag("lap-sheet")))
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag("lap-row").assertCountEquals(2)
    }

    private fun settle() {
        repeat(60) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }
}

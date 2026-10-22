package com.swetw.multilaps.timing

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.history.FakeHistoryStore
import com.swetw.multilaps.stopwatch.FakeClock
import com.swetw.multilaps.theme.MultiLapsTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class RatingFromHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clock = FakeClock()
    private val installedAt = clock.wallNow()
    private val ratingStore = FakeRatingStore()
    private val viewModel = TimingViewModel(
        clock,
        FakeTimingStore(),
        FakeLabelStore(),
        FakeHistoryStore(),
        ratingAsk = RatingAsk(installedAt, ratingStore),
    )
    private var prompts = 0
    private var lastStillWanted: () -> Boolean = { false }
    private lateinit var backDispatcher: OnBackPressedDispatcher

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MultiLapsTheme { TimingScreen(
                    viewModel,
                    ratingPrompt = { stillWanted ->
                        lastStillWanted = stillWanted
                        if (stillWanted()) prompts++
                    },
                ) }
        }
    }

    private fun frames() = repeat(3) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun openHistory() {
        composeRule.onNodeWithTag("open-history").performClick()
        frames()
    }

    private fun leaveByBackButton() {
        composeRule.onNodeWithTag("history-back").performClick()
        frames()
    }

    private fun leaveBySystemBack() {
        composeRule.runOnUiThread { backDispatcher.onBackPressed() }
        frames()
    }

    @Test
    fun `leaving History ten days after installing, with nothing running, asks for a rating`() {
        clock.advanceBy(10.days)
        openHistory()
        leaveByBackButton()

        assertEquals(1, prompts)
    }

    @Test
    fun `leaving History by the phone's back gesture asks too`() {
        clock.advanceBy(10.days)
        openHistory()
        leaveBySystemBack()

        assertEquals(1, prompts)
    }

    @Test
    fun `it is asked for once, however often History is left afterwards`() {
        clock.advanceBy(10.days)
        repeat(3) {
            openHistory()
            leaveByBackButton()
        }

        assertEquals(1, prompts)
    }

    @Test
    fun `nothing is asked while a stopwatch is running`() {
        clock.advanceBy(10.days)
        composeRule.runOnUiThread { viewModel.onStartPressed(0) }
        frames()
        openHistory()
        leaveByBackButton()

        assertEquals(0, prompts)
    }

    @Test
    fun `a stopwatch started after leaving History, before the card is up, keeps it away`() {
        clock.advanceBy(10.days)
        openHistory()
        leaveByBackButton()

        assertEquals(true, lastStillWanted())
        composeRule.runOnUiThread { viewModel.onStartPressed(0) }
        assertEquals(false, lastStillWanted())
    }

    @Test
    fun `nothing is asked in the first ten days`() {
        clock.advanceBy(9.days)
        openHistory()
        leaveByBackButton()

        assertEquals(0, prompts)
    }

    @Test
    fun `opening History does not ask`() {
        clock.advanceBy(10.days)
        openHistory()

        assertEquals(0, prompts)
    }
}

package com.swetw.multilaps

import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.Devices.PORTRAIT_PHONE
import com.swetw.multilaps.timing.StopwatchColour
import com.swetw.multilaps.timing.StopwatchColourKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class AppLifecycleTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // The test device moves its time since boot without its wall clock, so it must say which
        // boot it is on, as a phone does, for the stopwatches to stay in this boot.
        Settings.Global.putInt(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.BOOT_COUNT,
            1,
        )
        composeRule.mainClock.autoAdvance = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun `closing and reopening the app keeps what was being timed`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(seconds = 20)
        press("Lap", tile = 2)
        advance(seconds = 5)
        press("Stop", tile = 2)

        // Finishing the activity discards its view model, as the app being killed would.
        scenario.close()
        advance(seconds = 60)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.mainClock.advanceTimeByFrame()

        text("Lap", tile = 1).assertIsEnabled()
        described("Total time 1:2", tile = 1).assertExists()
        text("Reset", tile = 2).assertIsEnabled()
        described("Total time 0:25.", tile = 2).assertExists()
        described("Last lap 0:20.", tile = 2).assertExists()
        described("Laps 1", tile = 2).assertExists()
        text("Start", tile = 3).assertIsEnabled()
    }

    @Test
    fun `closing and reopening the app keeps names and colours`() {
        composeRule.onNode(hasTestTag("stopwatch-name") and inTile(2)).performClick()
        settle()
        composeRule.onNodeWithTag("name-field").performTextReplacement("Ada")
        composeRule.onNodeWithTag("lap-list").performScrollToIndex(0)
        composeRule.onNode(hasTestTag("colour-swatch") and hasContentDescription("Pink")).performClick()
        settle()
        composeRule.onNode(hasText("Close")).performClick()
        settle()

        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        settle()

        text("Ada", tile = 2).assertExists()
        assertEquals(
            StopwatchColour.Pink,
            composeRule.onNodeWithTag("stopwatch-2").fetchSemanticsNode().config[StopwatchColourKey],
        )
        text("1", tile = 1).assertExists()
    }

    @Test
    fun `closing and reopening the app keeps what was timed and reset`() {
        press("Start", tile = 2)
        advance(seconds = 20)
        press("Lap", tile = 2)
        advance(seconds = 5)
        press("Stop", tile = 2)
        advance(seconds = 2)
        press("Reset", tile = 2)

        // While undo is still on offer, which does not outlive the app.
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        settle()
        composeRule.onNodeWithTag("open-history").performClick()
        settle()

        composeRule.onAllNodesWithTag("result-row").assertCountEquals(1)
        composeRule.onNode(hasTestTag("result-row") and hasContentDescription("1 lap, best lap 0:20.", substring = true))
            .assertExists()
    }

    @Test
    fun `the phone's back button leaves history for the stopwatches, not the app`() {
        composeRule.onNodeWithTag("open-history").performClick()
        settle()
        composeRule.onNodeWithTag("history").assertExists()

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        settle()

        composeRule.onNodeWithTag("history").assertDoesNotExist()
        composeRule.onNodeWithTag("stopwatch-1").assertExists()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
    }

    private fun settle() {
        repeat(60) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `the screen is kept on exactly while a stopwatch is running`() {
        assertFalse(keepsScreenOn())

        press("Start", tile = 1)
        assertTrue(keepsScreenOn())

        press("Start", tile = 3)
        press("Stop", tile = 1)
        assertTrue(keepsScreenOn())

        press("Stop", tile = 3)
        assertFalse(keepsScreenOn())
    }

    @Test
    fun `the screen is still kept on for a running stopwatch while history is open`() {
        press("Start", tile = 1)
        composeRule.onNodeWithTag("open-history").performClick()
        settle()

        composeRule.onNodeWithTag("history").assertExists()
        assertTrue(keepsScreenOn())
    }

    private fun keepsScreenOn(): Boolean {
        var result = false
        scenario.onActivity { result = it.window.decorView.anyViewKeepsScreenOn() }
        return result
    }

    private fun View.anyViewKeepsScreenOn(): Boolean =
        keepScreenOn || (this is ViewGroup && (0 until childCount).any { getChildAt(it).anyViewKeepsScreenOn() })

    private fun inTile(tile: Int) = hasAnyAncestor(hasTestTag("stopwatch-$tile"))

    private fun text(text: String, tile: Int) = composeRule.onNode(hasText(text) and inTile(tile))

    /** The simulated system clock also moves a little with every frame, so times match by prefix. */
    private fun described(prefix: String, tile: Int) =
        composeRule.onNode(hasContentDescription(prefix, substring = true) and inTile(tile))

    private fun press(label: String, tile: Int) {
        text(label, tile).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(seconds: Long) {
        ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds))
        composeRule.mainClock.advanceTimeByFrame()
    }
}

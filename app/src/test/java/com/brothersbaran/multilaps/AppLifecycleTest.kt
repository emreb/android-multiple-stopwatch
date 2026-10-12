package com.brothersbaran.multilaps

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brothersbaran.multilaps.Devices.PORTRAIT_PHONE
import org.junit.After
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

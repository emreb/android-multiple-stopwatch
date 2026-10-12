package com.brothersbaran.multilaps

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brothersbaran.multilaps.Devices.LANDSCAPE_PHONE
import com.brothersbaran.multilaps.Devices.PORTRAIT_PHONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PORTRAIT_PHONE)
class RotationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun `rotating keeps every stopwatch's state, time and laps`() {
        press("Start", tile = 1)
        press("Start", tile = 2)
        advance(seconds = 20)
        press("Lap", tile = 1)
        press("Lap", tile = 2)
        advance(seconds = 5)
        press("Stop", tile = 2)
        advance(seconds = 3)
        val before = (1..4).map(::figures)

        rotateTo(LANDSCAPE_PHONE)
        val after = (1..4).map(::figures)

        // Tile 1 is still running: no time lost, none invented, and it keeps counting.
        text("Lap", tile = 1).assertIsEnabled()
        assertTrue(time(after[0], "Total time") >= time(before[0], "Total time"))
        assertTrue(time(after[0], "Total time") in 28.seconds..<29.seconds)
        assertTrue(time(after[0], "Current lap") in 8.seconds..<9.seconds)
        assertEquals(before[0]["Last lap"], after[0]["Last lap"])
        assertEquals("1", after[0]["Laps"])
        advance(seconds = 2)
        assertTrue(time(figures(1), "Total time") in 30.seconds..<31.seconds)

        // Tile 2 is still stopped, holding exactly the figures it had, lap included.
        text("Reset", tile = 2).assertIsEnabled()
        assertEquals(before[1], after[1])
        assertEquals("1", after[1]["Laps"])
        assertTrue(time(after[1], "Last lap") in 20.seconds..<21.seconds)
        assertTrue(time(after[1], "Current lap") in 5.seconds..<6.seconds)
        assertTrue(time(after[1], "Total time") in 25.seconds..<26.seconds)

        // Tiles 3 and 4 are still idle.
        assertEquals(before[2], after[2])
        assertEquals(before[3], after[3])
        assertEquals("0:00.00", after[2]["Total time"])
        text("Start", tile = 4).assertIsEnabled()
    }

    @Test
    fun `rotating switches between two by two and four across`() {
        assertNotEquals(tileTop(1), tileTop(3))

        rotateTo(LANDSCAPE_PHONE)
        assertEquals(tileTop(1), tileTop(3))

        rotateTo(PORTRAIT_PHONE)
        assertNotEquals(tileTop(1), tileTop(3))
    }

    private fun rotateTo(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        composeRule.activityRule.scenario.recreate()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun tileTop(tile: Int) =
        composeRule.onNodeWithTag("stopwatch-$tile").getUnclippedBoundsInRoot().top

    private fun inTile(tile: Int) = hasAnyAncestor(hasTestTag("stopwatch-$tile"))

    private fun text(text: String, tile: Int) = composeRule.onNode(hasText(text) and inTile(tile))

    /**
     * What a tile reads, keyed by figure. The simulated system clock also moves a little with
     * every frame, so times are compared with each other rather than with exact values.
     */
    private fun figures(tile: Int): Map<String, String> =
        composeRule.onAllNodes(inTile(tile))
            .fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
            .associate { it.substringBeforeLast(' ') to it.substringAfterLast(' ') }

    /** Parses a figure shown as `m:ss.hh`. */
    private fun time(figures: Map<String, String>, figure: String): Duration {
        val (minutes, seconds, hundredths) = figures.getValue(figure).split(':', '.').map(String::toInt)
        return minutes.minutes + seconds.seconds + (hundredths * 10).milliseconds
    }

    private fun press(label: String, tile: Int) {
        text(label, tile).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(seconds: Long) {
        ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(seconds))
        composeRule.mainClock.advanceTimeByFrame()
    }
}

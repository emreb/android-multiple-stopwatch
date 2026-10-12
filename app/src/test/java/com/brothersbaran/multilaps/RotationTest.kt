package com.brothersbaran.multilaps

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-port")
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
        press("Lap", tile = 2)
        advance(seconds = 5)
        press("Stop", tile = 2)
        advance(seconds = 3)
        val before = (1..4).map(::figures)

        rotateTo("w891dp-h411dp-land")
        val after = (1..4).map(::figures)

        // Tile 1 is still running: it reads no earlier than before and keeps counting.
        text("Lap", tile = 1).assertIsEnabled()
        assertTrue(after[0].getValue("Total time") >= before[0].getValue("Total time"))
        assertTrue(after[0].getValue("Total time") >= "0:28.00")
        advance(seconds = 2)
        assertTrue(figures(1).getValue("Total time") >= "0:30.00")

        // Tile 2 is still stopped, holding exactly the figures it had, lap included.
        text("Reset", tile = 2).assertIsEnabled()
        assertEquals(before[1], after[1])
        assertEquals("1", after[1]["Laps"])
        assertTrue(after[1].getValue("Last lap").startsWith("0:20."))

        // Tiles 3 and 4 are still idle.
        assertEquals(before[2], after[2])
        assertEquals(before[3], after[3])
        assertEquals("0:00.00", after[2]["Total time"])
        text("Start", tile = 4).assertIsEnabled()
    }

    @Test
    fun `rotating switches between two by two and four across`() {
        assertNotEquals(tileTop(1), tileTop(3))

        rotateTo("w891dp-h411dp-land")
        assertEquals(tileTop(1), tileTop(3))

        rotateTo("w411dp-h891dp-port")
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

    private fun described(description: String, tile: Int) =
        composeRule.onNode(hasContentDescription(description) and inTile(tile))

    /**
     * What a tile reads, keyed by figure. The simulated system clock also moves a little with
     * every frame, so times are compared with each other rather than with exact values.
     */
    private fun figures(tile: Int): Map<String, String> =
        composeRule.onAllNodes(inTile(tile))
            .fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
            .associate { it.substringBeforeLast(' ') to it.substringAfterLast(' ') }

    private fun press(label: String, tile: Int) {
        text(label, tile).performClick()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(seconds: Long) {
        ShadowSystemClock.advanceBy(Duration.ofSeconds(seconds))
        composeRule.mainClock.advanceTimeByFrame()
    }
}

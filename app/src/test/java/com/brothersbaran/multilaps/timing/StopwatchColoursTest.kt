package com.brothersbaran.multilaps.timing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchColoursTest {

    /** The contrast ratio as WCAG defines it, from 1 (none) to 21 (black on white). */
    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun `text on every colour meets the contrast needed for small text`() {
        for (colour in StopwatchColour.entries) {
            val ratio = contrast(colour.color, colour.contentColor)
            assertTrue("$colour has contrast $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun `text on every colour is whichever of black and white reads better`() {
        for (colour in StopwatchColour.entries) {
            val best = listOf(Color.Black, Color.White).maxBy { contrast(colour.color, it) }
            assertEquals("for $colour", best, colour.contentColor)
        }
    }

    @Test
    fun `every colour has a name and a shade of its own`() {
        assertEquals(StopwatchColour.entries.size, StopwatchColour.entries.map { it.color }.distinct().size)
        assertEquals(StopwatchColour.entries.size, StopwatchColour.entries.map { it.nameResource }.distinct().size)
    }
}

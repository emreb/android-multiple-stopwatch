package com.swetw.multilaps.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiLapsThemeTest {

    private val light = multiLapsColorScheme(dark = false)
    private val dark = multiLapsColorScheme(dark = true)

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    @Test
    fun `the light theme is bright and the dark theme is dark`() {
        assertTrue(light.background.luminance() > 0.9f)
        assertTrue(dark.background.luminance() < 0.05f)
    }

    @Test
    fun `text stands out strongly against its background in both themes`() {
        for (scheme in listOf(light, dark)) {
            assertTrue(contrast(scheme.onBackground, scheme.background) >= 7f)
            assertTrue(contrast(scheme.onSurface, scheme.surface) >= 7f)
            assertTrue(contrast(scheme.onSurface, scheme.surfaceContainerLow) >= 7f)
            assertTrue(contrast(scheme.onPrimary, scheme.primary) >= 7f)
        }
    }

    @Test
    fun `figures are set with digits of equal width, so they do not shift as they change`() {
        for (style in listOf(FigureStyles.currentLap, FigureStyles.lap, FigureStyles.small)) {
            assertEquals("tnum", style.fontFeatureSettings)
        }
    }

    @Test
    fun `the current lap is set far larger than any other figure`() {
        assertTrue(FigureStyles.currentLap.fontSize.value >= 2 * FigureStyles.lap.fontSize.value)
        assertTrue(FigureStyles.lap.fontSize.value > FigureStyles.small.fontSize.value)
    }
}

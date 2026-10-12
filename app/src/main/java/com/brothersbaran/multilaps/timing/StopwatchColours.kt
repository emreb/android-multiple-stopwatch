package com.brothersbaran.multilaps.timing

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsPropertyKey
import com.brothersbaran.multilaps.R

/** The colour a tile is drawn in, exposed so it can be checked without reading pixels. */
val StopwatchColourKey = SemanticsPropertyKey<StopwatchColour>("StopwatchColour")

val StopwatchColour.color: Color
    get() = when (this) {
        StopwatchColour.Red -> Color(0xFFE53935)
        StopwatchColour.Orange -> Color(0xFFFB8C00)
        StopwatchColour.Yellow -> Color(0xFFFDD835)
        StopwatchColour.Green -> Color(0xFF43A047)
        StopwatchColour.Teal -> Color(0xFF00ACC1)
        StopwatchColour.Blue -> Color(0xFF1E88E5)
        StopwatchColour.Purple -> Color(0xFF8E24AA)
        StopwatchColour.Pink -> Color(0xFFD81B60)
    }

/** Black or white, whichever stands out more against the colour. */
val StopwatchColour.contentColor: Color
    get() {
        val luminance = color.luminance()
        val againstWhite = 1.05f / (luminance + 0.05f)
        val againstBlack = (luminance + 0.05f) / 0.05f
        return if (againstBlack >= againstWhite) Color.Black else Color.White
    }

@get:StringRes
val StopwatchColour.nameResource: Int
    get() = when (this) {
        StopwatchColour.Red -> R.string.colour_red
        StopwatchColour.Orange -> R.string.colour_orange
        StopwatchColour.Yellow -> R.string.colour_yellow
        StopwatchColour.Green -> R.string.colour_green
        StopwatchColour.Teal -> R.string.colour_teal
        StopwatchColour.Blue -> R.string.colour_blue
        StopwatchColour.Purple -> R.string.colour_purple
        StopwatchColour.Pink -> R.string.colour_pink
    }

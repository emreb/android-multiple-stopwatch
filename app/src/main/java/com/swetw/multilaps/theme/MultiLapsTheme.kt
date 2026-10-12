package com.swetw.multilaps.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.swetw.multilaps.R

/**
 * Narrow and heavy, so a time can be set large in a tile a quarter of the screen wide, and with
 * digits of equal width.
 */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
)

/** Digits of equal width, so a ticking time does not jitter from side to side. */
private const val TABULAR_FIGURES = "tnum"

/** How times are set on a tile. The current lap is the one thing meant to be read at arm's length. */
object FigureStyles {

    /** The largest it is drawn; it shrinks to fit a narrower tile. */
    val currentLap = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 76.sp,
        lineHeight = 76.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )

    val lap = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )

    val small = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = TABULAR_FIGURES,
    )
}

/**
 * Plain black and white around the tiles, which carry all the colour. Sunlight washes out
 * anything subtler.
 */
fun multiLapsColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFC8C8C8),
            surfaceContainerLow = Color(0xFF1E1E1E),
            outline = Color(0xFF9A9A9A),
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF444444),
            surfaceContainerLow = Color(0xFFF0F0F0),
            outline = Color(0xFF6B6B6B),
        )
    }

private val defaults = Typography()

private fun TextStyle.inBarlow(weight: FontWeight = FontWeight.Medium) =
    copy(fontFamily = BarlowCondensed, fontWeight = weight)

private val MultiLapsTypography = Typography(
    titleLarge = defaults.titleLarge.inBarlow(FontWeight.SemiBold).copy(fontSize = 24.sp),
    titleMedium = defaults.titleMedium.inBarlow(FontWeight.SemiBold).copy(fontSize = 20.sp),
    bodyLarge = defaults.bodyLarge.inBarlow().copy(fontSize = 20.sp, fontFeatureSettings = TABULAR_FIGURES),
    bodyMedium = defaults.bodyMedium.inBarlow().copy(fontSize = 18.sp),
    bodySmall = defaults.bodySmall.inBarlow().copy(fontSize = 16.sp),
    labelLarge = defaults.labelLarge.inBarlow(FontWeight.SemiBold).copy(fontSize = 20.sp),
    labelMedium = defaults.labelMedium.inBarlow().copy(fontSize = 16.sp),
)

@Composable
fun MultiLapsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = multiLapsColorScheme(dark),
        typography = MultiLapsTypography,
        content = content,
    )
}

package com.brothersbaran.multilaps.timing

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.time.Duration

/** The shape every button on the timing screen shares. */
val BUTTON_SHAPE = RoundedCornerShape(16.dp)

/** How strongly a control that cannot be pressed is drawn. */
const val DISABLED_STRENGTH = 0.38f

private val BAR_HEIGHT = 6.dp

/**
 * The share of its time an undo has left, from one down to nothing; exposed so it can be checked
 * without reading pixels. It is not a progress bar to a screen reader: the button stays a button.
 */
val UndoTimeLeftKey = SemanticsPropertyKey<Float>("UndoTimeLeft")

/**
 * The share of an undo offer's time that is left, from one down to nothing. It is read from the
 * offer's own clock on every frame rather than animated, so it is right after a rotation and
 * runs at the true speed whatever the phone's animation setting.
 */
@Composable
fun rememberUndoShareLeft(offer: UndoOffer?, timeLeft: (UndoOffer) -> Duration): () -> Float {
    var share by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(offer?.id) {
        if (offer != null) {
            while (true) {
                share = (timeLeft(offer) / TimingViewModel.UNDO_WINDOW).toFloat()
                withFrameNanos { }
            }
        }
    }
    return { share }
}

/**
 * Marks a button as an undo whose time is running out: a bar in [ink] along its foot that
 * shortens to nothing, and word of the button to a screen reader when it appears. The bar is kept
 * clear of the label, which stays as readable as on any other button.
 */
fun Modifier.undoCountdown(shareLeft: () -> Float, ink: Color): Modifier =
    drawBehind {
        val height = BAR_HEIGHT.toPx()
        drawRect(
            color = ink,
            topLeft = Offset(0f, size.height - height),
            size = Size(size.width * shareLeft(), height),
        )
    }.semantics {
        this[UndoTimeLeftKey] = shareLeft()
        liveRegion = LiveRegionMode.Polite
    }

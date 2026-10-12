package com.brothersbaran.multilaps.timing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import kotlin.time.Duration

private const val FILL_STRENGTH = 0.28f

/**
 * The share of an undo offer's time that is left, from one down to nothing, running down
 * smoothly. It starts from the time the offer really has left, so it is right after a rotation.
 */
@Composable
fun rememberUndoTimeLeft(offer: UndoOffer?, timeLeft: (UndoOffer) -> Duration): () -> Float {
    val share = remember { Animatable(1f) }
    LaunchedEffect(offer?.id) {
        if (offer != null) {
            val left = timeLeft(offer)
            share.snapTo((left / TimingViewModel.UNDO_WINDOW).toFloat())
            share.animateTo(0f, tween(left.inWholeMilliseconds.toInt(), easing = LinearEasing))
        }
    }
    return { share.value }
}

/**
 * Marks a button as an undo whose time is running out: a wash in [ink] that drains away to the
 * left, and word of it to a screen reader when it appears.
 */
fun Modifier.undoCountdown(timeLeft: () -> Float, ink: Color): Modifier =
    drawBehind {
        drawRect(ink.copy(alpha = FILL_STRENGTH), size = Size(size.width * timeLeft(), size.height))
    }.semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(timeLeft(), 0f..1f)
        liveRegion = LiveRegionMode.Polite
    }

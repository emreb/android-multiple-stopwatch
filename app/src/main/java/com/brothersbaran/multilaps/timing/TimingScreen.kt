package com.brothersbaran.multilaps.timing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brothersbaran.multilaps.R
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import kotlinx.coroutines.delay
import kotlin.time.Duration

private const val UPRIGHT_COLUMNS = 2
private const val SIDEWAYS_COLUMNS = 4

/** Narrower than this, a tile has no room for two buttons side by side. */
private val MIN_TILE_WIDTH = 120.dp
private val GAP = 8.dp

/** All the stopwatches at once, under the one control that acts on all of them. */
@Composable
fun TimingScreen(viewModel: TimingViewModel) {
    val stopwatches by viewModel.stopwatches.collectAsStateWithLifecycle()
    val undoOffer by viewModel.undoOffer.collectAsStateWithLifecycle()
    KeepScreenOn(stopwatches.any { it.state == StopwatchState.Running })
    ExpireUndoOffer(undoOffer, viewModel::undoTimeLeft, viewModel::onUndoExpired)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(GAP)) {
            ResetAllButton(
                undoOffered = undoOffer?.fromResetAll == true,
                enabled = canResetAll(stopwatches),
                onResetAllPressed = viewModel::onResetAllPressed,
                onUndoPressed = viewModel::onUndoPressed,
                modifier = Modifier.align(Alignment.End),
            )
            StopwatchGrid(stopwatches, undoOffer, viewModel, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Side by side when the space is wider than tall and wide enough for four tiles, otherwise two by
 * two.
 */
@Composable
private fun StopwatchGrid(
    stopwatches: List<Stopwatch>,
    undoOffer: UndoOffer?,
    viewModel: TimingViewModel,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val widthForFour = MIN_TILE_WIDTH * SIDEWAYS_COLUMNS + GAP * (SIDEWAYS_COLUMNS - 1)
        val sideways = maxWidth > maxHeight && maxWidth >= widthForFour
        val columns = if (sideways) SIDEWAYS_COLUMNS else UPRIGHT_COLUMNS

        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
            stopwatches.indices.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(GAP),
                ) {
                    row.forEach { index ->
                        // Keyed so a tile keeps its own state when the grid changes shape.
                        key(index) {
                            StopwatchTile(
                                name = (index + 1).toString(),
                                stopwatch = stopwatches[index],
                                clock = viewModel.clock,
                                onStartPressed = { viewModel.onStartPressed(index) },
                                onStopPressed = { viewModel.onStopPressed(index) },
                                onUndoPressed = viewModel::onUndoPressed,
                                undoOffered = undoOffer?.fromResetAll == false &&
                                    index in undoOffer.stopwatches,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("stopwatch-${index + 1}"),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Reset all, which for a short while after it is used becomes the way to take it back. */
@Composable
private fun ResetAllButton(
    undoOffered: Boolean,
    enabled: Boolean,
    onResetAllPressed: () -> Unit,
    onUndoPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = if (undoOffered) onUndoPressed else onResetAllPressed,
        enabled = undoOffered || enabled,
        modifier = modifier.testTag("reset-all"),
    ) {
        Text(stringResource(if (undoOffered) R.string.action_undo else R.string.action_reset_all))
    }
}

/**
 * Closes an offer to undo when its time runs out. The wait is for the time the offer has left, so
 * starting over, as after a rotation, does not give it longer.
 */
@Composable
private fun ExpireUndoOffer(
    offer: UndoOffer?,
    timeLeft: (UndoOffer) -> Duration,
    onExpired: (id: Int) -> Unit,
) {
    LaunchedEffect(offer?.id) {
        if (offer != null) {
            delay(timeLeft(offer))
            onExpired(offer.id)
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

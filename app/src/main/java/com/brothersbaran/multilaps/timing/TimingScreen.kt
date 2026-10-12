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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val UPRIGHT_COLUMNS = 2
private const val SIDEWAYS_COLUMNS = 4

/** Narrower than this, a tile has no room for two buttons side by side. */
private val MIN_TILE_WIDTH = 120.dp
private val GAP = 8.dp

/**
 * All the stopwatches at once: side by side when the space is wider than tall and wide enough for
 * four tiles, otherwise two by two.
 */
@Composable
fun TimingScreen(viewModel: TimingViewModel) {
    val stopwatches by viewModel.stopwatches.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.safeDrawingPadding().padding(GAP)) {
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
}

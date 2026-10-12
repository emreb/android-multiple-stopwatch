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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val PORTRAIT_COLUMNS = 2
private const val LANDSCAPE_COLUMNS = 4

/** All the stopwatches at once: two by two when upright, side by side when turned. */
@Composable
fun TimingScreen(viewModel: TimingViewModel) {
    val stopwatches by viewModel.stopwatches.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.safeDrawingPadding().padding(8.dp)) {
            val columns = if (maxWidth > maxHeight) LANDSCAPE_COLUMNS else PORTRAIT_COLUMNS

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stopwatches.indices.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { index ->
                            StopwatchTile(
                                name = (index + 1).toString(),
                                stopwatch = stopwatches[index],
                                clock = viewModel.clock,
                                onStartPressed = { viewModel.onStartPressed(index) },
                                onStopPressed = { viewModel.onStopPressed(index) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

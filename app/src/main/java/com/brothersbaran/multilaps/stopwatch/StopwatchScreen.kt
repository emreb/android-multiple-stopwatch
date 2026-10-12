package com.brothersbaran.multilaps.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brothersbaran.multilaps.R
import kotlin.time.Duration

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel) {
    val stopwatch by viewModel.stopwatch.collectAsStateWithLifecycle()
    val totalTime = rememberTotalTime(stopwatch, viewModel::totalTime)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val formattedTotalTime = formatTime(totalTime)
            val totalTimeDescription = stringResource(R.string.total_time_description, formattedTotalTime)
            Text(
                text = formattedTotalTime,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.semantics { contentDescription = totalTimeDescription },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = viewModel::onStopPressed,
                    enabled = stopwatch.state != StopwatchState.Idle,
                ) {
                    val label = when (stopwatch.state) {
                        StopwatchState.Stopped -> R.string.action_reset
                        StopwatchState.Idle, StopwatchState.Running -> R.string.action_stop
                    }
                    Text(stringResource(label))
                }
                Button(
                    onClick = viewModel::onStartPressed,
                    enabled = stopwatch.state != StopwatchState.Running,
                ) {
                    Text(stringResource(R.string.action_start))
                }
            }
        }
    }
}

/**
 * Reads the total time afresh on every recomposition, and recomposes on every frame while the
 * stopwatch is running.
 */
@Composable
private fun rememberTotalTime(stopwatch: Stopwatch, readTotalTime: () -> Duration): Duration {
    val isRunning = stopwatch.state == StopwatchState.Running
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                withFrameNanos { frame = it }
            }
        }
    }
    // Reading the frame subscribes this composition to it.
    return remember(stopwatch, frame) { readTotalTime() }
}

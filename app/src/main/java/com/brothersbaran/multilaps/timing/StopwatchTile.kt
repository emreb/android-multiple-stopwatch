package com.brothersbaran.multilaps.timing

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brothersbaran.multilaps.R
import com.brothersbaran.multilaps.stopwatch.Clock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import com.brothersbaran.multilaps.stopwatch.formatTime
import kotlin.time.Duration

/** One stopwatch: its name, its figures, and its two buttons along the bottom edge. */
@Composable
fun StopwatchTile(
    name: String,
    stopwatch: Stopwatch,
    clock: Clock,
    onStartPressed: () -> Unit,
    onStopPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.testTag("stopwatch-$name")) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(name, style = MaterialTheme.typography.titleLarge)
            LiveTimes(stopwatch, clock)
            CompletedLaps(stopwatch)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TileButton(
                    label = when (stopwatch.state) {
                        StopwatchState.Stopped -> R.string.action_reset
                        StopwatchState.Idle, StopwatchState.Running -> R.string.action_stop
                    },
                    onClick = onStopPressed,
                    enabled = stopwatch.state != StopwatchState.Idle,
                    modifier = Modifier.weight(1f),
                )
                TileButton(
                    label = when (stopwatch.state) {
                        StopwatchState.Running -> R.string.action_lap
                        StopwatchState.Idle, StopwatchState.Stopped -> R.string.action_start
                    },
                    onClick = onStartPressed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TileButton(
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier.height(64.dp),
    ) {
        Text(stringResource(label), maxLines = 1)
    }
}

/** The times that advance while running; kept apart so only they recompose on every frame. */
@Composable
private fun LiveTimes(stopwatch: Stopwatch, clock: Clock) {
    val now = rememberNow(stopwatch, clock)
    val currentLap = formatTime(stopwatch.currentLap(now))
    val currentLapDescription = stringResource(R.string.current_lap_description, currentLap)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentLap,
            style = MaterialTheme.typography.displayLarge,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 57.sp),
            modifier = Modifier.semantics { contentDescription = currentLapDescription },
        )
        LabelledValue(
            label = R.string.total_time_label,
            value = formatTime(stopwatch.totalTime(now)),
            description = R.string.total_time_description,
        )
    }
}

@Composable
private fun CompletedLaps(stopwatch: Stopwatch) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LabelledLap(R.string.last_lap_label, R.string.last_lap_description, stopwatch.lastLap)
        LabelledLap(R.string.best_lap_label, R.string.best_lap_description, stopwatch.bestLap)
        LabelledValue(
            label = R.string.lap_count_label,
            value = stopwatch.lapCount.toString(),
            description = R.string.lap_count_description,
        )
    }
}

@Composable
private fun LabelledLap(@StringRes label: Int, @StringRes description: Int, lap: Duration?) {
    LabelledValue(
        label = label,
        value = lap?.let(::formatTime) ?: stringResource(R.string.no_lap),
        description = description,
        spokenValue = lap?.let(::formatTime) ?: stringResource(R.string.no_lap_description),
    )
}

/** A caption and its value, read out together as [description] filled with [spokenValue]. */
@Composable
private fun LabelledValue(
    @StringRes label: Int,
    value: String,
    @StringRes description: Int,
    spokenValue: String = value,
) {
    val spoken = stringResource(description, spokenValue)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Reads the clock afresh on every recomposition, and recomposes on every frame while the
 * stopwatch is running.
 */
@Composable
private fun rememberNow(stopwatch: Stopwatch, clock: Clock): Duration {
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
    return remember(stopwatch, frame) { clock.now() }
}

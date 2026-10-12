package com.brothersbaran.multilaps.timing

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
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

/** One stopwatch in its colour: its name, its figures, and its two buttons along the bottom edge. */
@Composable
fun StopwatchTile(
    label: StopwatchLabel,
    stopwatch: Stopwatch,
    clock: Clock,
    onNamePressed: () -> Unit,
    onStartPressed: () -> Unit,
    onStopPressed: () -> Unit,
    onUndoPressed: () -> Unit,
    undoOffered: Boolean,
    modifier: Modifier = Modifier,
) {
    val colour = label.colour
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colour.color,
            contentColor = colour.contentColor,
        ),
        modifier = modifier.semantics { this[StopwatchColourKey] = colour },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The figures take what the buttons leave, and scroll if that is not enough.
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TileName(label.name, onNamePressed)
                LiveTimes(stopwatch, clock)
                CompletedLaps(stopwatch)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // The button that reset the stopwatch is, for a short while, the one that undoes it.
                val undoes = undoOffered && stopwatch.state == StopwatchState.Idle
                TileButton(
                    stopwatchName = label.name,
                    colour = colour,
                    label = when {
                        undoes -> R.string.action_undo
                        stopwatch.state == StopwatchState.Stopped -> R.string.action_reset
                        else -> R.string.action_stop
                    },
                    onClick = if (undoes) onUndoPressed else onStopPressed,
                    enabled = undoes || stopwatch.state != StopwatchState.Idle,
                    modifier = Modifier.weight(1f),
                )
                TileButton(
                    stopwatchName = label.name,
                    colour = colour,
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

/** The stopwatch's name, which is also the way in to its laps. */
@Composable
private fun TileName(name: String, onPressed: () -> Unit) {
    val spoken = stringResource(R.string.stopwatch_title, name)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = stringResource(R.string.show_laps),
                role = Role.Button,
                onClick = onPressed,
            )
            .semantics { contentDescription = spoken }
            .testTag("stopwatch-name"),
    ) {
        Text(name, style = MaterialTheme.typography.titleLarge)
    }
}

/** A button in the reverse of its tile's colours, announced along with whose stopwatch it is. */
@Composable
private fun TileButton(
    stopwatchName: String,
    colour: StopwatchColour,
    @StringRes label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val action = stringResource(label)
    val spoken = stringResource(R.string.button_description, action, stopwatchName)
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colour.contentColor,
            contentColor = colour.color,
        ),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier.height(64.dp).semantics { contentDescription = spoken },
    ) {
        Text(action, maxLines = 1)
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
 * The clock reading to show: taken again whenever the stopwatch changes, and on every frame
 * while it is running.
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

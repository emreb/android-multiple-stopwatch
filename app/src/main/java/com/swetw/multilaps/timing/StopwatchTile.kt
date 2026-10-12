package com.swetw.multilaps.timing

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.swetw.multilaps.R
import com.swetw.multilaps.stopwatch.Clock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import com.swetw.multilaps.stopwatch.formatTime
import com.swetw.multilaps.theme.FigureStyles
import kotlin.time.Duration

private val TILE_SHAPE = RoundedCornerShape(20.dp)
private val PLATE_SHAPE = RoundedCornerShape(10.dp)
private val PLATE_INSET = 6.dp

/** Lap is pressed far more than anything else, so it is the bigger target in both directions. */
private val LAP_BUTTON_HEIGHT = 72.dp
private val STOP_BUTTON_HEIGHT = 56.dp
private val MIN_STOP_BUTTON_WIDTH = 48.dp
private const val STOP_BUTTON_SHARE = 0.32f

/**
 * Shorter than this, as on a phone turned sideways, a tile sets its current lap and its buttons a
 * little smaller so that every figure still fits above the buttons.
 */
private val SHORT_TILE = 330.dp
private val SHORT_TILE_LAP_BUTTON_HEIGHT = 56.dp
private val SHORT_TILE_STOP_BUTTON_HEIGHT = 48.dp
private val SHORT_TILE_CURRENT_LAP = 46.sp
private val SHORT_TILE_LAP_LINE = 27.sp
private val CLOSED_UP_CAPTION = 14.sp

/**
 * One stopwatch, solid in its colour in every state so a runner is found by colour. What it is
 * doing shows in the current lap: lighter while idle, ticking on the colour while running, set on
 * a plate once stopped.
 */
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
    undoShareLeft: () -> Float,
    modifier: Modifier = Modifier,
) {
    val colour = label.colour
    Card(
        shape = TILE_SHAPE,
        colors = CardDefaults.cardColors(
            containerColor = colour.color,
            contentColor = colour.contentColor,
        ),
        modifier = modifier.semantics { this[StopwatchColourKey] = colour },
    ) {
        BoxWithConstraints {
            val short = maxHeight < SHORT_TILE
            Column(modifier = Modifier.fillMaxSize().padding(if (short) 8.dp else 10.dp)) {
                // The figures take what the buttons leave, and scroll if that is not enough.
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    TileName(label.name, stopwatch.state, onNamePressed)
                    Figures(
                        stopwatch = stopwatch,
                        clock = clock,
                        colour = colour,
                        largestCurrentLap = if (short) SHORT_TILE_CURRENT_LAP else FigureStyles.currentLap.fontSize,
                        totalAndLapCountOnOneLine = short,
                        tight = short,
                    )
                }
                TileButtons(
                    stopwatchName = label.name,
                    colour = colour,
                    state = stopwatch.state,
                    lapButtonHeight = if (short) SHORT_TILE_LAP_BUTTON_HEIGHT else LAP_BUTTON_HEIGHT,
                    stopButtonHeight = if (short) SHORT_TILE_STOP_BUTTON_HEIGHT else STOP_BUTTON_HEIGHT,
                    undoOffered = undoOffered,
                    undoShareLeft = undoShareLeft,
                    onStartPressed = onStartPressed,
                    onStopPressed = onStopPressed,
                    onUndoPressed = onUndoPressed,
                )
            }
        }
    }
}

/**
 * The stopwatch's name, which is also the way in to its laps; the mark beside it says so. It is
 * where a screen reader stops to say whose tile this is, so it says what the stopwatch is doing
 * too.
 */
@Composable
private fun TileName(name: String, state: StopwatchState, onPressed: () -> Unit) {
    val spoken = stringResource(R.string.stopwatch_title, name)
    val spokenState = stringResource(
        when (state) {
            StopwatchState.Idle -> R.string.state_idle
            StopwatchState.Running -> R.string.state_running
            StopwatchState.Stopped -> R.string.state_stopped
        },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = stringResource(R.string.show_laps),
                role = Role.Button,
                onClick = onPressed,
            )
            .semantics {
                contentDescription = spoken
                stateDescription = spokenState
            }
            .testTag("stopwatch-name"),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ListMark()
    }
}

/** Three short lines, as a list is drawn: there are laps behind this name. */
@Composable
private fun ListMark() {
    val colour = LocalContentColor.current
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        for (line in 0..2) {
            val y = size.height * (0.25f + 0.25f * line)
            drawLine(colour, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/**
 * Every figure on the tile, read against one clock reading, so that the current lap and the total
 * time never disagree. It is what recomposes on every frame while the stopwatch runs.
 */
@Composable
private fun Figures(
    stopwatch: Stopwatch,
    clock: Clock,
    colour: StopwatchColour,
    largestCurrentLap: TextUnit,
    totalAndLapCountOnOneLine: Boolean,
    tight: Boolean,
) {
    val now = rememberNow(stopwatch, clock)
    val totalTime = formatTime(stopwatch.totalTime(now))

    CurrentLap(formatTime(stopwatch.currentLap(now)), stopwatch.state, colour, largestCurrentLap)
    // A short tile closes up the space between its lines to get them all in.
    val lapStyle = if (tight) FigureStyles.lap.copy(lineHeight = SHORT_TILE_LAP_LINE) else FigureStyles.lap
    val gap = if (tight) 0.dp else 4.dp
    Column(modifier = Modifier.padding(top = gap)) {
        LabelledLap(R.string.last_lap_label, R.string.last_lap_description, stopwatch.lastLap, lapStyle)
        LabelledLap(R.string.best_lap_label, R.string.best_lap_description, stopwatch.bestLap, lapStyle)
    }
    // The two figures looked at least, small. Each has a line to itself, since a total past an
    // hour is long. A short tile cannot spare the line, so there they share one, closed up.
    if (totalAndLapCountOnOneLine) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalTime(totalTime, closeUp = true, modifier = Modifier.weight(1f))
            LapCount(stopwatch.lapCount, closeUp = true, modifier = Modifier.width(IntrinsicSize.Max))
        }
    } else {
        Column {
            TotalTime(totalTime, modifier = Modifier.fillMaxWidth())
            LapCount(stopwatch.lapCount, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * The one figure meant to be read at arm's length, as large as its tile's width allows up to
 * [largest]. Idle, it is set lighter. Stopped, it is set on a plate in the tile's reversed
 * colours, so a time that has come to rest looks different from one still going. It keeps the
 * same inset in every state, so the digits do not move when the plate appears.
 */
@Composable
private fun CurrentLap(currentLap: String, state: StopwatchState, colour: StopwatchColour, largest: TextUnit) {
    val spoken = stringResource(R.string.current_lap_description, currentLap)
    val stopped = state == StopwatchState.Stopped

    Text(
        text = currentLap,
        style = FigureStyles.currentLap.copy(
            fontSize = largest,
            // In ems, so a time shrunk to fit a narrow tile does not keep a tall line.
            lineHeight = 1.1.em,
            fontWeight = if (state == StopwatchState.Idle) FontWeight.Medium else FontWeight.SemiBold,
        ),
        color = if (stopped) colour.color else colour.contentColor,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 28.sp, maxFontSize = largest, stepSize = 2.sp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (stopped) Modifier.background(colour.contentColor, PLATE_SHAPE) else Modifier)
            .padding(horizontal = PLATE_INSET)
            .semantics { contentDescription = spoken },
    )
}

@Composable
private fun TotalTime(totalTime: String, modifier: Modifier = Modifier, closeUp: Boolean = false) {
    LabelledValue(
        label = R.string.total_time_label,
        value = totalTime,
        description = R.string.total_time_description,
        valueStyle = FigureStyles.small,
        closeUp = closeUp,
        modifier = modifier,
    )
}

@Composable
private fun LapCount(lapCount: Int, modifier: Modifier = Modifier, closeUp: Boolean = false) {
    LabelledValue(
        label = R.string.lap_count_label,
        value = lapCount.toString(),
        description = R.string.lap_count_description,
        valueStyle = FigureStyles.small,
        closeUp = closeUp,
        modifier = modifier,
    )
}

@Composable
private fun LabelledLap(@StringRes label: Int, @StringRes description: Int, lap: Duration?, style: TextStyle) {
    LabelledValue(
        label = label,
        value = lap?.let(::formatTime) ?: stringResource(R.string.no_lap),
        description = description,
        spokenValue = lap?.let(::formatTime) ?: stringResource(R.string.no_lap_description),
        valueStyle = style,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * A caption at the left and its value at the right of whatever width [modifier] gives, read out
 * together as [description] filled with [spokenValue].
 */
@Composable
private fun LabelledValue(
    @StringRes label: Int,
    value: String,
    @StringRes description: Int,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    spokenValue: String = value,
    closeUp: Boolean = false,
) {
    val spoken = stringResource(description, spokenValue)
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (closeUp) 4.dp else 8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        // Sharing a line, the captions give ground so the values keep theirs.
        Text(
            text = stringResource(label),
            style = if (closeUp) FigureStyles.small.copy(fontSize = CLOSED_UP_CAPTION) else FigureStyles.small,
            maxLines = 1,
        )
        Text(
            text = value,
            style = valueStyle,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            // A value never loses digits to a narrow tile or enlarged text; it shrinks instead.
            autoSize = TextAutoSize.StepBased(
                minFontSize = 11.sp,
                maxFontSize = valueStyle.fontSize,
                stepSize = 1.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Stop, small, at the left; Start or Lap, large, at the right. Both stand on the same line so
 * the thumb finds them by feel.
 */
@Composable
private fun TileButtons(
    stopwatchName: String,
    colour: StopwatchColour,
    state: StopwatchState,
    lapButtonHeight: Dp,
    stopButtonHeight: Dp,
    undoOffered: Boolean,
    undoShareLeft: () -> Float,
    onStartPressed: () -> Unit,
    onStopPressed: () -> Unit,
    onUndoPressed: () -> Unit,
) {
    // The button that reset the stopwatch is, for a short while, the one that undoes it.
    val undoes = undoOffered && state == StopwatchState.Idle
    BoxWithConstraints {
        val stopWidth = max(MIN_STOP_BUTTON_WIDTH, maxWidth * STOP_BUTTON_SHARE)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            TileButton(
                stopwatchName = stopwatchName,
                label = when {
                    undoes -> R.string.action_undo
                    state == StopwatchState.Stopped -> R.string.action_reset
                    else -> R.string.action_stop
                },
                onClick = if (undoes) onUndoPressed else onStopPressed,
                enabled = undoes || state != StopwatchState.Idle,
                ink = colour.contentColor,
                paper = colour.color,
                filled = false,
                countdown = undoShareLeft.takeIf { undoes },
                modifier = Modifier.width(stopWidth).height(stopButtonHeight),
            )
            TileButton(
                stopwatchName = stopwatchName,
                label = when (state) {
                    StopwatchState.Running -> R.string.action_lap
                    StopwatchState.Idle, StopwatchState.Stopped -> R.string.action_start
                },
                onClick = onStartPressed,
                ink = colour.contentColor,
                paper = colour.color,
                filled = true,
                modifier = Modifier.weight(1f).height(lapButtonHeight),
            )
        }
    }
}

/**
 * A button on a tile, announced along with whose stopwatch it is. A [filled] one is solid in the
 * tile's [ink] with its label in the tile's own colour, [paper], and is the one to reach for; the
 * other is an outline.
 */
@Composable
private fun TileButton(
    stopwatchName: String,
    @StringRes label: Int,
    onClick: () -> Unit,
    ink: Color,
    paper: Color,
    filled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    countdown: (() -> Float)? = null,
) {
    val action = stringResource(label)
    val spoken = stringResource(R.string.button_description, action, stopwatchName)
    val strength = if (enabled) 1f else DISABLED_STRENGTH
    val labelColour = if (filled) paper else ink.copy(alpha = strength)
    val labelStyle = if (filled) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium

    // The press ripple takes the content colour, so it is given the label's, which shows on the
    // button; the tile's own would be ink on ink on the filled one.
    CompositionLocalProvider(LocalContentColor provides labelColour) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(BUTTON_SHAPE)
                .then(
                    if (filled) {
                        Modifier.background(ink.copy(alpha = strength))
                    } else {
                        Modifier.border(2.dp, ink.copy(alpha = strength), BUTTON_SHAPE)
                    },
                )
                .then(if (countdown != null) Modifier.undoCountdown(countdown, ink) else Modifier)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = spoken },
        ) {
            // Shrinks rather than loses letters where the button is narrow or the text enlarged.
            Text(
                text = action,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 11.sp,
                    maxFontSize = labelStyle.fontSize,
                    stepSize = 1.sp,
                ),
            )
        }
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

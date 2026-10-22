package com.swetw.multilaps.timing

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swetw.multilaps.R
import com.swetw.multilaps.history.HistoryScreen
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import kotlinx.coroutines.delay
import kotlin.time.Duration

private const val UPRIGHT_COLUMNS = 2
private const val SIDEWAYS_COLUMNS = 4

/** Narrower than this, a tile has no room for two buttons side by side. */
private val MIN_TILE_WIDTH = 120.dp
private val GAP = 8.dp

/**
 * What the two controls are given in a column beside the tiles, gap included. They are held to
 * this width, shrinking their labels if
 * need be, so that enlarged text cannot take from the tiles more than was allowed for.
 */
private val WIDTH_FOR_CONTROLS = 104.dp

/**
 * The narrowest a tile may be left by the controls' column, so that its figures and its two
 * buttons keep some room; where the tiles would be narrower the controls go above them instead,
 * where they cost height but no width.
 */
private val MIN_TILE_WIDTH_BESIDE_CONTROLS = 144.dp

/**
 * All the stopwatches at once, with the two controls that act on all of them and the way into
 * history between the two. Upright, they sit above the tiles, start all at the top left and reset
 * all at the top right. On its side with width to spare, where height is what the tiles are short
 * of, they share one column at the left, start all at its top and reset all at its foot: one
 * column, because a second would take too much width from the tiles.
 */
@Composable
fun TimingScreen(viewModel: TimingViewModel, ratingPrompt: RatingPrompt = RatingPrompt.None) {
    val stopwatches by viewModel.stopwatches.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val undoOffer by viewModel.undoOffer.collectAsStateWithLifecycle()
    KeepScreenOn(stopwatches.any { it.state == StopwatchState.Running })
    ExpireUndoOffer(undoOffer, viewModel::undoTimeLeft, viewModel::onUndoExpired)
    val undoShareLeft = rememberUndoShareLeft(undoOffer, viewModel::undoTimeLeft)
    // Which stopwatch's laps are being looked at, by position; kept through a rotation.
    var lapSheetFor by rememberSaveable { mutableStateOf<Int?>(null) }
    // History takes the place of the stopwatches, which go on being timed, and their offer to
    // undo timed out, by what is above.
    var showingHistory by rememberSaveable { mutableStateOf(false) }
    if (showingHistory) {
        val results by viewModel.results.collectAsStateWithLifecycle()
        HistoryScreen(
            results = results,
            onResultDeleted = viewModel::onResultDeleted,
            onAllResultsDeleted = viewModel::onAllResultsDeleted,
            onBack = {
                showingHistory = false
                if (viewModel.onHistoryLeft()) ratingPrompt.show(stillWanted = { !viewModel.isAnyRunning() })
            },
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.safeDrawingPadding().padding(GAP)) {
            // Side by side when the space is wider than tall and wide enough for four tiles,
            // otherwise two by two.
            val widthForFour = MIN_TILE_WIDTH * SIDEWAYS_COLUMNS + GAP * (SIDEWAYS_COLUMNS - 1)
            val sideways = maxWidth > maxHeight && maxWidth >= widthForFour
            val widthForFourBesideControls =
                MIN_TILE_WIDTH_BESIDE_CONTROLS * SIDEWAYS_COLUMNS + GAP * (SIDEWAYS_COLUMNS - 1) + WIDTH_FOR_CONTROLS
            val controlsBeside = sideways && maxWidth >= widthForFourBesideControls

            val startAll: @Composable (Modifier) -> Unit = { modifier ->
                ScreenButton(
                    label = R.string.action_start_all,
                    enabled = canStartAll(stopwatches),
                    outlined = true,
                    onClick = viewModel::onStartAllPressed,
                    modifier = modifier.testTag("start-all"),
                )
            }
            val history: @Composable (Modifier) -> Unit = { modifier ->
                ScreenButton(
                    label = R.string.action_history,
                    enabled = true,
                    outlined = false,
                    onClick = { showingHistory = true },
                    modifier = modifier.testTag("open-history"),
                )
            }
            val resetAll: @Composable (Modifier) -> Unit = { modifier ->
                ResetAllButton(
                    undoOffered = undoOffer?.fromResetAll == true,
                    undoShareLeft = undoShareLeft,
                    enabled = canResetAll(stopwatches),
                    onResetAllPressed = viewModel::onResetAllPressed,
                    onUndoPressed = viewModel::onUndoPressed,
                    modifier = modifier,
                )
            }
            val grid: @Composable (Modifier) -> Unit = { modifier ->
                StopwatchGrid(
                    columns = if (sideways) SIDEWAYS_COLUMNS else UPRIGHT_COLUMNS,
                    stopwatches = stopwatches,
                    labels = labels,
                    undoOffer = undoOffer,
                    undoShareLeft = undoShareLeft,
                    viewModel = viewModel,
                    onNamePressed = { lapSheetFor = it },
                    modifier = modifier,
                )
            }

            if (controlsBeside) {
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.width(WIDTH_FOR_CONTROLS - GAP).fillMaxHeight(),
                    ) {
                        startAll(Modifier.fillMaxWidth())
                        history(Modifier.fillMaxWidth())
                        resetAll(Modifier.fillMaxWidth())
                    }
                    grid(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    // Each may take a third of the row and no more, its label shrinking to fit, so
                    // that enlarged text on a narrow phone cannot push one of them off the screen.
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        startAll(Modifier.weight(1f, fill = false))
                        history(Modifier.weight(1f, fill = false))
                        resetAll(Modifier.weight(1f, fill = false))
                    }
                    grid(Modifier.weight(1f))
                }
            }
        }
    }
    lapSheetFor?.let { index ->
        LapSheet(
            label = labels[index],
            stopwatch = stopwatches[index],
            onNameChanged = { viewModel.onNameChanged(index, it) },
            onColourChosen = { viewModel.onColourChosen(index, it) },
            onDismiss = { lapSheetFor = null },
        )
    }
}

@Composable
private fun StopwatchGrid(
    columns: Int,
    stopwatches: List<Stopwatch>,
    labels: List<StopwatchLabel>,
    undoOffer: UndoOffer?,
    undoShareLeft: () -> Float,
    viewModel: TimingViewModel,
    onNamePressed: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GAP)) {
        stopwatches.indices.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(GAP),
            ) {
                row.forEach { index ->
                    // Keyed so a tile keeps its own state when the grid changes shape.
                    key(index) {
                        StopwatchTile(
                            label = labels[index],
                            onNamePressed = { onNamePressed(index) },
                            stopwatch = stopwatches[index],
                            clock = viewModel.clock,
                            onStartPressed = { viewModel.onStartPressed(index) },
                            onStopPressed = { viewModel.onStopPressed(index) },
                            onUndoPressed = viewModel::onUndoPressed,
                            undoOffered = undoOffer?.fromResetAll == false &&
                                index in undoOffer.stopwatches,
                            undoShareLeft = undoShareLeft,
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

/** Reset all, which for a short while after it is used becomes the way to take it back. */
@Composable
private fun ResetAllButton(
    undoOffered: Boolean,
    undoShareLeft: () -> Float,
    enabled: Boolean,
    onResetAllPressed: () -> Unit,
    onUndoPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenButton(
        label = if (undoOffered) R.string.action_undo else R.string.action_reset_all,
        enabled = undoOffered || enabled,
        outlined = undoOffered,
        countdown = undoShareLeft.takeIf { undoOffered },
        onClick = if (undoOffered) onUndoPressed else onResetAllPressed,
        modifier = modifier.testTag("reset-all"),
    )
}

/**
 * A control for all the stopwatches at once, in the screen's own ink, drawn [outlined] to stand
 * out. With a [countdown] it is an undo whose time is running out.
 */
@Composable
internal fun ScreenButton(
    @StringRes label: Int,
    enabled: Boolean,
    outlined: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    countdown: (() -> Float)? = null,
) {
    val ink = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else DISABLED_STRENGTH)
    CompositionLocalProvider(LocalContentColor provides ink) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(BUTTON_SHAPE)
                .then(if (outlined) Modifier.border(2.dp, ink, BUTTON_SHAPE) else Modifier)
                .then(if (countdown != null) Modifier.undoCountdown(countdown, ink) else Modifier)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp),
        ) {
            val style = MaterialTheme.typography.titleMedium
            Text(
                text = stringResource(label),
                style = style,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = style.fontSize, stepSize = 1.sp),
            )
        }
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

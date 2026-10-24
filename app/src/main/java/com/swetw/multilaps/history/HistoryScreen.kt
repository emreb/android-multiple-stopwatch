package com.swetw.multilaps.history

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import com.swetw.multilaps.R
import com.swetw.multilaps.stopwatch.formatTime
import com.swetw.multilaps.theme.FigureStyles
import com.swetw.multilaps.timing.BUTTON_SHAPE
import com.swetw.multilaps.timing.LapColumns
import com.swetw.multilaps.timing.LapRow
import com.swetw.multilaps.timing.NoteMark
import com.swetw.multilaps.timing.ScreenButton
import com.swetw.multilaps.timing.StopwatchColourKey
import com.swetw.multilaps.timing.color
import com.swetw.multilaps.timing.contentColor
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.launch

private val GAP = 8.dp

/** About five lines of a result's note, the most it shows before it scrolls. */
private val NOTE_MAX_HEIGHT = 120.dp

/**
 * What has been timed and reset, by day with the most recent first. Tapping a result opens its
 * laps in place of the list, from where that result can be shared or deleted. Timing carries on meanwhile; nothing here touches it.
 *
 * @param zone where the phone is, which decides the day and the time of day a result belongs to.
 */
@Composable
fun HistoryScreen(
    results: List<Result>,
    onResultDeleted: (id: Long) -> Unit,
    onAllResultsDeleted: () -> Unit,
    onBack: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    // Which result's laps are being looked at; kept through a rotation. One that has since been
    // deleted is no longer found, and the list shows again.
    var openResultId by rememberSaveable { mutableStateOf<Long?>(null) }
    val open = results.find { it.id == openResultId }
    // Held here, above the choice between list and result, so that coming back from a result
    // finds the list where it was left.
    val listState = rememberLazyListState()
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val title = stringResource(R.string.history_title)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("history").semantics { paneTitle = title },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(GAP),
            modifier = Modifier.safeDrawingPadding().padding(GAP),
        ) {
            if (open == null) {
                BackHandler(onBack = onBack)
                ResultList(
                    results = results,
                    listState = listState,
                    zone = zone,
                    locale = locale,
                    onResultPressed = { openResultId = it },
                    onResultDeleted = onResultDeleted,
                    onAllResultsDeleted = onAllResultsDeleted,
                    onBack = onBack,
                )
            } else {
                BackHandler { openResultId = null }
                ResultDetail(open, zone, locale, onDeleted = { onResultDeleted(open.id) }, onBack = { openResultId = null })
            }
        }
    }
}

/**
 * The results by day. A tap opens one; a swipe to the left offers to delete it; pressing and
 * holding one selects it, after which taps select and deselect, and what is selected can be shared
 * together.
 */
@Composable
private fun ResultList(
    results: List<Result>,
    listState: LazyListState,
    zone: ZoneId,
    locale: Locale,
    onResultPressed: (id: Long) -> Unit,
    onResultDeleted: (id: Long) -> Unit,
    onAllResultsDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var confirmingDeleteAll by rememberSaveable { mutableStateOf(false) }
    // The result a swipe has offered to delete, until that is answered.
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedIds by rememberSaveable(stateSaver = SelectionSaver) { mutableStateOf(emptySet<Long>()) }
    // Only what is still there counts, so a result deleted meanwhile drops out of the selection.
    val selected = results.filter { it.id in selectedIds }
    val selecting = selected.isNotEmpty()
    fun toggle(id: Long) {
        val ids = selected.map { it.id }.toSet()
        selectedIds = if (id in ids) ids - id else ids + id
    }

    if (selecting) {
        BackHandler { selectedIds = emptySet() }
        TopBar(
            title = pluralStringResource(R.plurals.selection_count, selected.size, selected.size),
            titleTag = "selection-count",
            back = R.string.action_cancel,
            backTag = "cancel-selection",
            onBack = { selectedIds = emptySet() },
            action = R.string.action_share,
            actionEnabled = true,
            onAction = {
                context.share(shareText(selected, zone, locale, context.resources, context.packageName))
                selectedIds = emptySet()
            },
            actionTag = "share-selected",
        )
    } else {
        TopBar(
            title = stringResource(R.string.history_title),
            onBack = onBack,
            action = R.string.action_delete_all,
            actionEnabled = results.isNotEmpty(),
            onAction = { confirmingDeleteAll = true },
            actionTag = "delete-all",
        )
    }
    if (results.isEmpty()) {
        Text(
            text = stringResource(R.string.no_results_yet),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 24.dp, horizontal = GAP).testTag("no-results"),
        )
    } else {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(GAP),
            modifier = Modifier.testTag("result-list"),
        ) {
            results.byDay(zone).forEach { day ->
                item(key = day.date.toEpochDay()) {
                    DayHeading(formatDate(day.date, locale))
                }
                items(day.results, key = { "result-${it.id}" }) { result ->
                    ResultRow(
                        result = result,
                        zone = zone,
                        locale = locale,
                        selecting = selecting,
                        selected = result.id in selectedIds,
                        onPressed = { if (selecting) toggle(result.id) else onResultPressed(result.id) },
                        onHeld = { toggle(result.id) },
                        onDeleteAsked = { deleting = result.id },
                    )
                }
            }
        }
    }
    if (confirmingDeleteAll) {
        ConfirmDelete(
            count = results.size,
            confirm = R.string.action_delete_all,
            onConfirmed = {
                confirmingDeleteAll = false
                onAllResultsDeleted()
            },
            onDismissed = { confirmingDeleteAll = false },
        )
    }
    deleting?.let { id ->
        ConfirmDelete(
            count = 1,
            confirm = R.string.action_delete,
            onConfirmed = {
                deleting = null
                onResultDeleted(id)
            },
            onDismissed = { deleting = null },
        )
    }
}

private val SelectionSaver = Saver<Set<Long>, LongArray>(save = { it.toLongArray() }, restore = { it.toSet() })

/** Hands [text] to whichever app the user picks from the phone's own share sheet. */
private fun Context.share(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}

/** Back at the left, what the page is in the middle, and the page's one action at the right. */
@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    @StringRes action: Int,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    actionTag: String,
    @StringRes back: Int = R.string.action_back,
    backTag: String = "history-back",
    titleTag: String = "history-title",
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ScreenButton(
            label = back,
            enabled = true,
            outlined = true,
            onClick = onBack,
            modifier = Modifier.testTag(backTag),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).testTag(titleTag).semantics { heading() },
        )
        ScreenButton(
            label = action,
            enabled = actionEnabled,
            outlined = false,
            onClick = onAction,
            modifier = Modifier.testTag(actionTag),
        )
    }
}

@Composable
private fun DayHeading(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().padding(top = GAP).testTag("day-heading").semantics { heading() },
    )
}

/**
 * One result in the list: whose it was, when, and the three figures worth comparing. Swiped to
 * the left it offers to be deleted, and springs back to wait for the answer; a screen reader,
 * which cannot swipe, is offered the same as an action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(
    result: Result,
    zone: ZoneId,
    locale: Locale,
    selecting: Boolean,
    selected: Boolean,
    onPressed: () -> Unit,
    onHeld: () -> Unit,
    onDeleteAsked: () -> Unit,
) {
    val timeOfDay = formatTimeOfDay(result.resetTime(zone), locale)
    val total = formatTime(result.totalTime)
    val best = formatTime(result.bestLap)
    val laps = pluralStringResource(R.plurals.lap_count, result.lapCount, result.lapCount)
    val spoken = stringResource(R.string.result_description, result.name, timeOfDay, total, laps, best)
    val delete = stringResource(R.string.action_delete)
    val ink = MaterialTheme.colorScheme.onBackground
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        // Called once the row has been let go far enough over, not while it is still being
        // dragged. The row goes back to wait for the answer rather than staying swiped away.
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteAsked()
            scope.launch { swipe.reset() }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !selecting,
        backgroundContent = {
            // Drawn only while the row is on its way over; at rest it would show as a dark
            // fringe around the row's rounded corners.
            if (swipe.dismissDirection == SwipeToDismissBoxValue.EndToStart) Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize().clip(BUTTON_SHAPE).background(ink).padding(horizontal = 16.dp),
            ) {
                Text(delete, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.background)
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(BUTTON_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .then(if (selected) Modifier.border(2.dp, ink, BUTTON_SHAPE) else Modifier)
                .combinedClickable(
                    onClickLabel = stringResource(if (selecting) R.string.toggle_result else R.string.show_result),
                    onLongClickLabel = stringResource(R.string.select_result),
                    role = Role.Button,
                    onLongClick = onHeld,
                    onClick = onPressed,
                )
                .testTag("result-row")
                .semantics(mergeDescendants = true) {
                    contentDescription = spoken
                    this[StopwatchColourKey] = result.colour
                    if (selecting) this.selected = selected
                    if (!selecting) {
                        customActions = listOf(
                            CustomAccessibilityAction(delete) {
                                onDeleteAsked()
                                true
                            },
                        )
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).clip(BUTTON_SHAPE).background(result.colour.color),
            ) {
                if (selected) Text("✓", color = result.colour.contentColor, style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The mark stays beside the name, however short, and a long name gives way to it.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = result.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (result.note.isNotEmpty()) NoteMark(size = 14.dp)
                    }
                    Text(total, style = FigureStyles.lap, maxLines = 1)
                }
                // One line that may run to two, so that on a narrow phone with enlarged text the
                // lesser figures wrap instead of squeezing the name out.
                Text(
                    text = stringResource(R.string.result_details, timeOfDay, laps, best),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One result's figures and every lap, newest first as a stopwatch's own lap list shows them. */
@Composable
private fun ResultDetail(result: Result, zone: ZoneId, locale: Locale, onDeleted: () -> Unit, onBack: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val reset = result.resetTime(zone)
    val context = LocalContext.current

    TopBar(
        title = result.name,
        onBack = onBack,
        action = R.string.action_delete,
        actionEnabled = true,
        onAction = { confirming = true },
        actionTag = "delete-result",
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(GAP),
        modifier = Modifier.padding(horizontal = GAP).testTag("result-detail"),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(BUTTON_SHAPE).background(result.colour.color))
            Text(
                text = stringResource(
                    R.string.date_and_time,
                    formatDate(reset.toLocalDate(), locale),
                    formatTimeOfDay(reset, locale),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // This one result and no other: sharing is by the result, never the whole history.
            ScreenButton(
                label = R.string.action_share,
                enabled = true,
                outlined = true,
                onClick = { context.share(shareText(listOf(result), zone, locale, context.resources, context.packageName)) },
                modifier = Modifier.testTag("share-result"),
            )
        }
        // A long note scrolls in a few lines of its own, rather than pushing the figures and
        // laps out of sight.
        if (result.note.isNotEmpty()) {
            Text(
                text = result.note,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .heightIn(max = NOTE_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
                    .testTag("result-note"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP), modifier = Modifier.testTag("result-figures")) {
            Figure(R.string.total_time_label, R.string.total_time_description, formatTime(result.totalTime), Modifier.weight(1f))
            Figure(R.string.best_lap_label, R.string.best_lap_description, formatTime(result.bestLap), Modifier.weight(1f))
            Figure(R.string.lap_count_label, R.string.lap_count_description, result.lapCount.toString(), Modifier.weight(1f))
        }
        val entries = result.lapEntries()
        LapColumns(
            number = stringResource(R.string.lap_column),
            time = stringResource(R.string.lap_time_column),
            totalTime = stringResource(R.string.total_time_label),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.testTag("result-laps")) {
            items(entries, key = { it.number }) { LapRow(it) }
        }
    }
    if (confirming) {
        ConfirmDelete(
            count = 1,
            confirm = R.string.action_delete,
            onConfirmed = {
                confirming = false
                onDeleted()
            },
            onDismissed = { confirming = false },
        )
    }
}

/** A figure under its caption, read out as one, as on a stopwatch's tile. */
@Composable
private fun Figure(@StringRes caption: Int, @StringRes description: Int, value: String, modifier: Modifier = Modifier) {
    val spoken = stringResource(description, value)
    Column(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Text(
            text = stringResource(caption),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Three share the width, so on a narrow phone with enlarged text a time shrinks to fit
        // rather than losing its last digits.
        Text(
            text = value,
            style = FigureStyles.lap,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = FigureStyles.lap.fontSize, stepSize = 1.sp),
        )
    }
}

/** Deleting cannot be taken back, so it is asked about first, saying how many results would go. */
@Composable
private fun ConfirmDelete(count: Int, @StringRes confirm: Int, onConfirmed: () -> Unit, onDismissed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissed,
        // The theme's own plain grey, not the tinted default, which is the one colour on the screen
        // that is neither black, white nor a stopwatch's.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(pluralStringResource(R.plurals.delete_question, count, count)) },
        text = { Text(stringResource(R.string.delete_cannot_be_undone)) },
        confirmButton = {
            TextButton(onClick = onConfirmed, modifier = Modifier.testTag("confirm-delete")) {
                Text(stringResource(confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed, modifier = Modifier.testTag("cancel-delete")) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

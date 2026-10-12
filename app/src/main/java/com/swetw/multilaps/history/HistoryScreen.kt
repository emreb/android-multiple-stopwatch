package com.swetw.multilaps.history

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swetw.multilaps.R
import com.swetw.multilaps.stopwatch.formatTime
import com.swetw.multilaps.theme.FigureStyles
import com.swetw.multilaps.timing.BUTTON_SHAPE
import com.swetw.multilaps.timing.LapColumns
import com.swetw.multilaps.timing.LapRow
import com.swetw.multilaps.timing.ScreenButton
import com.swetw.multilaps.timing.StopwatchColourKey
import com.swetw.multilaps.timing.color
import java.time.ZoneId
import java.util.Locale

private val GAP = 8.dp

/**
 * What has been timed and reset, by day with the most recent first. Tapping a result opens its
 * laps in place of the list. Timing carries on meanwhile; nothing here touches it.
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
    val locale = LocalConfiguration.current.locales[0]
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
                ResultList(results, listState, zone, locale, onResultPressed = { openResultId = it }, onAllResultsDeleted, onBack)
            } else {
                BackHandler { openResultId = null }
                ResultDetail(open, zone, locale, onDeleted = { onResultDeleted(open.id) }, onBack = { openResultId = null })
            }
        }
    }
}

@Composable
private fun ResultList(
    results: List<Result>,
    listState: LazyListState,
    zone: ZoneId,
    locale: Locale,
    onResultPressed: (id: Long) -> Unit,
    onAllResultsDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var confirming by rememberSaveable { mutableStateOf(false) }

    TopBar(
        title = stringResource(R.string.history_title),
        onBack = onBack,
        action = R.string.action_delete_all,
        actionEnabled = results.isNotEmpty(),
        onAction = { confirming = true },
        actionTag = "delete-all",
    )
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
                    DayHeading(
                        date = formatDate(day.date, locale),
                        onSharePressed = { context.share(shareText(day, zone, locale, context.resources)) },
                    )
                }
                items(day.results, key = { "result-${it.id}" }) { result ->
                    ResultRow(result, zone, locale, onPressed = { onResultPressed(result.id) })
                }
            }
        }
    }
    if (confirming) {
        ConfirmDelete(
            count = results.size,
            confirm = R.string.action_delete_all,
            onConfirmed = {
                confirming = false
                onAllResultsDeleted()
            },
            onDismissed = { confirming = false },
        )
    }
}

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
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ScreenButton(
            label = R.string.action_back,
            enabled = true,
            outlined = true,
            onClick = onBack,
            modifier = Modifier.testTag("history-back"),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
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
private fun DayHeading(date: String, onSharePressed: () -> Unit) {
    val share = stringResource(R.string.share_day_description, date)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = GAP),
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).testTag("day-heading").semantics { heading() },
        )
        ScreenButton(
            label = R.string.action_share,
            enabled = true,
            outlined = true,
            onClick = onSharePressed,
            modifier = Modifier.testTag("share-day").semantics { contentDescription = share },
        )
    }
}

/** One result in the list: whose it was, when, and the three figures worth comparing. */
@Composable
private fun ResultRow(result: Result, zone: ZoneId, locale: Locale, onPressed: () -> Unit) {
    val timeOfDay = formatTimeOfDay(result.resetTime(zone), locale)
    val total = formatTime(result.totalTime)
    val best = formatTime(result.bestLap)
    val laps = pluralStringResource(R.plurals.lap_count, result.lapCount, result.lapCount)
    val spoken = stringResource(R.string.result_description, result.name, timeOfDay, total, laps, best)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(BUTTON_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClickLabel = stringResource(R.string.show_result), role = Role.Button, onClick = onPressed)
            .testTag("result-row")
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                this[StopwatchColourKey] = result.colour
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(28.dp).clip(BUTTON_SHAPE).background(result.colour.color))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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

/** One result's figures and every lap, newest first as a stopwatch's own lap list shows them. */
@Composable
private fun ResultDetail(result: Result, zone: ZoneId, locale: Locale, onDeleted: () -> Unit, onBack: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val reset = result.resetTime(zone)

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

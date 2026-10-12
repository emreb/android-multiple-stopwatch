package com.brothersbaran.multilaps.timing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brothersbaran.multilaps.R
import com.brothersbaran.multilaps.stopwatch.LapEntry
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.formatTime
import com.brothersbaran.multilaps.stopwatch.lapEntries
import kotlinx.coroutines.launch

/** One stopwatch's completed laps, over the timing screen, which keeps running behind it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LapSheet(name: String, stopwatch: Stopwatch, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.testTag("lap-sheet").padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.stopwatch_title, name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                // Slides away like a swipe or the back button would, rather than vanishing.
                TextButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
            val entries = stopwatch.lapEntries()
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_laps_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LapList(entries)
            }
        }
    }
}

@Composable
private fun LapList(entries: List<LapEntry>) {
    Column {
        LapColumns(
            number = stringResource(R.string.lap_column),
            time = stringResource(R.string.lap_time_column),
            totalTime = stringResource(R.string.total_time_label),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        // Not keyed by lap: a keyed list holds on to the row it was showing, which would leave a
        // lap recorded meanwhile out of sight above the top.
        LazyColumn(modifier = Modifier.testTag("lap-list")) {
            items(entries) { entry ->
                val time = formatTime(entry.time)
                val totalTime = formatTime(entry.totalTimeAtCompletion)
                val spoken = stringResource(
                    if (entry.isBest) R.string.best_lap_row_description else R.string.lap_row_description,
                    entry.number,
                    time,
                    totalTime,
                )
                LapColumns(
                    number = entry.number.toString(),
                    time = time,
                    totalTime = totalTime,
                    best = entry.isBest,
                    modifier = Modifier
                        .testTag("lap-row")
                        .semantics(mergeDescendants = true) { contentDescription = spoken }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

/** One line of the lap list, its columns shared by the heading and every lap. */
@Composable
private fun LapColumns(
    number: String,
    time: String,
    totalTime: String,
    modifier: Modifier = Modifier,
    best: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(number, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = time,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = totalTime,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge,
        )
        // The place is kept either way, so the columns line up whether or not a lap is the best.
        if (best) {
            Text(
                text = stringResource(R.string.best_lap_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

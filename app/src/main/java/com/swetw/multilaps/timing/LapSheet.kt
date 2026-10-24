package com.swetw.multilaps.timing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swetw.multilaps.R
import com.swetw.multilaps.stopwatch.LapEntry
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.formatTime
import com.swetw.multilaps.stopwatch.lapEntries
import kotlinx.coroutines.launch

private const val SWATCHES_PER_ROW = 4

/**
 * One stopwatch's name, colour, note and completed laps, over the timing screen, which keeps
 * running behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LapSheet(
    label: StopwatchLabel,
    stopwatch: Stopwatch,
    onNameChanged: (String) -> Unit,
    onColourChosen: (StopwatchColour) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val entries = stopwatch.lapEntries()

    val title = stringResource(R.string.stopwatch_title, label.name)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .testTag("lap-sheet")
                .semantics { paneTitle = title }
                .padding(horizontal = 24.dp),
        ) {
            // The name and the way out stay put; everything under them scrolls.
            Row(verticalAlignment = Alignment.CenterVertically) {
                NameField(label.name, onNameChanged, modifier = Modifier.weight(1f))
                // Slides away like a swipe or the back button would, rather than vanishing.
                TextButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                ) {
                    Text(stringResource(R.string.action_close))
                }
            }
            // Not keyed by lap: a keyed list holds on to the row it was showing, which would
            // leave a lap recorded meanwhile out of sight above the top.
            LazyColumn(modifier = Modifier.testTag("lap-list")) {
                item { ColourPalette(selected = label.colour, onColourChosen = onColourChosen) }
                item { NoteField(stopwatch.note, onNoteChanged) }
                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_laps_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    stickyHeader {
                        LapColumns(
                            number = stringResource(R.string.lap_column),
                            time = stringResource(R.string.lap_time_column),
                            totalTime = stringResource(R.string.total_time_label),
                            modifier = Modifier
                                .background(BottomSheetDefaults.ContainerColor)
                                .padding(vertical = 8.dp),
                        )
                    }
                    items(entries) { LapRow(it) }
                }
            }
        }
    }
}

/**
 * Shows what is being typed, not the name in use: with the field emptied the stopwatch goes by its
 * number, but the field stays empty to type into.
 */
@Composable
private fun NameField(name: String, onNameChanged: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf(name) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = StopwatchLabel.cutToFit(it)
            onNameChanged(text)
        },
        label = { Text(stringResource(R.string.name_field_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = modifier.testTag("name-field"),
    )
}

/**
 * Shows what is being typed, as the name field does: the note kept has the space around it
 * dropped, but the field keeps it to carry on typing after.
 */
@Composable
private fun NoteField(note: String, onNoteChanged: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(note) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = Note.cutToFit(it)
            onNoteChanged(text)
        },
        label = { Text(stringResource(R.string.note_field_label)) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("note-field"),
    )
}

@Composable
private fun ColourPalette(selected: StopwatchColour, onColourChosen: (StopwatchColour) -> Unit) {
    val groupLabel = stringResource(R.string.colour_group_label)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(vertical = 16.dp)
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
    ) {
        StopwatchColour.entries.chunked(SWATCHES_PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { colour ->
                    Swatch(
                        colour = colour,
                        selected = colour == selected,
                        onChosen = { onColourChosen(colour) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    colour: StopwatchColour,
    selected: Boolean,
    onChosen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(colour.nameResource)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colour.color)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onChosen)
            .semantics { contentDescription = name }
            .testTag("colour-swatch"),
    ) {
        if (selected) {
            Text("✓", color = colour.contentColor, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun LapRow(entry: LapEntry) {
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

/** One line of the lap list, its columns shared by the heading and every lap. */
@Composable
internal fun LapColumns(
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

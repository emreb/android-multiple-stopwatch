package com.swetw.multilaps.timing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swetw.multilaps.history.HistoryStore
import com.swetw.multilaps.history.PreferencesHistoryStore
import com.swetw.multilaps.history.Result
import com.swetw.multilaps.stopwatch.Clock
import com.swetw.multilaps.stopwatch.DeviceClock
import com.swetw.multilaps.stopwatch.Stopwatch
import com.swetw.multilaps.stopwatch.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimingViewModel(
    val clock: Clock,
    private val store: TimingStore,
    private val labelStore: LabelStore,
    private val historyStore: HistoryStore,
    private val haptics: Haptics = Haptics.None,
    private val ratingAsk: RatingAsk = RatingAsk.Never,
) : ViewModel() {

    private val _stopwatches = MutableStateFlow(restored())
    val stopwatches: StateFlow<List<Stopwatch>> = _stopwatches.asStateFlow()

    private val _labels = MutableStateFlow(
        labelStore.load()?.takeIf { it.size == STOPWATCH_COUNT }
            ?.mapIndexed { index, label -> label.renamed(label.name, index) }
            ?: List(STOPWATCH_COUNT) { StopwatchLabel.default(it) },
    )
    val labels: StateFlow<List<StopwatchLabel>> = _labels.asStateFlow()

    private val _undoOffer = MutableStateFlow<UndoOffer?>(null)
    val undoOffer: StateFlow<UndoOffer?> = _undoOffer.asStateFlow()

    private val _results = MutableStateFlow(historyStore.load().sortedWith(MOST_RECENT_FIRST))

    /** What has been timed and reset, most recently reset first. */
    val results: StateFlow<List<Result>> = _results.asStateFlow()

    /** When each stopwatch was last stopped, or brought back stopped, in this run of the app. */
    private val stoppedAt = arrayOfNulls<Duration>(STOPWATCH_COUNT)
    private var offersMade = 0

    /** The results the reset on offer gave rise to, by stopwatch, so that undoing it can take them back. */
    private var resultsOnOffer = emptyMap<Int, Long>()
    private var lastUndoAt: Duration? = null

    /** The highest identifier given out in this run of the app, so one is not given out twice. */
    private var lastResultId = 0L

    /** Whether to ask for a rating now that History has been left; see [RatingAsk]. */
    fun onHistoryLeft(): Boolean = ratingAsk.tryAsk(clock.wallNow(), isAnyRunning())

    /** Whether any stopwatch is running right now. */
    fun isAnyRunning(): Boolean = _stopwatches.value.any { it.state == StopwatchState.Running }

    /** Start begins or resumes a stopwatch; pressed while running, it records a lap. */
    fun onStartPressed(index: Int) {
        val now = clock.now()
        val wallNow = clock.wallNow()
        val stopwatch = stopwatchAt(index)
        when (stopwatch.state) {
            StopwatchState.Running -> replace(mapOf(index to stopwatch.lap(now)), Feedback.Lap)
            StopwatchState.Idle, StopwatchState.Stopped -> {
                withdrawFromUndoOffer(index)
                replace(mapOf(index to stopwatch.start(now, wallNow)), Feedback.Start)
            }
        }
    }

    /**
     * Sets every idle stopwatch off at the same instant. Running and stopped ones are left as they
     * are, so a time that has been taken is never set going again by this.
     */
    fun onStartAllPressed() {
        val now = clock.now()
        val wallNow = clock.wallNow()
        val stopwatches = _stopwatches.value
        val idle = stopwatches.indices.filter { stopwatches[it].state == StopwatchState.Idle }
        idle.forEach(::withdrawFromUndoOffer)
        replace(idle.associateWith { stopwatches[it].start(now, wallNow) }, Feedback.Start)
    }

    /**
     * Stop holds a running stopwatch; pressed again while stopped, it resets. A reset sooner than
     * [MIN_RESET_INTERVAL] after the stop is taken for a bounce and ignored.
     */
    fun onStopPressed(index: Int) {
        val now = clock.now()
        val stopwatch = stopwatchAt(index)
        when (stopwatch.state) {
            StopwatchState.Running -> {
                stoppedAt[index] = now
                replace(mapOf(index to stopwatch.stop(now)), Feedback.Stop)
            }
            StopwatchState.Stopped -> if (!isBounce(since = stoppedAt[index], now)) {
                reset(listOf(index), fromResetAll = false, now)
            }
            StopwatchState.Idle -> Unit
        }
    }

    /**
     * Resets every stopped stopwatch. The same control undoes, so a press sooner than
     * [MIN_RESET_INTERVAL] after an undo is taken for a bounce and ignored.
     */
    fun onResetAllPressed() {
        val stopwatches = _stopwatches.value
        val now = clock.now()
        if (!canResetAll(stopwatches) || isBounce(since = lastUndoAt, now)) return
        val stopped = stopwatches.indices.filter { stopwatches[it].state == StopwatchState.Stopped }
        reset(stopped, fromResetAll = true, now)
    }

    /**
     * Takes back the reset on offer. A press sooner than [MIN_RESET_INTERVAL] after that reset is
     * taken for a bounce and ignored, and so is one after the offer's time has run out.
     */
    fun onUndoPressed() {
        val offer = _undoOffer.value ?: return
        val now = clock.now()
        if (isBounce(since = offer.offeredAt, now)) return
        if (undoTimeLeft(offer) == Duration.ZERO) {
            _undoOffer.value = null
            return
        }
        offer.stopwatches.keys.forEach { stoppedAt[it] = now }
        lastUndoAt = now
        val taken = offer.stopwatches.keys.mapNotNull { resultsOnOffer[it] }.toSet()
        keepResults(_results.value.filterNot { it.id in taken })
        _undoOffer.value = null
        replace(offer.stopwatches, Feedback.Undo)
    }

    /** How much longer [offer] stays open, measured from its reset whenever this is asked. */
    fun undoTimeLeft(offer: UndoOffer): Duration =
        (UNDO_WINDOW - (clock.now() - offer.offeredAt)).coerceAtLeast(Duration.ZERO)

    /** Closes the offer with this [id], unless a later reset has already replaced it. */
    fun onUndoExpired(id: Int) {
        if (_undoOffer.value?.id == id) _undoOffer.value = null
    }

    /** Names the stopwatch at [index]; with nothing typed it goes back to its number. */
    fun onNameChanged(index: Int, name: String) = relabel(index) { it.renamed(name, index) }

    fun onColourChosen(index: Int, colour: StopwatchColour) = relabel(index) { it.copy(colour = colour) }

    /** Deletes the result with this [id] for good; there is no undo for it. */
    fun onResultDeleted(id: Long) = keepResults(_results.value.filterNot { it.id == id })

    fun onAllResultsDeleted() = keepResults(emptyList())

    private fun relabel(index: Int, change: (StopwatchLabel) -> StopwatchLabel) {
        val before = _labels.value
        require(index in before.indices) { "There is no stopwatch $index" }
        val after = before.mapIndexed { i, label -> if (i == index) change(label) else label }
        if (after == before) return
        _labels.value = after
        labelStore.save(after)
    }

    private fun reset(indices: List<Int>, fromResetAll: Boolean, now: Duration) {
        val stopwatches = _stopwatches.value
        resultsOnOffer = record(indices.filter { stopwatches[it].laps.isNotEmpty() })
        _undoOffer.value = UndoOffer(
            id = ++offersMade,
            stopwatches = indices.associateWith { stopwatches[it] },
            fromResetAll = fromResetAll,
            offeredAt = now,
        )
        replace(indices.associateWith { stopwatches[it].reset() }, Feedback.Reset)
    }

    /**
     * Keeps a result for each of the stopwatches at [indices], which are about to be reset. They
     * are kept at the reset and taken back by an undo, not kept once the undo runs out: an offer
     * to undo does not outlive the app's process, and the result must.
     */
    private fun record(indices: List<Int>): Map<Int, Long> {
        if (indices.isEmpty()) return emptyMap()
        val resetAt = clock.wallNow()
        val firstId = maxOf(_results.value.maxOfOrNull { it.id } ?: 0L, lastResultId) + 1
        val recorded = indices.mapIndexed { place, index ->
            val stopwatch = _stopwatches.value[index]
            val label = _labels.value[index]
            index to Result(
                id = firstId + place,
                name = label.name,
                colour = label.colour,
                laps = stopwatch.laps,
                totalTime = stopwatch.accumulated,
                resetAt = resetAt,
                // The wall clock may have been put back since the start.
                startedAt = stopwatch.startedAt?.coerceAtMost(resetAt),
            )
        }
        lastResultId = firstId + indices.size - 1
        keepResults(_results.value + recorded.map { it.second })
        return recorded.associate { (index, result) -> index to result.id }
    }

    /** Results are only ever added or removed, so the same number of them means nothing changed. */
    private fun keepResults(results: List<Result>) {
        if (results.size == _results.value.size) return
        _results.value = results.sortedWith(MOST_RECENT_FIRST)
        historyStore.save(_results.value)
    }

    /**
     * Undoing a reset must not overwrite a stopwatch that has since set off again, so the offer
     * lets go of it, and closes once it covers nothing.
     */
    private fun withdrawFromUndoOffer(index: Int) {
        val offer = _undoOffer.value ?: return
        val remaining = offer.stopwatches - index
        _undoOffer.value = if (remaining.isEmpty()) null else offer.copy(stopwatches = remaining)
    }

    private fun isBounce(since: Duration?, now: Duration): Boolean =
        since != null && now - since < MIN_RESET_INTERVAL

    private fun stopwatchAt(index: Int): Stopwatch {
        require(index in _stopwatches.value.indices) { "There is no stopwatch $index" }
        return _stopwatches.value[index]
    }

    /**
     * Puts [replacements] in place by position, saves, and lets the press be felt as [feedback];
     * unless nothing actually changed, as when start all finds no idle stopwatch.
     */
    private fun replace(replacements: Map<Int, Stopwatch>, feedback: Feedback) {
        val before = _stopwatches.value
        val after = before.mapIndexed { index, stopwatch -> replacements[index] ?: stopwatch }
        if (after == before) return
        _stopwatches.value = after
        save(after)
        haptics.play(feedback)
    }

    private fun restored(): List<Stopwatch> {
        val saved = store.load()?.takeIf { it.stopwatches.size == STOPWATCH_COUNT }
            ?: return List(STOPWATCH_COUNT) { Stopwatch() }
        val restored = saved.restoredAt(clock)
        // Readings carried over from an earlier boot are saved as they now stand, so that
        // returning again within this boot does not carry them over a second time.
        if (restored != saved.stopwatches) save(restored)
        return restored
    }

    private fun save(stopwatches: List<Stopwatch>) = store.save(SavedTiming.of(stopwatches, clock))

    companion object {
        const val STOPWATCH_COUNT = 4
        val MIN_RESET_INTERVAL: Duration = 1.seconds

        /** Results reset at the same moment stay in the order of their stopwatches. */
        private val MOST_RECENT_FIRST = compareByDescending<Result> { it.resetAt }.thenBy { it.id }

        /** How long an offer to undo a reset stays open. */
        val UNDO_WINDOW: Duration = 5.seconds

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                TimingViewModel(
                    clock = DeviceClock(application.contentResolver),
                    store = PreferencesTimingStore(
                        application.getSharedPreferences(PreferencesTimingStore.FILE_NAME, Context.MODE_PRIVATE),
                    ),
                    labelStore = PreferencesLabelStore(
                        application.getSharedPreferences(PreferencesLabelStore.FILE_NAME, Context.MODE_PRIVATE),
                    ),
                    historyStore = PreferencesHistoryStore(
                        application.getSharedPreferences(PreferencesHistoryStore.FILE_NAME, Context.MODE_PRIVATE),
                    ),
                    haptics = VibratorHaptics.from(application),
                    ratingAsk = RatingAsk(
                        installedAt = installedAt(application),
                        store = PreferencesRatingStore(
                            application.getSharedPreferences(PreferencesRatingStore.FILE_NAME, Context.MODE_PRIVATE),
                        ),
                    ),
                )
            }
        }
    }
}

/** When the app was first installed on this phone, by the wall clock; updates leave it as it was. */
private fun installedAt(context: Context): Duration {
    val packages = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packages.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packages.getPackageInfo(context.packageName, 0)
    }
    return info.firstInstallTime.milliseconds
}

/** Whether there is any stopwatch for start all to set off. */
fun canStartAll(stopwatches: List<Stopwatch>): Boolean =
    stopwatches.any { it.state == StopwatchState.Idle }

/** Whether there is anything for reset all to do, and nothing running that it would disturb. */
fun canResetAll(stopwatches: List<Stopwatch>): Boolean =
    stopwatches.none { it.state == StopwatchState.Running } &&
        stopwatches.any { it.state == StopwatchState.Stopped }

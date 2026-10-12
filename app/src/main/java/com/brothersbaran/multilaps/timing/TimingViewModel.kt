package com.brothersbaran.multilaps.timing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.brothersbaran.multilaps.stopwatch.Clock
import com.brothersbaran.multilaps.stopwatch.DeviceClock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TimingViewModel(
    val clock: Clock,
    private val store: TimingStore,
    private val labelStore: LabelStore,
    private val haptics: Haptics = Haptics.None,
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

    /** When each stopwatch was last stopped, or brought back stopped, in this run of the app. */
    private val stoppedAt = arrayOfNulls<Duration>(STOPWATCH_COUNT)
    private var offersMade = 0
    private var lastUndoAt: Duration? = null

    /** Start begins or resumes a stopwatch; pressed while running, it records a lap. */
    fun onStartPressed(index: Int) {
        val now = clock.now()
        val stopwatch = stopwatchAt(index)
        when (stopwatch.state) {
            StopwatchState.Running -> replace(mapOf(index to stopwatch.lap(now)), Feedback.Lap)
            StopwatchState.Idle, StopwatchState.Stopped -> {
                withdrawFromUndoOffer(index)
                replace(mapOf(index to stopwatch.start(now)), Feedback.Start)
            }
        }
    }

    /**
     * Sets every idle stopwatch off at the same instant. Running and stopped ones are left as they
     * are, so a time that has been taken is never set going again by this.
     */
    fun onStartAllPressed() {
        val now = clock.now()
        val idle = _stopwatches.value.indices.filter { _stopwatches.value[it].state == StopwatchState.Idle }
        idle.forEach(::withdrawFromUndoOffer)
        replace(idle.associateWith { _stopwatches.value[it].start(now) }, Feedback.Start)
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
        _undoOffer.value = UndoOffer(
            id = ++offersMade,
            stopwatches = indices.associateWith { stopwatches[it] },
            fromResetAll = fromResetAll,
            offeredAt = now,
        )
        replace(indices.associateWith { stopwatches[it].reset() }, Feedback.Reset)
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
     * unless nothing actually changed, as when a lap press is ignored as a bounce.
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
                    haptics = VibratorHaptics.from(application),
                )
            }
        }
    }
}

/** Whether there is any stopwatch for start all to set off. */
fun canStartAll(stopwatches: List<Stopwatch>): Boolean =
    stopwatches.any { it.state == StopwatchState.Idle }

/** Whether there is anything for reset all to do, and nothing running that it would disturb. */
fun canResetAll(stopwatches: List<Stopwatch>): Boolean =
    stopwatches.none { it.state == StopwatchState.Running } &&
        stopwatches.any { it.state == StopwatchState.Stopped }

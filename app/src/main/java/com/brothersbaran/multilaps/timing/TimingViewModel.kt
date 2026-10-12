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

class TimingViewModel(val clock: Clock, private val store: TimingStore) : ViewModel() {

    private val _stopwatches = MutableStateFlow(restored())
    val stopwatches: StateFlow<List<Stopwatch>> = _stopwatches.asStateFlow()

    private val _undoOffer = MutableStateFlow<UndoOffer?>(null)
    val undoOffer: StateFlow<UndoOffer?> = _undoOffer.asStateFlow()

    /** When each stopwatch was last stopped, or brought back stopped, in this run of the app. */
    private val stoppedAt = arrayOfNulls<Duration>(STOPWATCH_COUNT)
    private var offersMade = 0

    /** Start begins or resumes a stopwatch; pressed while running, it records a lap. */
    fun onStartPressed(index: Int) {
        val now = clock.now()
        val stopwatch = stopwatchAt(index)
        when (stopwatch.state) {
            StopwatchState.Running -> replace(mapOf(index to stopwatch.lap(now)))
            StopwatchState.Idle, StopwatchState.Stopped -> {
                // Undoing a reset must not overwrite a stopwatch that has since set off again.
                if (_undoOffer.value?.stopwatches?.containsKey(index) == true) _undoOffer.value = null
                replace(mapOf(index to stopwatch.start(now)))
            }
        }
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
                replace(mapOf(index to stopwatch.stop(now)))
            }
            StopwatchState.Stopped -> if (!isBounce(since = stoppedAt[index], now)) {
                reset(listOf(index), coversAll = false, now)
            }
            StopwatchState.Idle -> Unit
        }
    }

    /** Whether there is anything for reset all to do, and nothing running that it would disturb. */
    fun canResetAll(stopwatches: List<Stopwatch>): Boolean =
        stopwatches.none { it.state == StopwatchState.Running } &&
            stopwatches.any { it.state == StopwatchState.Stopped }

    fun onResetAllPressed() {
        val stopwatches = _stopwatches.value
        if (!canResetAll(stopwatches)) return
        val stopped = stopwatches.indices.filter { stopwatches[it].state == StopwatchState.Stopped }
        reset(stopped, coversAll = true, clock.now())
    }

    /**
     * Takes back the reset on offer. A press sooner than [MIN_RESET_INTERVAL] after that reset is
     * taken for a bounce and ignored.
     */
    fun onUndoPressed() {
        val offer = _undoOffer.value ?: return
        val now = clock.now()
        if (isBounce(since = offer.offeredAt, now)) return
        offer.stopwatches.keys.forEach { stoppedAt[it] = now }
        _undoOffer.value = null
        replace(offer.stopwatches)
    }

    /** Closes the offer with this [id], unless a later reset has already replaced it. */
    fun onUndoExpired(id: Int) {
        if (_undoOffer.value?.id == id) _undoOffer.value = null
    }

    private fun reset(indices: List<Int>, coversAll: Boolean, now: Duration) {
        val stopwatches = _stopwatches.value
        _undoOffer.value = UndoOffer(
            id = ++offersMade,
            stopwatches = indices.associateWith { stopwatches[it] },
            coversAll = coversAll,
            offeredAt = now,
        )
        replace(indices.associateWith { stopwatches[it].reset() })
    }

    private fun isBounce(since: Duration?, now: Duration): Boolean =
        since != null && now - since < MIN_RESET_INTERVAL

    private fun stopwatchAt(index: Int): Stopwatch {
        require(index in _stopwatches.value.indices) { "There is no stopwatch $index" }
        return _stopwatches.value[index]
    }

    /** Puts [replacements] in place by position and saves, unless nothing actually changed. */
    private fun replace(replacements: Map<Int, Stopwatch>) {
        val before = _stopwatches.value
        val after = before.mapIndexed { index, stopwatch -> replacements[index] ?: stopwatch }
        if (after == before) return
        _stopwatches.value = after
        save(after)
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
                )
            }
        }
    }
}

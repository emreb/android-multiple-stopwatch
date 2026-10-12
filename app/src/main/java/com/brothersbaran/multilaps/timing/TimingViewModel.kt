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

class TimingViewModel(val clock: Clock, private val store: TimingStore) : ViewModel() {

    private val _stopwatches = MutableStateFlow(restored())
    val stopwatches: StateFlow<List<Stopwatch>> = _stopwatches.asStateFlow()

    /** Start begins or resumes a stopwatch; pressed while running, it records a lap. */
    fun onStartPressed(index: Int) = press(index) { stopwatch, now ->
        when (stopwatch.state) {
            StopwatchState.Running -> stopwatch.lap(now)
            StopwatchState.Idle, StopwatchState.Stopped -> stopwatch.start(now)
        }
    }

    /** Stop holds a running stopwatch; pressed again while stopped, it resets. */
    fun onStopPressed(index: Int) = press(index) { stopwatch, now ->
        when (stopwatch.state) {
            StopwatchState.Running -> stopwatch.stop(now)
            StopwatchState.Stopped -> stopwatch.reset()
            StopwatchState.Idle -> stopwatch
        }
    }

    private fun press(index: Int, action: (Stopwatch, now: Duration) -> Stopwatch) {
        require(index in _stopwatches.value.indices) { "There is no stopwatch $index" }
        val now = clock.now()
        val before = _stopwatches.value
        val after = before.mapIndexed { i, stopwatch -> if (i == index) action(stopwatch, now) else stopwatch }
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
        if (saved.bootCount != clock.bootCount) save(restored)
        return restored
    }

    private fun save(stopwatches: List<Stopwatch>) = store.save(SavedTiming.of(stopwatches, clock))

    companion object {
        const val STOPWATCH_COUNT = 4

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                TimingViewModel(
                    clock = DeviceClock(application.contentResolver),
                    store = PreferencesTimingStore(
                        application.getSharedPreferences("timing", Context.MODE_PRIVATE),
                    ),
                )
            }
        }
    }
}

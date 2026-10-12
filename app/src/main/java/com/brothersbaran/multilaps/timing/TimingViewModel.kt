package com.brothersbaran.multilaps.timing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.brothersbaran.multilaps.stopwatch.Clock
import com.brothersbaran.multilaps.stopwatch.ElapsedRealtimeClock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import com.brothersbaran.multilaps.stopwatch.StopwatchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

class TimingViewModel(val clock: Clock) : ViewModel() {

    private val _stopwatches = MutableStateFlow(List(STOPWATCH_COUNT) { Stopwatch() })
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
        _stopwatches.update { stopwatches ->
            stopwatches.mapIndexed { i, stopwatch -> if (i == index) action(stopwatch, now) else stopwatch }
        }
    }

    companion object {
        const val STOPWATCH_COUNT = 4

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TimingViewModel(ElapsedRealtimeClock) }
        }
    }
}

package com.brothersbaran.multilaps.stopwatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

class StopwatchViewModel(private val clock: Clock) : ViewModel() {

    private val _stopwatch = MutableStateFlow(Stopwatch())
    val stopwatch: StateFlow<Stopwatch> = _stopwatch.asStateFlow()

    fun onStartPressed() {
        _stopwatch.update { it.start(clock.now()) }
    }

    /** Stop holds a running stopwatch; pressed again while stopped, it resets. */
    fun onStopPressed() {
        _stopwatch.update {
            when (it.state) {
                StopwatchState.Running -> it.stop(clock.now())
                StopwatchState.Stopped -> it.reset()
                StopwatchState.Idle -> it
            }
        }
    }

    fun totalTime(): Duration = stopwatch.value.totalTime(clock.now())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { StopwatchViewModel(ElapsedRealtimeClock) }
        }
    }
}

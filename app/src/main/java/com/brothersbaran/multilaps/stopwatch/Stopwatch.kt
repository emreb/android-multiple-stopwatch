package com.brothersbaran.multilaps.stopwatch

import kotlin.time.Duration

enum class StopwatchState { Idle, Running, Stopped }

/**
 * @property accumulated total time gathered before the current run began.
 * @property startedAt the clock reading at which the current run began; set only while running.
 */
data class Stopwatch(
    val state: StopwatchState = StopwatchState.Idle,
    private val accumulated: Duration = Duration.ZERO,
    private val startedAt: Duration? = null,
) {

    fun start(now: Duration): Stopwatch = when (state) {
        StopwatchState.Running -> this
        StopwatchState.Idle, StopwatchState.Stopped ->
            copy(state = StopwatchState.Running, startedAt = now)
    }

    fun stop(now: Duration): Stopwatch = when (state) {
        StopwatchState.Running ->
            copy(state = StopwatchState.Stopped, accumulated = totalTime(now), startedAt = null)
        StopwatchState.Idle, StopwatchState.Stopped -> this
    }

    fun reset(): Stopwatch = when (state) {
        StopwatchState.Stopped -> Stopwatch()
        StopwatchState.Idle, StopwatchState.Running -> this
    }

    fun totalTime(now: Duration): Duration = accumulated + (startedAt?.let { now - it } ?: Duration.ZERO)
}

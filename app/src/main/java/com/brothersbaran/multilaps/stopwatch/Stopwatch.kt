package com.brothersbaran.multilaps.stopwatch

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class StopwatchState { Idle, Running, Stopped }

/**
 * @property laps the completed laps, oldest first.
 * @property accumulated total time gathered up to [countingFrom].
 * @property countingFrom the clock reading of the press that last started, resumed or lapped this
 *   stopwatch; set only while running.
 */
data class Stopwatch(
    val state: StopwatchState = StopwatchState.Idle,
    val laps: List<Duration> = emptyList(),
    private val accumulated: Duration = Duration.ZERO,
    private val countingFrom: Duration? = null,
) {

    init {
        require((countingFrom != null) == (state == StopwatchState.Running)) {
            "A stopwatch has a clock reading to count from exactly while it is running"
        }
    }

    private val completedLapsTime: Duration = laps.fold(Duration.ZERO, Duration::plus)

    val lapCount: Int get() = laps.size

    val lastLap: Duration? get() = laps.lastOrNull()

    val bestLap: Duration? get() = laps.minOrNull()

    fun start(now: Duration): Stopwatch = when (state) {
        StopwatchState.Running -> this
        StopwatchState.Idle, StopwatchState.Stopped ->
            copy(state = StopwatchState.Running, countingFrom = now)
    }

    /**
     * Completes the current lap. A press sooner than [MIN_LAP_INTERVAL] after the press that
     * started, resumed or last lapped the stopwatch is taken for a bounce and ignored.
     */
    fun lap(now: Duration): Stopwatch {
        if (countingFrom == null || now - countingFrom < MIN_LAP_INTERVAL) return this
        return copy(laps = laps + currentLap(now), accumulated = totalTime(now), countingFrom = now)
    }

    fun stop(now: Duration): Stopwatch = when (state) {
        StopwatchState.Running ->
            copy(state = StopwatchState.Stopped, accumulated = totalTime(now), countingFrom = null)
        StopwatchState.Idle, StopwatchState.Stopped -> this
    }

    fun reset(): Stopwatch = when (state) {
        StopwatchState.Stopped ->
            copy(state = StopwatchState.Idle, laps = emptyList(), accumulated = Duration.ZERO)
        StopwatchState.Idle, StopwatchState.Running -> this
    }

    fun totalTime(now: Duration): Duration =
        accumulated + (countingFrom?.let { now - it } ?: Duration.ZERO)

    fun currentLap(now: Duration): Duration = totalTime(now) - completedLapsTime

    companion object {
        val MIN_LAP_INTERVAL: Duration = 1.seconds
    }
}

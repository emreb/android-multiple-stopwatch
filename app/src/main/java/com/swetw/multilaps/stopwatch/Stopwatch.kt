package com.swetw.multilaps.stopwatch

import kotlin.time.Duration

enum class StopwatchState { Idle, Running, Stopped }

/**
 * @property laps the completed laps, oldest first.
 * @property accumulated total time gathered before [countingFrom], or all of it when not running.
 * @property countingFrom the clock reading of the press that last started, resumed or lapped this
 *   stopwatch; set only while running. It can be negative once carried over a restart, standing
 *   for a press made before the device booted.
 * @property startedAt the wall-clock time, since the epoch, of the first start after the last
 *   reset; never set while idle. A stopwatch carried over from before start times were kept has
 *   none until its next reset.
 */
data class Stopwatch(
    val state: StopwatchState = StopwatchState.Idle,
    val laps: List<Duration> = emptyList(),
    val accumulated: Duration = Duration.ZERO,
    val countingFrom: Duration? = null,
    val startedAt: Duration? = null,
) {

    private val completedLapsTime: Duration = laps.fold(Duration.ZERO, Duration::plus)

    init {
        require((countingFrom != null) == (state == StopwatchState.Running)) {
            "A stopwatch has a clock reading to count from exactly while it is running"
        }
        require(state != StopwatchState.Idle || accumulated == Duration.ZERO) {
            "An idle stopwatch has no time"
        }
        require(state != StopwatchState.Idle || startedAt == null) {
            "An idle stopwatch has not been started"
        }
        require(completedLapsTime <= accumulated) {
            "Completed laps cannot be longer than the total time"
        }
    }


    val lapCount: Int get() = laps.size

    val lastLap: Duration? get() = laps.lastOrNull()

    val bestLap: Duration? get() = laps.minOrNull()

    /** Starts or resumes; [wallNow] becomes the start time only when starting from idle. */
    fun start(now: Duration, wallNow: Duration): Stopwatch = when (state) {
        StopwatchState.Running -> this
        StopwatchState.Idle -> copy(state = StopwatchState.Running, countingFrom = now, startedAt = wallNow)
        StopwatchState.Stopped -> copy(state = StopwatchState.Running, countingFrom = now)
    }

    /** Completes the current lap. Every press counts, however soon after the last. */
    fun lap(now: Duration): Stopwatch {
        if (countingFrom == null) return this
        return copy(laps = laps + currentLap(now), accumulated = totalTime(now), countingFrom = now)
    }

    fun stop(now: Duration): Stopwatch = when (state) {
        StopwatchState.Running ->
            copy(state = StopwatchState.Stopped, accumulated = totalTime(now), countingFrom = null)
        StopwatchState.Idle, StopwatchState.Stopped -> this
    }

    fun reset(): Stopwatch = when (state) {
        StopwatchState.Stopped ->
            copy(state = StopwatchState.Idle, laps = emptyList(), accumulated = Duration.ZERO, startedAt = null)
        StopwatchState.Idle, StopwatchState.Running -> this
    }

    /**
     * Carries a running stopwatch onto a clock whose readings differ by [shift]. A reading that
     * would land after [now] is held to it, so the stopwatch never shows less than it had gathered.
     */
    fun rebased(shift: Duration, now: Duration): Stopwatch =
        if (countingFrom == null) this else copy(countingFrom = minOf(countingFrom + shift, now))

    fun totalTime(now: Duration): Duration =
        accumulated + (countingFrom?.let { now - it } ?: Duration.ZERO)

    fun currentLap(now: Duration): Duration = totalTime(now) - completedLapsTime
}

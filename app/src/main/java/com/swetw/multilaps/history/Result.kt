package com.swetw.multilaps.history

import com.swetw.multilaps.stopwatch.LapEntry
import com.swetw.multilaps.stopwatch.lapEntriesOf
import com.swetw.multilaps.timing.StopwatchColour
import kotlin.time.Duration

/**
 * What one stopwatch had timed when it was reset, kept to look back on. It is a record of that
 * moment: renaming or recolouring the stopwatch afterwards does not touch it.
 *
 * @property name the stopwatch's name at the reset.
 * @property laps the completed laps, oldest first; there is always at least one.
 * @property totalTime the total time at the reset, which takes in a last lap left unfinished.
 * @property resetAt the wall-clock time of the reset, since the epoch.
 * @property note what was written about the run; empty when there is none.
 * @property startedAt the wall-clock time the stopwatch was first started after its last reset,
 *   since the epoch; never after [resetAt]. Results kept before start times were recorded have none.
 * @property stopwatchIndex which of the stopwatches, counting from 0, timed it. Results kept before
 *   this was recorded do not say.
 */
data class Result(
    val id: Long,
    val name: String,
    val colour: StopwatchColour,
    val laps: List<Duration>,
    val totalTime: Duration,
    val resetAt: Duration,
    val note: String = "",
    val startedAt: Duration? = null,
    val stopwatchIndex: Int? = null,
) {

    init {
        require(laps.isNotEmpty()) { "A result has at least one lap" }
        require(laps.fold(Duration.ZERO, Duration::plus) <= totalTime) {
            "Completed laps cannot be longer than the total time"
        }
        require(startedAt == null || startedAt <= resetAt) { "A result cannot start after its reset" }
    }

    /** What places this result in History: its start, or its reset where no start time was kept. */
    val placedAt: Duration get() = startedAt ?: resetAt

    val lapCount: Int get() = laps.size

    val bestLap: Duration get() = laps.min()

    /** The laps, newest first, as the lap list of a stopwatch shows them. */
    fun lapEntries(): List<LapEntry> = lapEntriesOf(laps)
}

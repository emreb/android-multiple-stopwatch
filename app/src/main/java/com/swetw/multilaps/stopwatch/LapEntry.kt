package com.swetw.multilaps.stopwatch

import kotlin.time.Duration

/**
 * A completed lap as it appears in a list of laps.
 *
 * @property number its place in the order the laps were completed, from one.
 * @property totalTimeAtCompletion the stopwatch's total time at the press that completed it.
 */
data class LapEntry(
    val number: Int,
    val time: Duration,
    val totalTimeAtCompletion: Duration,
    val isBest: Boolean,
)

/** The completed laps, newest first. When laps tie for best, the earliest is the one marked. */
fun Stopwatch.lapEntries(): List<LapEntry> = lapEntriesOf(laps)

/** [laps], given oldest first, as entries newest first. */
fun lapEntriesOf(laps: List<Duration>): List<LapEntry> {
    val best = laps.indices.minByOrNull { laps[it] }
    var totalTime = Duration.ZERO
    return laps.mapIndexed { index, lap ->
        totalTime += lap
        LapEntry(number = index + 1, time = lap, totalTimeAtCompletion = totalTime, isBest = index == best)
    }.asReversed()
}

package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.Clock
import com.brothersbaran.multilaps.stopwatch.Stopwatch
import kotlin.time.Duration

/**
 * The stopwatches as they stood at a press, with what is needed to make sense of their clock
 * readings later.
 *
 * @property bootCount the boot the readings belong to.
 * @property bootedAt the wall-clock time of that boot, as best the device could tell.
 */
data class SavedTiming(
    val stopwatches: List<Stopwatch>,
    val bootCount: Int,
    val bootedAt: Duration,
) {

    /**
     * The stopwatches, readable against [clock] as it is now.
     *
     * Within the same boot the readings still hold, so nothing changes and no accuracy is lost. A
     * restart begins the readings again from zero, so they are carried over by way of the wall
     * clock; that is only as good as the wall clock was steady in between.
     */
    fun restoredAt(clock: Clock): List<Stopwatch> {
        val now = clock.now()
        val shift = if (clock.bootCount == bootCount) Duration.ZERO else bootedAt - bootedAt(clock)
        return stopwatches.map { it.rebased(shift, now) }
    }

    companion object {
        fun of(stopwatches: List<Stopwatch>, clock: Clock) =
            SavedTiming(stopwatches, clock.bootCount, bootedAt(clock))

        private fun bootedAt(clock: Clock): Duration = clock.wallNow() - clock.now()
    }
}

package com.swetw.multilaps.timing

import com.swetw.multilaps.stopwatch.Clock
import com.swetw.multilaps.stopwatch.Stopwatch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
        val shift = if (isFromThisBoot(clock)) Duration.ZERO else bootedAt - bootedAt(clock)
        return stopwatches.map { it.rebased(shift, now) }
    }

    /**
     * A reading from the future can only belong to an earlier boot, whatever the boot count says;
     * some devices do not report it. Where the count is not reported at all, as before Android 7,
     * the moment the device started tells boots apart: a restart moves it on by at least the time
     * the device was off, while a correction to the wall clock moves it by a second or so.
     */
    private fun isFromThisBoot(clock: Clock): Boolean {
        val now = clock.now()
        val sameBoot = if (bootCount == UNKNOWN_BOOT_COUNT && clock.bootCount == UNKNOWN_BOOT_COUNT) {
            (bootedAt - bootedAt(clock)).absoluteValue <= BOOT_MOMENT_TOLERANCE
        } else {
            clock.bootCount == bootCount
        }
        return sameBoot && stopwatches.none { stopwatch -> stopwatch.countingFrom?.let { it > now } ?: false }
    }

    companion object {
        fun of(stopwatches: List<Stopwatch>, clock: Clock) =
            SavedTiming(stopwatches, clock.bootCount, bootedAt(clock))

        private fun bootedAt(clock: Clock): Duration = clock.wallNow() - clock.now()

        private const val UNKNOWN_BOOT_COUNT = 0
        private val BOOT_MOMENT_TOLERANCE = 10.seconds
    }
}

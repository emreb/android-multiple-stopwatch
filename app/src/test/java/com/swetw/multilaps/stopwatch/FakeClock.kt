package com.swetw.multilaps.stopwatch

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/** A clock the test moves by hand, on a device the test can restart. */
class FakeClock(
    private var now: Duration = 1000.seconds,
    private var wallNow: Duration = 20_000.days,
) : Clock {

    override var bootCount: Int = 1
        private set

    override fun now(): Duration = now

    override fun wallNow(): Duration = wallNow

    fun advanceBy(duration: Duration) {
        now += duration
        wallNow += duration
    }

    /** Moves the wall clock alone, as when the user or the network corrects the time. */
    fun adjustWallClockBy(duration: Duration) {
        wallNow += duration
    }

    /**
     * Switches the device off for [downtime], then lets it run for [uptime] before the app returns.
     * [countsBoot] is false for a device that fails to report the new boot.
     */
    fun restart(downtime: Duration, uptime: Duration = 30.seconds, countsBoot: Boolean = true) {
        if (countsBoot) bootCount++
        now = uptime
        wallNow += downtime + uptime
    }
}

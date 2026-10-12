package com.brothersbaran.multilaps.stopwatch

import android.os.SystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** Time since boot, which keeps advancing while the device sleeps. */
object ElapsedRealtimeClock : Clock {
    override fun now(): Duration = SystemClock.elapsedRealtimeNanos().nanoseconds
}

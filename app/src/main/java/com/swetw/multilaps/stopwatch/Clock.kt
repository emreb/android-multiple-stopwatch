package com.swetw.multilaps.stopwatch

import kotlin.time.Duration

/** The device's sense of time. */
interface Clock {

    /** A monotonic reading; only differences between two readings from the same boot mean anything. */
    fun now(): Duration

    /** Wall-clock time, which outlives a restart but can jump when the time is corrected. */
    fun wallNow(): Duration

    /** How many times the device has booted; a change means [now] readings started over. */
    val bootCount: Int
}

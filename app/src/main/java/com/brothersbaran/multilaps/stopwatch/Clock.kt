package com.brothersbaran.multilaps.stopwatch

import kotlin.time.Duration

/** A monotonic time source; only differences between two readings are meaningful. */
fun interface Clock {
    fun now(): Duration
}

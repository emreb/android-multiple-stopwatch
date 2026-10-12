package com.brothersbaran.multilaps.stopwatch

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FakeClock(private var now: Duration = 1000.seconds) : Clock {

    override fun now(): Duration = now

    fun advanceBy(duration: Duration) {
        now += duration
    }
}

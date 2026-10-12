package com.brothersbaran.multilaps.stopwatch

import kotlin.time.Duration

data class Stopwatch(private val startedAt: Duration? = null) {

    val isRunning: Boolean get() = startedAt != null

    fun start(now: Duration): Stopwatch = if (isRunning) this else copy(startedAt = now)

    fun totalTime(now: Duration): Duration = startedAt?.let { now - it } ?: Duration.ZERO
}

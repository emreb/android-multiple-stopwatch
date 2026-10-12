package com.brothersbaran.multilaps.timing

import com.brothersbaran.multilaps.stopwatch.Stopwatch
import kotlin.time.Duration

/**
 * The chance, open for a short while, to take back a reset.
 *
 * @property stopwatches what each reset stopwatch was, by its position.
 * @property fromResetAll whether this came from reset all, however many stopwatches that reset.
 * @property offeredAt the clock reading at the reset.
 */
data class UndoOffer(
    val id: Int,
    val stopwatches: Map<Int, Stopwatch>,
    val fromResetAll: Boolean,
    val offeredAt: Duration,
)

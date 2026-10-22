package com.swetw.multilaps.timing

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** Whether this install has asked Google Play for a rating yet, kept across runs of the app. */
interface RatingStore {

    fun hasAsked(): Boolean

    fun recordAsked()
}

/**
 * When to ask for a rating on Google Play: on leaving History with no stopwatch running, once
 * [WAIT] has passed since the app was installed at [installedAt] (wall-clock time since the epoch),
 * and only once per install, unless the app's data is cleared. Once it has said yes it never does
 * again, whether or not Google then shows its prompt.
 */
class RatingAsk(private val installedAt: Duration, private val store: RatingStore) {

    /** Whether to ask now, at [wallNow]; a yes is remembered, so it is the only one. */
    fun tryAsk(wallNow: Duration, anyRunning: Boolean): Boolean {
        if (anyRunning || wallNow - installedAt < WAIT || store.hasAsked()) return false
        store.recordAsked()
        return true
    }

    companion object {
        val WAIT: Duration = 10.days

        /** Never asks, as for an app that has asked already. */
        val Never = RatingAsk(
            Duration.ZERO,
            object : RatingStore {
                override fun hasAsked() = true

                override fun recordAsked() = Unit
            },
        )
    }
}

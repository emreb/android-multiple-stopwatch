package com.swetw.multilaps.timing

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks Google Play to show its rating card over [activity]. Play decides whether it actually
 * appears, and says nothing either way; where Play is missing, as on a phone without it, the
 * request fails and nothing happens. [manager] is Play's own unless a test stands in for it.
 */
class PlayRatingPrompt(
    private val activity: ComponentActivity,
    private val manager: ReviewManager = ReviewManagerFactory.create(activity),
) : RatingPrompt {

    override fun show(stillWanted: () -> Boolean) {
        manager.requestReviewFlow().addOnCompleteListener { request ->
            // Play answers later. By then a stopwatch may have been started, or the screen turned,
            // closed or sent to the background; the card goes up only over the screen still in use.
            val inFront = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (request.isSuccessful && inFront && stillWanted()) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}

package com.swetw.multilaps.timing

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks Google Play to show its rating card over [activity]. Play decides whether it actually
 * appears, and says nothing either way; where Play is missing, as on a phone without it, the
 * request fails and nothing happens. [manager] is Play's own unless a test stands in for it.
 */
class PlayRatingPrompt(
    private val activity: Activity,
    private val manager: ReviewManager = ReviewManagerFactory.create(activity),
) : RatingPrompt {

    override fun show() {
        manager.requestReviewFlow().addOnCompleteListener { request ->
            // Play answers later; by then the screen may have turned or closed.
            if (request.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }
}

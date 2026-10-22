package com.swetw.multilaps.timing

import android.app.Activity
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PlayRatingPromptTest {

    private val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    private val activity = controller.get()

    /** Google's stand-in for Play, counting the times the rating card is put up. */
    private class CountingReviewManager(activity: Activity) : ReviewManager {
        private val play = FakeReviewManager(activity)
        var launches = 0

        override fun requestReviewFlow(): Task<ReviewInfo> = play.requestReviewFlow()

        override fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Task<Void> {
            launches++
            return play.launchReviewFlow(activity, reviewInfo)
        }
    }

    private fun answers() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `when Play answers, its rating card is put up once`() {
        val play = CountingReviewManager(activity)

        PlayRatingPrompt(activity, play).show(stillWanted = { true })
        answers()

        assertEquals(1, play.launches)
    }

    @Test
    fun `nothing is put up over a screen that has gone by the time Play answers`() {
        val play = CountingReviewManager(activity)

        PlayRatingPrompt(activity, play).show(stillWanted = { true })
        controller.pause().stop().destroy()
        answers()

        assertEquals(0, play.launches)
    }

    @Test
    fun `nothing is put up if, by the time Play answers, it is no longer wanted`() {
        val play = CountingReviewManager(activity)
        var wanted = true

        PlayRatingPrompt(activity, play).show(stillWanted = { wanted })
        wanted = false
        answers()

        assertEquals(0, play.launches)
    }

    @Test
    fun `nothing is put up over a screen sent to the background before Play answers`() {
        val play = CountingReviewManager(activity)

        PlayRatingPrompt(activity, play).show(stillWanted = { true })
        controller.pause().stop()
        answers()

        assertEquals(0, play.launches)
    }

    @Test
    fun `without Play on the phone, asking does nothing and breaks nothing`() {
        PlayRatingPrompt(activity).show(stillWanted = { true })
        answers()

        assertEquals(null, shadowOf(activity).nextStartedActivity)
    }
}

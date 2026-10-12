package com.brothersbaran.multilaps.timing

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationAttributes
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class VibratorHapticsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val shadow = shadowOf(vibrator)
    private val haptics = VibratorHaptics(vibrator)

    /** What one kind of feedback makes the device do: on and off spans, in milliseconds. */
    private fun timings(feedback: Feedback): List<Long> {
        haptics.play(feedback)
        return shadow.pattern?.toList() ?: listOf(shadow.milliseconds)
    }

    @Test
    fun `every kind of feedback vibrates`() {
        for (feedback in Feedback.entries) {
            vibrator.cancel()
            haptics.play(feedback)

            assertTrue("$feedback", shadow.isVibrating)
        }
    }

    @Test
    fun `no pulse is too brief for a slow motor to get going`() {
        for (feedback in Feedback.entries) {
            val pulses = timings(feedback).let { if (it.size == 1) it else it.filterIndexed { i, _ -> i % 2 == 1 } }
            assertTrue("$feedback pulses $pulses", pulses.all { it >= 50 })
        }
    }

    @Test
    fun `every vibration is short`() {
        for (feedback in Feedback.entries) {
            assertTrue("$feedback", timings(feedback).sum() <= 250)
        }
    }

    @Test
    fun `a lap is one pulse and a stop is two, so they are told apart without looking`() {
        val lap = timings(Feedback.Lap)
        val stop = timings(Feedback.Stop)

        assertEquals(1, lap.size)
        assertEquals("a wait, a pulse, a gap, a pulse", 4, stop.size)
        assertNotEquals(lap, stop)
    }

    @Test
    fun `on a device that cannot vibrate nothing happens and nothing breaks`() {
        shadow.setHasVibrator(false)

        for (feedback in Feedback.entries) haptics.play(feedback)

        assertFalse(shadow.isVibrating)
    }

    @Test
    fun `with no vibrator service at all nothing breaks`() {
        val haptics = VibratorHaptics(null)

        for (feedback in Feedback.entries) haptics.play(feedback)
    }

    @Test
    fun `no vibration is left to be taken for touch feedback, which many phones have switched off`() {
        for (feedback in Feedback.entries) {
            haptics.play(feedback)

            assertEquals(
                "$feedback",
                VibrationAttributes.USAGE_MEDIA,
                (shadow.vibrationAttributesFromLastVibration as VibrationAttributes?)?.usage,
            )
        }
    }

    @Test
    @Config(sdk = [30])
    fun `on Android before 13 the same is said the older way`() {
        for (feedback in Feedback.entries) {
            haptics.play(feedback)

            assertEquals("$feedback", AudioAttributes.USAGE_MEDIA, shadow.audioAttributesFromLastVibration?.usage)
        }
    }

    @Test
    @Config(sdk = [26])
    fun `it vibrates on the oldest Android the app supports`() {
        haptics.play(Feedback.Stop)

        assertTrue(shadow.isVibrating)
    }

    @Test
    @Config(sdk = [26])
    fun `the haptics the app is given reach the device's vibrator on older Android`() {
        VibratorHaptics.from(context).play(Feedback.Lap)

        assertTrue(shadow.isVibrating)
    }

    @Test
    fun `the haptics the app is given reach the device's vibrator on current Android`() {
        VibratorHaptics.from(context).play(Feedback.Lap)

        assertTrue(shadow.isVibrating)
    }
}

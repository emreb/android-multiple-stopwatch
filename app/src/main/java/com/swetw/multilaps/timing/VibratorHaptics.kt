package com.swetw.multilaps.timing

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays feedback as short vibrations whose rhythm tells the presses apart: one pulse for a lap or
 * a start, two for a stop, one long one for a reset and three for an undo. No pulse is shorter
 * than 50 ms, which is about what an older motor needs to get going. They are played as media
 * vibration, so they follow the phone's vibration setting but not its touch-feedback one. Without
 * a vibrator it does nothing.
 */
class VibratorHaptics(private val vibrator: Vibrator?) : Haptics {

    override fun play(feedback: Feedback) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = effect(feedback)
        // Said outright not to be touch feedback. Left unsaid, Android takes a single short pulse
        // for touch feedback and drops it on any phone with that switched off, which silenced
        // start, lap and reset while the two-pulse stop still played.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
        }
    }

    private fun effect(feedback: Feedback): VibrationEffect = when (feedback) {
        Feedback.Lap -> pulse(50)
        Feedback.Start -> pulse(80)
        Feedback.Stop -> pulses(60, 90, 60)
        Feedback.Reset -> pulse(200)
        Feedback.Undo -> pulses(50, 50, 50, 50, 50)
    }

    private fun pulse(milliseconds: Long): VibrationEffect =
        VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)

    /** Pulses and the gaps between them, in turn, starting at once. */
    private fun pulses(vararg onAndOff: Long): VibrationEffect =
        VibrationEffect.createWaveform(longArrayOf(0, *onAndOff), NO_REPEAT)

    companion object {
        private const val NO_REPEAT = -1

        fun from(context: Context): VibratorHaptics {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
            return VibratorHaptics(vibrator)
        }
    }
}

package com.brothersbaran.multilaps.timing

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
 * than 50 ms, which is about what an older motor needs to get going. They are played as an
 * alarm's vibration: from Android 11 that follows the phone's main vibration switch and its
 * alarm-vibration level, but not its touch-feedback setting, and it is still played in Battery
 * Saver. Without a vibrator it does nothing.
 */
class VibratorHaptics(private val vibrator: Vibrator?) : Haptics {

    override fun play(feedback: Feedback) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = effect(feedback)
        // Played as an alarm's vibration. Left with no usage, Android takes a short vibration for
        // touch feedback and drops it on any phone with that switched off, which silenced start,
        // lap and reset. Of the usages that are not touch feedback, an alarm's is the one that is
        // also still played in Battery Saver, which matters on a long day at the track.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            // Android 11 and 12 work the usage out from audio attributes, and an alarm's is one
            // they keep. Android 9 and 10 take any short vibration that is not a notification's
            // for touch feedback whatever is said here; there, touch feedback being off silences
            // these, and nothing an app can honestly say prevents it.
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
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

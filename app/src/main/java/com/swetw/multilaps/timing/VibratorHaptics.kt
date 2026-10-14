package com.swetw.multilaps.timing

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

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
        val timings = timings(feedback)
        // Played as an alarm's vibration. Left with no usage, Android takes a short vibration for
        // touch feedback and drops it on any phone with that switched off, which silenced start,
        // lap and reset. Of the usages that are not touch feedback, an alarm's is the one that is
        // also still played in Battery Saver, which matters on a long day at the track.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect(timings), VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 11 and 12 work the usage out from audio attributes, and an alarm's is one
            // they keep. Android 9 and 10 take any short vibration that is not a notification's
            // for touch feedback whatever is said here; there, touch feedback being off silences
            // these, and nothing an app can honestly say prevents it.
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect(timings), ALARM)
        } else {
            // Android 6 and 7 have no vibration effects, only patterns.
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, NO_REPEAT, ALARM)
        }
    }

    /** Pulses and the gaps between them, in turn, after a first wait of nothing. */
    private fun timings(feedback: Feedback): LongArray = when (feedback) {
        Feedback.Lap -> pulses(50)
        Feedback.Start -> pulses(80)
        Feedback.Stop -> pulses(60, 90, 60)
        Feedback.Reset -> pulses(200)
        Feedback.Undo -> pulses(50, 50, 50, 50, 50)
    }

    private fun pulses(vararg onAndOff: Long): LongArray = longArrayOf(0, *onAndOff)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun effect(timings: LongArray): VibrationEffect =
        if (timings.size == 2) {
            VibrationEffect.createOneShot(timings[1], VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createWaveform(timings, NO_REPEAT)
        }

    companion object {
        private const val NO_REPEAT = -1
        private val ALARM: AudioAttributes =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()

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

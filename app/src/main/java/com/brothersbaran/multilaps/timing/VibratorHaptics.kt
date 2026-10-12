package com.brothersbaran.multilaps.timing

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays feedback as short vibrations whose rhythm tells the presses apart: one pulse for a lap,
 * two for a stop. Without a vibrator it does nothing.
 */
class VibratorHaptics(private val vibrator: Vibrator?) : Haptics {

    override fun play(feedback: Feedback) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrate(effect(feedback))
    }

    private fun effect(feedback: Feedback): VibrationEffect = when (feedback) {
        Feedback.Lap -> pulse(30)
        Feedback.Start -> pulse(60)
        Feedback.Stop -> pulses(50, 70, 50)
        Feedback.Reset -> pulse(150)
        Feedback.Undo -> pulses(30, 50, 30, 50, 30)
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

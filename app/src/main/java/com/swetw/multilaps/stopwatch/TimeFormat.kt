package com.swetw.multilaps.stopwatch

import java.util.Locale
import kotlin.time.Duration

/**
 * Formats as `m:ss.hh`, or `h:mm:ss.hh` from an hour up, truncating to whole hundredths so a time
 * never reads ahead of itself.
 */
fun formatTime(time: Duration): String {
    val totalHundredths = time.inWholeMilliseconds / 10
    val hundredths = totalHundredths % 100
    val seconds = totalHundredths / 100 % 60
    val minutes = totalHundredths / 6000 % 60
    val hours = totalHundredths / 360_000
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
        String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, hundredths)
    }
}

package com.brothersbaran.multilaps.stopwatch

import java.util.Locale
import kotlin.time.Duration

/** Formats as `m:ss.hh`, truncating to whole hundredths so a time never reads ahead of itself. */
fun formatTime(time: Duration): String {
    val hundredths = time.inWholeMilliseconds / 10
    val minutes = hundredths / 6000
    val seconds = hundredths / 100 % 60
    return String.format(Locale.ROOT, "%d:%02d.%02d", minutes, seconds, hundredths % 100)
}

package com.swetw.multilaps.history

import android.content.res.Resources
import com.swetw.multilaps.R
import com.swetw.multilaps.stopwatch.formatTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A date written out in full, such as "Monday, 12 October 2026" in British English. */
fun formatDate(date: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)

/** A time of day to the minute, such as "16:42". */
fun formatTimeOfDay(time: ZonedDateTime, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)

/**
 * A day's results as plain text to hand to another app: the date, then each result with its
 * figures and every lap, in the order the laps were run.
 */
fun shareText(day: Day, zone: ZoneId, locale: Locale, resources: Resources): String {
    val title = resources.getString(R.string.share_title, formatDate(day.date, locale))
    val results = day.results.map { result ->
        val heading = resources.getString(
            R.string.share_result_heading,
            result.name,
            formatTimeOfDay(result.resetTime(zone), locale),
        )
        val summary = resources.getString(
            R.string.share_result_summary,
            formatTime(result.totalTime),
            resources.getQuantityString(R.plurals.lap_count, result.lapCount, result.lapCount),
            formatTime(result.bestLap),
        )
        val laps = result.laps.mapIndexed { index, lap ->
            resources.getString(R.string.share_lap, index + 1, formatTime(lap))
        }
        (listOf(heading, summary) + laps).joinToString("\n")
    }
    return (listOf(title) + results).joinToString("\n\n")
}

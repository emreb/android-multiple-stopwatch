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

private const val PLAY_STORE_PAGE = "https://play.google.com/store/apps/details?id="

/** A date written out in full, such as "Monday, 12 October 2026" in British English. */
fun formatDate(date: LocalDate, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)

/** A time of day to the minute, as [locale] writes it: "16:42" in British English, "4:42 PM" in American. */
fun formatTimeOfDay(time: ZonedDateTime, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)

/**
 * Results as plain text to hand to another app. Each day's come under its date: whose each was
 * and when, its figures, and every lap in the order the laps were run. Days come in the order they are first met.
 * The text ends with a link to the app on Google Play, found by its [applicationId].
 */
fun shareText(results: List<Result>, zone: ZoneId, locale: Locale, resources: Resources, applicationId: String): String {
    val days = results.byDay(zone).map { day ->
        val title = resources.getString(R.string.share_title, formatDate(day.date, locale))
        (listOf(title) + day.results.map { shareText(it, zone, locale, resources) }).joinToString("\n\n")
    }
    val footer = resources.getString(R.string.share_footer, "$PLAY_STORE_PAGE$applicationId")
    return (days + footer).joinToString("\n\n")
}

private fun shareText(result: Result, zone: ZoneId, locale: Locale, resources: Resources): String {
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
    return (listOf(heading, summary) + laps).joinToString("\n")
}

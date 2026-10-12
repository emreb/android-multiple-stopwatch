package com.swetw.multilaps.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** The results reset on one calendar day. */
data class Day(val date: LocalDate, val results: List<Result>)

/** When this result's stopwatch was reset, as the clocks in [zone] read. */
fun Result.resetTime(zone: ZoneId): ZonedDateTime =
    Instant.ofEpochSecond(resetAt.inWholeSeconds).atZone(zone)

/**
 * These results under the days they were reset on in [zone]. Days come in the order they are
 * first met, and results keep their order within a day, so most recent first stays so.
 */
fun List<Result>.byDay(zone: ZoneId): List<Day> =
    groupBy { it.resetTime(zone).toLocalDate() }.map { (date, results) -> Day(date, results) }

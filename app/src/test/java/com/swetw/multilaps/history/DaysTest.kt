package com.swetw.multilaps.history

import com.swetw.multilaps.timing.StopwatchColour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

class DaysTest {

    private val lisbon = ZoneId.of("Europe/Lisbon")

    @Test
    fun `no results make no days`() {
        assertTrue(emptyList<Result>().byDay(lisbon).isEmpty())
    }

    @Test
    fun `results are gathered under the day they were reset, keeping their order`() {
        val results = listOf(
            result(1, "2026-10-12T18:30", lisbon),
            result(2, "2026-10-12T09:00", lisbon),
            result(3, "2026-10-10T17:45", lisbon),
        )

        assertEquals(
            listOf(
                Day(LocalDate.of(2026, 10, 12), results.take(2)),
                Day(LocalDate.of(2026, 10, 10), results.drop(2)),
            ),
            results.byDay(lisbon),
        )
    }

    @Test
    fun `either side of midnight is two days`() {
        val results = listOf(
            result(1, "2026-10-13T00:00", lisbon),
            result(2, "2026-10-12T23:59", lisbon),
        )

        assertEquals(listOf(13, 12), results.byDay(lisbon).map { it.date.dayOfMonth })
    }

    @Test
    fun `the day is the one it was where the phone is, not in Greenwich`() {
        val auckland = ZoneId.of("Pacific/Auckland")
        val results = listOf(result(1, "2026-10-12T20:00", ZoneOffset.UTC))

        assertEquals(LocalDate.of(2026, 10, 13), results.byDay(auckland).single().date)
        assertEquals(LocalDate.of(2026, 10, 12), results.byDay(lisbon).single().date)
    }

    @Test
    fun `a result knows the time of day it was reset`() {
        val result = result(1, "2026-10-12T16:42", lisbon)

        assertEquals(LocalDateTime.parse("2026-10-12T16:42"), result.resetTime(lisbon).toLocalDateTime())
    }

    @Test
    fun `a result knows the time of day it started, where it has one`() {
        val result = result(1, "2026-10-12T16:42", lisbon, startedAt = "2026-10-12T16:02")

        assertEquals(LocalDateTime.parse("2026-10-12T16:02"), result.startTime(lisbon)?.toLocalDateTime())
        assertNull(result(2, "2026-10-12T16:42", lisbon).startTime(lisbon))
    }
}

/** A result with one lap, reset at [localTime] in [zone] and, if given, started at [startedAt] there. */
fun result(
    id: Long,
    localTime: String,
    zone: ZoneId,
    name: String = id.toString(),
    colour: StopwatchColour = StopwatchColour.Blue,
    laps: List<Int> = listOf(60),
    extra: Int = 0,
    startedAt: String? = null,
) = Result(
    id = id,
    name = name,
    colour = colour,
    laps = laps.map { it.seconds },
    totalTime = (laps.sum() + extra).seconds,
    resetAt = LocalDateTime.parse(localTime).atZone(zone).toEpochSecond().seconds,
    startedAt = startedAt?.let { LocalDateTime.parse(it).atZone(zone).toEpochSecond().seconds },
)

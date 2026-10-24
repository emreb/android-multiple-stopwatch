package com.swetw.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class LapEntryTest {

    private val start = 100.seconds
    private val running = Stopwatch().start(now = start, wallNow = start)

    @Test
    fun `a stopwatch without completed laps has no entries, even while its first lap is in progress`() {
        assertTrue(Stopwatch().lapEntries().isEmpty())
        assertTrue(running.lapEntries().isEmpty())
    }

    @Test
    fun `entries are newest first, numbered from one in the order the laps were completed`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .lap(now = start + 95.seconds)

        assertEquals(listOf(3, 2, 1), stopwatch.lapEntries().map { it.number })
        assertEquals(listOf(40.seconds, 25.seconds, 30.seconds), stopwatch.lapEntries().map { it.time })
    }

    @Test
    fun `each entry carries the total time at which its lap was completed`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .lap(now = start + 95.seconds)

        assertEquals(
            listOf(95.seconds, 55.seconds, 30.seconds),
            stopwatch.lapEntries().map { it.totalTimeAtCompletion },
        )
    }

    @Test
    fun `time spent stopped is in neither a lap nor the total time at its completion`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .stop(now = start + 40.seconds)
            .start(now = start + 500.seconds, wallNow = start + 500.seconds)
            .lap(now = start + 505.seconds)

        val newest = stopwatch.lapEntries().first()
        assertEquals(15.seconds, newest.time)
        assertEquals(45.seconds, newest.totalTimeAtCompletion)
    }

    @Test
    fun `only the best lap is marked best`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 55.seconds)
            .lap(now = start + 95.seconds)

        assertEquals(listOf(false, true, false), stopwatch.lapEntries().map { it.isBest })
    }

    @Test
    fun `when laps tie for best the earliest is the one marked`() {
        val stopwatch = running
            .lap(now = start + 30.seconds)
            .lap(now = start + 50.seconds)
            .lap(now = start + 70.seconds)

        assertEquals(listOf(3, 2, 1), stopwatch.lapEntries().map { it.number })
        assertEquals(listOf(false, true, false), stopwatch.lapEntries().map { it.isBest })
    }

    @Test
    fun `a single lap is the best lap`() {
        assertEquals(listOf(true), running.lap(now = start + 30.seconds).lapEntries().map { it.isBest })
    }
}

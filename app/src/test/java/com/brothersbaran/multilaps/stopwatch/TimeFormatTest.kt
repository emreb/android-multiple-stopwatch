package com.brothersbaran.multilaps.stopwatch

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeFormatTest {

    @Test
    fun `zero is shown with minutes, seconds and hundredths`() {
        assertEquals("0:00.00", formatTime(Duration.ZERO))
    }

    @Test
    fun `minutes, seconds and hundredths are shown`() {
        assertEquals("1:23.45", formatTime(1.minutes + 23.seconds + 450.milliseconds))
    }

    @Test
    fun `seconds and hundredths are zero padded`() {
        assertEquals("0:05.07", formatTime(5.seconds + 70.milliseconds))
    }

    @Test
    fun `hundredths are truncated, never rounded up`() {
        assertEquals("0:01.23", formatTime(1239.milliseconds))
        assertEquals("0:59.99", formatTime(59_999.milliseconds))
    }

    @Test
    fun `minutes are not padded below an hour`() {
        assertEquals("12:00.00", formatTime(12.minutes))
        assertEquals("59:59.99", formatTime(1.hours - 1.milliseconds))
    }

    @Test
    fun `an hour or more adds hours and pads minutes`() {
        assertEquals("1:00:00.00", formatTime(1.hours))
        assertEquals("1:02:03.45", formatTime(1.hours + 2.minutes + 3.seconds + 450.milliseconds))
        assertEquals("12:00:00.00", formatTime(12.hours))
    }
}

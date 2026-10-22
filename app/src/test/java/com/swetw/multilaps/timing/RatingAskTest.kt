package com.swetw.multilaps.timing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class RatingAskTest {

    private val installedAt = 20_000.days
    private val store = FakeRatingStore()
    private val ask = RatingAsk(installedAt, store)

    @Test
    fun `ten days after installing, with nothing running, it asks`() {
        assertTrue(ask.tryAsk(wallNow = installedAt + 10.days, anyRunning = false))
    }

    @Test
    fun `it does not ask sooner than ten days after installing`() {
        assertFalse(ask.tryAsk(wallNow = installedAt + 10.days - 1.seconds, anyRunning = false))
        assertTrue(ask.tryAsk(wallNow = installedAt + 10.days, anyRunning = false))
    }

    @Test
    fun `it does not ask while a stopwatch is running, and asks once none is`() {
        assertFalse(ask.tryAsk(wallNow = installedAt + 30.days, anyRunning = true))
        assertTrue(ask.tryAsk(wallNow = installedAt + 30.days, anyRunning = false))
    }

    @Test
    fun `it asks once and never again`() {
        assertTrue(ask.tryAsk(wallNow = installedAt + 10.days, anyRunning = false))

        assertFalse(ask.tryAsk(wallNow = installedAt + 11.days, anyRunning = false))
        assertTrue(store.asked)
    }

    @Test
    fun `an install that has asked before, in an earlier run of the app, does not ask again`() {
        val afterRestart = RatingAsk(installedAt, FakeRatingStore(asked = true))

        assertFalse(afterRestart.tryAsk(wallNow = installedAt + 100.days, anyRunning = false))
    }

    @Test
    fun `a refusal leaves the one ask still to come`() {
        ask.tryAsk(wallNow = installedAt + 1.days, anyRunning = false)
        ask.tryAsk(wallNow = installedAt + 20.days, anyRunning = true)

        assertFalse(store.asked)
    }

    @Test
    fun `never is never due`() {
        assertFalse(RatingAsk.Never.tryAsk(wallNow = 100_000.days, anyRunning = false))
    }
}

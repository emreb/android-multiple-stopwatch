package com.swetw.multilaps.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ShareTextTest {

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val lisbon = ZoneId.of("Europe/Lisbon")

    @Test
    fun `a result is shared as its date, its name and time of day, its figures and every lap in order`() {
        val ada = result(1, "2026-10-12T16:42", lisbon, name = "Ada", laps = listOf(83, 82, 85), extra = 34)

        assertEquals(
            """
            MultiLaps, Monday, 12 October 2026

            Ada, 16:42
            Total 4:44.00, 3 laps, best 1:22.00
            Lap 1: 1:23.00
            Lap 2: 1:22.00
            Lap 3: 1:25.00
            """.trimIndent(),
            shareText(listOf(ada), lisbon, Locale.UK, resources),
        )
    }

    @Test
    fun `several results are shared together, each day's under its date`() {
        val ada = result(1, "2026-10-12T16:42", lisbon, name = "Ada", laps = listOf(83))
        val leo = result(2, "2026-10-12T16:40", lisbon, name = "Leo", laps = listOf(90))
        val mia = result(3, "2026-10-10T09:05", lisbon, name = "Mia", laps = listOf(75))

        assertEquals(
            """
            MultiLaps, Monday, 12 October 2026

            Ada, 16:42
            Total 1:23.00, 1 lap, best 1:23.00
            Lap 1: 1:23.00

            Leo, 16:40
            Total 1:30.00, 1 lap, best 1:30.00
            Lap 1: 1:30.00

            MultiLaps, Saturday, 10 October 2026

            Mia, 09:05
            Total 1:15.00, 1 lap, best 1:15.00
            Lap 1: 1:15.00
            """.trimIndent(),
            shareText(listOf(ada, leo, mia), lisbon, Locale.UK, resources),
        )
    }

    @Test
    fun `a single lap and times of an hour or more are written as the app shows them`() {
        val leo = result(2, "2026-10-12T16:40", lisbon, name = "Leo", laps = listOf(3725))

        assertEquals(
            listOf("Leo, 16:40", "Total 1:02:05.00, 1 lap, best 1:02:05.00", "Lap 1: 1:02:05.00"),
            shareText(listOf(leo), lisbon, Locale.UK, resources).lines().drop(2),
        )
    }

    @Test
    fun `dates and times of day are written the way the phone's language writes them`() {
        val american = shareText(listOf(result(1, "2026-10-12T16:42", lisbon)), lisbon, Locale.US, resources)

        assertEquals("MultiLaps, Monday, October 12, 2026", american.lines().first())
        assertEquals("1, 4:42 PM", american.lines()[2].replace('\u202f', ' '))
    }
}

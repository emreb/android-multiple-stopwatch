package com.brothersbaran.multilaps.timing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesLabelStoreTest {

    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("labels-test", Context.MODE_PRIVATE)
    private val store = PreferencesLabelStore(preferences)

    private val labels = listOf(
        StopwatchLabel("Ada", StopwatchColour.Purple),
        StopwatchLabel("Zoë \"Zed\" O'Neil, 🏃", StopwatchColour.Yellow),
        StopwatchLabel("3", StopwatchColour.Green),
    )

    @Test
    fun `nothing is loaded before anything is saved`() {
        assertNull(store.load())
    }

    @Test
    fun `names and colours come back exactly, awkward characters included`() {
        store.save(labels)

        assertEquals(labels, PreferencesLabelStore(preferences).load())
    }

    @Test
    fun `a later save replaces an earlier one`() {
        store.save(labels)
        store.save(labels.reversed())

        assertEquals(labels.reversed(), store.load())
    }

    @Test
    fun `unreadable data is treated as nothing saved`() {
        for (garbage in listOf("", "not json", "{}", """[{"name":"Ada"}]""", """["Ada"]""")) {
            preferences.edit().putString(PreferencesLabelStore.KEY, garbage).commit()

            assertNull("for $garbage", store.load())
        }
    }

    @Test
    fun `a colour this version does not know costs only that colour, not the names`() {
        preferences.edit()
            .putString(
                PreferencesLabelStore.KEY,
                """[{"name":"Ada","colour":"Chartreuse"},{"name":"Grace","colour":"Pink"}]""",
            )
            .commit()

        assertEquals(
            listOf(
                StopwatchLabel("Ada", StopwatchLabel.default(0).colour),
                StopwatchLabel("Grace", StopwatchColour.Pink),
            ),
            store.load(),
        )
    }
}

package com.swetw.multilaps.timing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesRatingStoreTest {

    private val preferences = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("rating-test", Context.MODE_PRIVATE)

    @Test
    fun `a new install has not asked`() {
        assertFalse(PreferencesRatingStore(preferences).hasAsked())
    }

    @Test
    fun `having asked is remembered by the next run of the app`() {
        PreferencesRatingStore(preferences).recordAsked()

        assertTrue(PreferencesRatingStore(preferences).hasAsked())
    }
}

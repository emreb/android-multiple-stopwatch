package com.swetw.multilaps.timing

import android.content.SharedPreferences
import androidx.core.content.edit

/** Keeps whether this install has asked for a rating in [preferences]; uninstalling forgets it. */
class PreferencesRatingStore(private val preferences: SharedPreferences) : RatingStore {

    override fun hasAsked(): Boolean = preferences.getBoolean(ASKED, false)

    override fun recordAsked() {
        preferences.edit { putBoolean(ASKED, true) }
    }

    companion object {
        const val FILE_NAME = "rating"
        private const val ASKED = "asked"
    }
}

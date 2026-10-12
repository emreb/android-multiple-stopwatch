package com.brothersbaran.multilaps.timing

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Keeps the labels as one JSON value in [preferences], apart from the timing they go with. */
class PreferencesLabelStore(private val preferences: SharedPreferences) : LabelStore {

    override fun load(): List<StopwatchLabel>? {
        val json = preferences.getString(KEY, null) ?: return null
        return try {
            val labels = JSONArray(json)
            List(labels.length()) { decode(labels.getJSONObject(it), index = it) }
        } catch (_: JSONException) {
            null
        }
    }

    override fun save(labels: List<StopwatchLabel>) {
        preferences.edit { putString(KEY, JSONArray(labels.map(::encode)).toString()) }
    }

    private fun encode(label: StopwatchLabel) = JSONObject()
        .put(NAME, label.name)
        .put(COLOUR, label.colour.name)

    /**
     * A colour this version does not know, as after the palette changes, costs only that colour:
     * the stopwatch at [index] keeps its name and takes its default colour.
     */
    private fun decode(json: JSONObject, index: Int): StopwatchLabel {
        val colourName = json.getString(COLOUR)
        return StopwatchLabel(
            name = json.getString(NAME),
            colour = StopwatchColour.entries.find { it.name == colourName }
                ?: StopwatchLabel.default(index).colour,
        )
    }

    companion object {
        const val FILE_NAME = "labels"
        const val KEY = "labels"

        private const val NAME = "name"
        private const val COLOUR = "colour"
    }
}

package com.swetw.multilaps.timing

/** Where the stopwatches' names and colours are kept so they outlive the app's process. */
interface LabelStore {

    /** The labels last saved, by position, or null if there are none that can be read. */
    fun load(): List<StopwatchLabel>?

    fun save(labels: List<StopwatchLabel>)
}

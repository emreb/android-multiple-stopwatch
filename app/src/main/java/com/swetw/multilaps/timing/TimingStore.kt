package com.swetw.multilaps.timing

/** Where the stopwatches are kept so they outlive the app's process. */
interface TimingStore {

    /** The last timing saved, or null if there is none that can be read. */
    fun load(): SavedTiming?

    fun save(timing: SavedTiming)
}

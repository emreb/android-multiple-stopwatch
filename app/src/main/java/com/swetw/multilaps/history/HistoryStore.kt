package com.swetw.multilaps.history

/** Where results are kept so they outlive the app's process. */
interface HistoryStore {

    /** Every result that can be read, in no particular order; none if there is nothing readable. */
    fun load(): List<Result>

    fun save(results: List<Result>)
}

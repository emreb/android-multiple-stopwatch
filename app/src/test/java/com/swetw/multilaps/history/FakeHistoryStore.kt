package com.swetw.multilaps.history

class FakeHistoryStore(var saved: List<Result> = emptyList()) : HistoryStore {

    override fun load(): List<Result> = saved

    override fun save(results: List<Result>) {
        saved = results
    }
}

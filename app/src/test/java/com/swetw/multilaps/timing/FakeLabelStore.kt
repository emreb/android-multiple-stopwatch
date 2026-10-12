package com.swetw.multilaps.timing

class FakeLabelStore(var saved: List<StopwatchLabel>? = null) : LabelStore {

    override fun load(): List<StopwatchLabel>? = saved

    override fun save(labels: List<StopwatchLabel>) {
        saved = labels
    }
}

package com.brothersbaran.multilaps.timing

class FakeTimingStore(var saved: SavedTiming? = null) : TimingStore {

    var saveCount = 0
        private set

    override fun load(): SavedTiming? = saved

    override fun save(timing: SavedTiming) {
        saved = timing
        saveCount++
    }
}

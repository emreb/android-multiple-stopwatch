package com.swetw.multilaps.timing

class FakeRatingStore(var asked: Boolean = false) : RatingStore {

    override fun hasAsked(): Boolean = asked

    override fun recordAsked() {
        asked = true
    }
}

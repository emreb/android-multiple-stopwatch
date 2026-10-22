package com.swetw.multilaps.timing

/** Puts the request for a rating in front of the user, in whatever way the store offers. */
fun interface RatingPrompt {

    fun show()

    companion object {
        val None = RatingPrompt {}
    }
}

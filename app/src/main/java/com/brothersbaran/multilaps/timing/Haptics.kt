package com.brothersbaran.multilaps.timing

/** What a press did, as it is to be felt by a timekeeper who is watching the track. */
enum class Feedback { Start, Lap, Stop, Reset, Undo }

/** Lets a press that took effect be felt. A press that was ignored is never played. */
fun interface Haptics {

    fun play(feedback: Feedback)

    companion object {
        val None = Haptics {}
    }
}

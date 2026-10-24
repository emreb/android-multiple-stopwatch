package com.swetw.multilaps.timing

/** What may be written about one run: free text, on as many lines as wanted, up to [MAX_LENGTH] characters. */
object Note {
    const val MAX_LENGTH = 500

    /** [text] cut to the longest note allowed, without leaving half of a character. */
    fun cutToFit(text: String): String = cutToLength(text, MAX_LENGTH)

    /** [text] as a note is kept: space around it dropped and cut to fit, so a blank note is empty. */
    fun tidied(text: String): String = cutToFit(text.trim()).trim()
}

package com.brothersbaran.multilaps.timing

/** The colours a stopwatch can be told apart by, chosen to stay distinct from one another. */
enum class StopwatchColour { Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink }

/** What a stopwatch is told apart by. It belongs to the stopwatch's place, not to its timing. */
data class StopwatchLabel(val name: String, val colour: StopwatchColour) {

    /** This label under [text], tidied; a stopwatch at [index] with no name of its own goes by its number. */
    fun renamed(text: String, index: Int): StopwatchLabel =
        copy(name = text.trim().take(MAX_NAME_LENGTH).trim().ifEmpty { defaultName(index) })

    companion object {
        const val MAX_NAME_LENGTH = 20

        /** The same every time, so a stopwatch is the same colour from one day at the track to the next. */
        private val DEFAULT_COLOURS = listOf(
            StopwatchColour.Blue,
            StopwatchColour.Red,
            StopwatchColour.Green,
            StopwatchColour.Orange,
        )

        fun defaultName(index: Int): String = (index + 1).toString()

        fun default(index: Int) =
            StopwatchLabel(defaultName(index), DEFAULT_COLOURS[index % DEFAULT_COLOURS.size])
    }
}

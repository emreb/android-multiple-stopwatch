package com.brothersbaran.multilaps

/** Robolectric qualifiers for the window shapes the layout is tested at. */
object Devices {
    const val PORTRAIT_PHONE = "w411dp-h891dp-port"
    const val LANDSCAPE_PHONE = "w891dp-h411dp-land"
    /** The commonest phone size, on its side, as left after the status and navigation bars. */
    const val COMMON_PHONE_LANDSCAPE = "w760dp-h336dp-land"
    /** Wide but very short, as a phone on its side in split-screen: the tiles go to their short form. */
    const val SHORT_WIDE_WINDOW = "w891dp-h280dp-land"
    /** On its side and only just wide enough for the controls' column to sit beside the tiles. */
    const val BARELY_WIDE_ENOUGH = "w624dp-h320dp-land"
    const val SMALL_PHONE = "w320dp-h568dp-port"
    const val SMALL_PHONE_LANDSCAPE = "w568dp-h320dp-land"

    /** Half of an upright phone in split-screen: wider than tall, yet too narrow for four tiles. */
    const val SPLIT_SCREEN = "w411dp-h400dp"
    const val TABLET_PORTRAIT = "w800dp-h1280dp-port"
    const val TABLET_LANDSCAPE = "w1280dp-h800dp-land"
}

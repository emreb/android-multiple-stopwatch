package com.brothersbaran.multilaps

/** Robolectric qualifiers for the window shapes the layout is tested at. */
object Devices {
    const val PORTRAIT_PHONE = "w411dp-h891dp-port"
    const val LANDSCAPE_PHONE = "w891dp-h411dp-land"
    const val SMALL_PHONE = "w320dp-h568dp-port"
    const val SMALL_PHONE_LANDSCAPE = "w568dp-h320dp-land"

    /** Half of an upright phone in split-screen: wider than tall, yet too narrow for four tiles. */
    const val SPLIT_SCREEN = "w411dp-h400dp"
    const val TABLET_PORTRAIT = "w800dp-h1280dp-port"
    const val TABLET_LANDSCAPE = "w1280dp-h800dp-land"
}

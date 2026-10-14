package com.swetw.multilaps.stopwatch

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.os.SystemClock
import android.provider.Settings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class DeviceClock(private val contentResolver: ContentResolver) : Clock {

    /** Time since boot, which keeps advancing while the device sleeps. */
    override fun now(): Duration = SystemClock.elapsedRealtimeNanos().nanoseconds

    override fun wallNow(): Duration = System.currentTimeMillis().milliseconds

    // Before Android 7 the setting is not kept, so this reads 0, which SavedTiming allows for.
    @get:SuppressLint("InlinedApi")
    override val bootCount: Int
        get() = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
}

package com.swetw.multilaps

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AppIconTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun icon() = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)

    @Test
    @Config(sdk = [23])
    fun `phones older than Android 8 get a picture of the icon, as they cannot shape one`() {
        assertTrue("${icon()}", icon() is BitmapDrawable)
    }

    @Test
    fun `from Android 8 the icon is adaptive, so the launcher shapes it`() {
        assertTrue("${icon()}", icon() is AdaptiveIconDrawable)
    }
}

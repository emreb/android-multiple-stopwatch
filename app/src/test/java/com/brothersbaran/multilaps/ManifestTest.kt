package com.brothersbaran.multilaps

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the application identifier is com_brothersbaran_multilaps`() {
        assertEquals("com.brothersbaran.multilaps", context.packageName)
    }

    @Test
    fun `the app is named MultiLaps`() {
        assertEquals("MultiLaps", context.applicationInfo.loadLabel(context.packageManager).toString())
    }

    @Test
    fun `the screen is free to turn either way`() {
        val activity = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.screenOrientation)
    }

    @Test
    fun `the app requests no network access`() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()

        assertFalse(Manifest.permission.INTERNET in requested)
        assertFalse(Manifest.permission.ACCESS_NETWORK_STATE in requested)
    }
}

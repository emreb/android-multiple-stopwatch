package com.swetw.multilaps

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swetw.multilaps.history.PreferencesHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.io.File

@RunWith(AndroidJUnit4::class)
class ManifestTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the application identifier is com_swetw_multilaps`() {
        assertEquals("com.swetw.multilaps", context.packageName)
    }

    @Test
    fun `the app is named MultiLaps`() {
        assertEquals("MultiLaps", context.applicationInfo.loadLabel(context.packageManager).toString())
    }

    @Test
    fun `the app has a launcher icon of its own`() {
        assertTrue(context.applicationInfo.icon != 0)
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
    fun `nothing runs in the background, so there is no notification to show`() {
        val info = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SERVICES or PackageManager.GET_PERMISSIONS,
        )

        assertTrue(info.services.orEmpty().isEmpty())
        assertFalse(Manifest.permission.POST_NOTIFICATIONS in info.requestedPermissions.orEmpty())
    }

    @Test
    fun `the only permission the app asks for is to vibrate`() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            // Added by the support libraries for their own use, not a permission over anything.
            .filterNot { it.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }

        assertEquals(listOf(Manifest.permission.VIBRATE), requested)
    }

    @Test
    fun `nothing kept in preferences, history included, is backed up or copied to a new device`() {
        val excluded = mutableMapOf<String, MutableList<String>>()
        val rules = context.resources.getXml(R.xml.data_extraction_rules)
        var section = ""
        while (rules.next() != XmlPullParser.END_DOCUMENT) {
            if (rules.eventType != XmlPullParser.START_TAG) continue
            when (rules.name) {
                "cloud-backup", "device-transfer" -> section = rules.name
                "exclude" -> excluded.getOrPut(section) { mutableListOf() } +=
                    rules.getAttributeValue(null, "domain") + ":" + rules.getAttributeValue(null, "path")
            }
        }

        assertEquals(
            mapOf("cloud-backup" to listOf("sharedpref:."), "device-transfer" to listOf("sharedpref:.")),
            excluded,
        )
        // History is kept where that rule reaches: in a preferences file of its own.
        context.getSharedPreferences(PreferencesHistoryStore.FILE_NAME, Context.MODE_PRIVATE)
            .edit().putString(PreferencesHistoryStore.KEY, "{}").commit()
        assertTrue(File(context.dataDir, "shared_prefs/${PreferencesHistoryStore.FILE_NAME}.xml").exists())
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

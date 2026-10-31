package com.squatchsports.training

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Nearby 19.3 rejects discovery on modern Android if these permissions are capped at SDK 32. */
@RunWith(AndroidJUnit4::class)
class NearbyManifestTest {
    @Test fun nearbyWifiPermissionsAreGrantedOnTheRunningAndroidVersion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (permission in listOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE)) {
            assertEquals("Required by the pinned Nearby Connections SDK: $permission",
                PackageManager.PERMISSION_GRANTED, ContextCompat.checkSelfPermission(context, permission))
        }
    }
}

package com.squatchsports.training.nearby

import org.junit.Assert.*
import org.junit.Test

class NearbyPermissionsTest {
    @Test fun android8UsesCoarseLocation() {
        assertEquals(listOf("android.permission.ACCESS_COARSE_LOCATION"), nearbyRuntimePermissions(26))
    }
    @Test fun android10RequiresBothLocationPermissions() {
        assertEquals(setOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"), nearbyRuntimePermissions(29).toSet())
    }
    @Test fun android12RequestsBluetoothAndLocation() {
        val permissions = nearbyRuntimePermissions(31)
        assertEquals(5, permissions.size)
        assertTrue("android.permission.BLUETOOTH_SCAN" in permissions)
        assertTrue("android.permission.ACCESS_FINE_LOCATION" in permissions)
    }
    @Test fun android13UsesNearbyWifiWithoutLocation() {
        val permissions = nearbyRuntimePermissions(33)
        assertEquals(4, permissions.size)
        assertTrue("android.permission.NEARBY_WIFI_DEVICES" in permissions)
        assertFalse(permissions.any { "LOCATION" in it })
    }
    @Test fun android17AlsoRequestsLocalNetworkPermission() {
        assertEquals(nearbyRuntimePermissions(33) + "android.permission.ACCESS_LOCAL_NETWORK", nearbyRuntimePermissions(37))
    }
}

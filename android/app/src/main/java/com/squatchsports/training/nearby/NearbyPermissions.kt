package com.squatchsports.training.nearby

/** Mirrors Google's Nearby Connections requirements, including local network access on target SDK 37. */
fun nearbyRuntimePermissions(sdk: Int): List<String> = buildList {
    if (sdk <= 32) {
        add("android.permission.ACCESS_COARSE_LOCATION")
        if (sdk >= 29) add("android.permission.ACCESS_FINE_LOCATION")
    }
    if (sdk >= 31) {
        add("android.permission.BLUETOOTH_ADVERTISE")
        add("android.permission.BLUETOOTH_CONNECT")
        add("android.permission.BLUETOOTH_SCAN")
    }
    if (sdk >= 33) add("android.permission.NEARBY_WIFI_DEVICES")
    if (sdk >= 37) add("android.permission.ACCESS_LOCAL_NETWORK")
}

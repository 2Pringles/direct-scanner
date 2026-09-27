package com.example.directscanner

/**
 * Returns true only if [ssid] starts with the exact, all-caps "DIRECT-"
 * prefix (e.g. "DIRECT-0F-HP Color LJ 3301", "DIRECT-F5-HP Color LJ 3301").
 * The wording after the prefix varies a lot between devices, so the
 * "DIRECT-" prefix itself is the only reliable, general signal -- no
 * brand/keyword matching is used or needed.
 */
fun isTargetNetwork(ssid: String?): Boolean {
    if (ssid.isNullOrBlank()) return false
    return ssid.startsWith("DIRECT-")
}

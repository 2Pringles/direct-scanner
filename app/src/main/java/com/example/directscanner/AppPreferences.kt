package com.example.directscanner

import android.content.Context

private const val PREFS_NAME = "direct_scanner_prefs"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
private const val KEY_IGNORED_SSIDS = "ignored_ssids"

/** Small persisted-settings wrapper so toggles survive app restarts. */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    /** Exact SSIDs the user has chosen to stop tracking (e.g. a phone or TV's
     *  own "DIRECT-" broadcast that isn't an actual cruiser in the game). */
    var ignoredSsids: Set<String>
        get() = prefs.getStringSet(KEY_IGNORED_SSIDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_IGNORED_SSIDS, value).apply()

    fun ignore(ssid: String) {
        ignoredSsids = ignoredSsids + ssid
    }

    fun unignore(ssid: String) {
        ignoredSsids = ignoredSsids - ssid
    }
}

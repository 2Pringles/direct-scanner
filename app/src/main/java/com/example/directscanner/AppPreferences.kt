package com.example.directscanner

import android.content.Context

private const val PREFS_NAME = "direct_scanner_prefs"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

/** Small persisted-settings wrapper so toggles survive app restarts. */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
}

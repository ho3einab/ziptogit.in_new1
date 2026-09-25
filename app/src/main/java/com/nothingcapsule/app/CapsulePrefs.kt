package com.nothingcapsule.app

import android.content.Context

class CapsulePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("capsule_prefs", Context.MODE_PRIVATE)

    var isCapsuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var lowBatteryThreshold: Int
        get() = prefs.getInt(KEY_LOW_BATTERY, 20)
        set(value) = prefs.edit().putInt(KEY_LOW_BATTERY, value).apply()

    /** Package names whose notifications are allowed to appear in the capsule. */
    var whitelistedApps: Set<String>
        get() = prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_WHITELIST, value).apply()

    companion object {
        private const val KEY_ENABLED = "capsule_enabled"
        private const val KEY_LOW_BATTERY = "low_battery_threshold"
        private const val KEY_WHITELIST = "notification_whitelist"
    }
}

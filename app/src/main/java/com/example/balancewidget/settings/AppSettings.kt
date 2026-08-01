package com.example.balancewidget.settings

import android.content.Context
import android.graphics.Color

/** User preferences that affect the app and widget presentation. */
class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var dynamicColorEnabled: Boolean
        get() = preferences.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    var customColor: Int
        get() = Color.parseColor(preferences.getString(KEY_CUSTOM_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR)
        set(value) = preferences.edit().putString(KEY_CUSTOM_COLOR, String.format("#%08X", value)).apply()

    var refreshIntervalMinutes: Int
        get() = preferences.getInt(KEY_REFRESH_INTERVAL, 30)
        set(value) = preferences.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()

    companion object {
        private const val NAME = "settings"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_CUSTOM_COLOR = "custom_color"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval"
        private const val DEFAULT_COLOR = "#405F91"
    }
}

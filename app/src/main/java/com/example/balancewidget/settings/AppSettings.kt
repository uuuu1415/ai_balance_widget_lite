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

    /** The value supplied by the user. A value of zero disables background refresh. */
    var refreshIntervalValue: Long
        get() = preferences.getLong(KEY_REFRESH_INTERVAL_VALUE, legacyRefreshMinutes().toLong())
        set(value) = preferences.edit().putLong(KEY_REFRESH_INTERVAL_VALUE, value).apply()

    var refreshIntervalUnit: RefreshIntervalUnit
        get() = RefreshIntervalUnit.fromId(preferences.getString(KEY_REFRESH_INTERVAL_UNIT, null))
        set(value) = preferences.edit().putString(KEY_REFRESH_INTERVAL_UNIT, value.id).apply()

    fun refreshIntervalMillis(): Long = refreshIntervalUnit.toMillis(refreshIntervalValue)

    private fun legacyRefreshMinutes(): Int = preferences.getInt(KEY_LEGACY_REFRESH_INTERVAL, 30)

    companion object {
        private const val NAME = "settings"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_CUSTOM_COLOR = "custom_color"
        private const val KEY_LEGACY_REFRESH_INTERVAL = "refresh_interval"
        private const val KEY_REFRESH_INTERVAL_VALUE = "refresh_interval_value"
        private const val KEY_REFRESH_INTERVAL_UNIT = "refresh_interval_unit"
        private const val DEFAULT_COLOR = "#405F91"
    }
}

enum class RefreshIntervalUnit(val id: String, val label: String, private val multiplier: Long) {
    SECONDS("seconds", "秒", 1_000L),
    MINUTES("minutes", "分钟", 60_000L);

    fun toMillis(value: Long): Long = value * multiplier

    companion object {
        fun fromId(id: String?): RefreshIntervalUnit = entries.firstOrNull { it.id == id } ?: MINUTES
    }
}

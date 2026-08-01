package com.example.balancewidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.balancewidget.settings.AppSettings

/** Schedules inexact background refreshes while respecting Android power policies. */
object WidgetRefreshScheduler {
    fun apply(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = refreshPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        val requestedInterval = AppSettings(context).refreshIntervalMillis()
        if (requestedInterval == 0L) return

        // Android can batch repeating alarms under power-saving policies. The configured unit is
        // preserved, while the registered value is clamped to the platform-safe one-minute floor.
        val interval = maxOf(requestedInterval, MINIMUM_INTERVAL_MS)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + interval,
            interval,
            pendingIntent
        )
    }

    fun refreshPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        300,
        Intent(context, MultiBalanceWidget::class.java).setAction(WidgetRefresh.ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val MINIMUM_INTERVAL_MS = 60_000L
}

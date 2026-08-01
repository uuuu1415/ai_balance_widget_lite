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

        val minutes = AppSettings(context).refreshIntervalMinutes
        if (minutes == 0) return

        val interval = minutes * 60_000L
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
}

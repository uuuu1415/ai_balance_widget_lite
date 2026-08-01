package com.example.balancewidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import com.example.balancewidget.data.AccountStore
import com.example.balancewidget.data.BalanceClient
import com.example.balancewidget.data.SnapshotStore

/** Shared background work used by both widget receivers. */
object WidgetRefresh {
    const val ACTION_REFRESH = "com.example.balancewidget.REFRESH"
    const val ACTION_RENDER = "com.example.balancewidget.RENDER"

    fun isRefreshAction(intent: Intent): Boolean =
        intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE

    fun isRenderAction(intent: Intent): Boolean = intent.action == ACTION_RENDER

    suspend fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val accountStore = AccountStore(appContext)
        val snapshotStore = SnapshotStore(appContext)
        val client = BalanceClient()
        accountStore.accounts().forEach { snapshotStore.save(client.fetch(it)) }
    }

    suspend fun refreshFirst(context: Context) {
        val appContext = context.applicationContext
        val account = AccountStore(appContext).accounts().firstOrNull() ?: return
        SnapshotStore(appContext).save(BalanceClient().fetch(account))
    }

    fun pendingIntent(context: Context, receiver: Class<out BroadcastReceiver>, requestCode: Int) =
        android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).setAction(ACTION_REFRESH),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

    fun activityPendingIntent(context: Context, requestCode: Int) =
        android.app.PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, com.example.balancewidget.MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
}

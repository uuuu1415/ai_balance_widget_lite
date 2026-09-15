package com.example.balancewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.balancewidget.R
import com.example.balancewidget.data.AccountStore
import com.example.balancewidget.data.SnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Home-screen widget showing all configured accounts in a compact list. */
class MultiBalanceWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (WidgetRefresh.isRenderAction(intent)) {
            updateWidgets(context)
            return
        }
        if (!WidgetRefresh.isRefreshAction(intent)) {
            super.onReceive(context, intent)
            return
        }

        // Restore cached content and click targets before waiting for the network.
        updateWidgets(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetRefresh.refreshAll(context)
                WidgetUpdater.update(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    private fun updateWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MultiBalanceWidget::class.java)
        onUpdate(context, manager, manager.getAppWidgetIds(component))
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val accounts = AccountStore(context).accounts()
        val snapshots = SnapshotStore(context)
        val lines = accounts.map { account ->
            val snapshot = snapshots.get(account.id)
            "${account.name}: ${snapshot?.balance ?: "-"} ${snapshot?.currency.orEmpty()}"
        }
        val views = RemoteViews(context.packageName, R.layout.widget_multi)
        views.setTextViewText(R.id.accounts, lines.ifEmpty { listOf("打开应用添加账户") }.joinToString("\n"))
        views.setOnClickPendingIntent(R.id.accounts, WidgetRefresh.activityPendingIntent(context, 200))
        views.setOnClickPendingIntent(R.id.refresh, WidgetRefresh.pendingIntent(context, MultiBalanceWidget::class.java, 201))
        manager.updateAppWidget(widgetId, views)
    }
}

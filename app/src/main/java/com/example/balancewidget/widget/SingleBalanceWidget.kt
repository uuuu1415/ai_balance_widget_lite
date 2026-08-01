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
import java.text.DateFormat

/** Home-screen widget showing the first configured account. */
class SingleBalanceWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (WidgetRefresh.isRenderAction(intent)) {
            updateWidgets(context)
            return
        }
        if (!WidgetRefresh.isRefreshAction(intent)) {
            super.onReceive(context, intent)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetRefresh.refreshFirst(context)
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
        val component = ComponentName(context, SingleBalanceWidget::class.java)
        onUpdate(context, manager, manager.getAppWidgetIds(component))
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val account = AccountStore(context).accounts().firstOrNull()
        val snapshot = account?.let { SnapshotStore(context).get(it.id) }
        val views = RemoteViews(context.packageName, R.layout.widget_single)

        views.setTextViewText(R.id.provider, account?.name ?: "余额小组件")
        views.setTextViewText(R.id.balance, balanceText(snapshot))
        views.setTextViewText(R.id.details, detailsText(snapshot))
        views.setOnClickPendingIntent(R.id.provider, WidgetRefresh.activityPendingIntent(context, 100))
        views.setOnClickPendingIntent(R.id.refresh, WidgetRefresh.pendingIntent(context, SingleBalanceWidget::class.java, 101))
        manager.updateAppWidget(widgetId, views)
    }

    private fun balanceText(snapshot: com.example.balancewidget.data.BalanceSnapshot?): String =
        snapshot?.balance?.let { "$it ${snapshot.currency.orEmpty()}" } ?: "请先添加账户"

    private fun detailsText(snapshot: com.example.balancewidget.data.BalanceSnapshot?): String = when {
        snapshot == null -> "打开应用添加账户"
        snapshot.error != null -> "更新失败，点击应用查看"
        else -> "更新于 ${DateFormat.getTimeInstance(DateFormat.SHORT).format(snapshot.fetchedAt)}"
    }
}

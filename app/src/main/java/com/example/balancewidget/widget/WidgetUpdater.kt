package com.example.balancewidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    /** Updates visible widget content from the existing cache without making network requests. */
    fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val single = manager.getAppWidgetIds(android.content.ComponentName(context, SingleBalanceWidget::class.java))
        val multi = manager.getAppWidgetIds(android.content.ComponentName(context, MultiBalanceWidget::class.java))
        context.sendBroadcast(Intent(context, SingleBalanceWidget::class.java).setAction(WidgetRefresh.ACTION_RENDER).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, single))
        context.sendBroadcast(Intent(context, MultiBalanceWidget::class.java).setAction(WidgetRefresh.ACTION_RENDER).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, multi))
    }
}

package com.example.balancewidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context

object WidgetUpdater {
    /** Updates visible widget content from the existing cache without making network requests. */
    fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val single = manager.getAppWidgetIds(android.content.ComponentName(context, SingleBalanceWidget::class.java))
        val multi = manager.getAppWidgetIds(android.content.ComponentName(context, MultiBalanceWidget::class.java))
        SingleBalanceWidget().onUpdate(context, manager, single)
        MultiBalanceWidget().onUpdate(context, manager, multi)
    }
}

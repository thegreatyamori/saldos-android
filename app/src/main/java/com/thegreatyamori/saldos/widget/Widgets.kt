package com.thegreatyamori.saldos.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll

object Widgets {

    suspend fun updateAll(context: Context) {
        BalanceWidget().updateAll(context)
        SquareBalanceWidget().updateAll(context)
    }

    fun requestPin(context: Context, receiver: Class<out AppWidgetProvider>) {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return
        manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }
}

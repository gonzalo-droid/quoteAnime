package com.gondroid.quoteanime.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateRoutineSummaryWidgetWorker

/** Same shape as [QuoteWidgetReceiver]: onUpdate fires on widget add / device reboot,
 *  and just enqueues the worker that actually reads habit data and renders it. */
class RoutineSummaryWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = RoutineSummaryWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueUpdateWork(context)
    }

    companion object {
        fun enqueueUpdateWork(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<UpdateRoutineSummaryWidgetWorker>().build())
        }
    }
}

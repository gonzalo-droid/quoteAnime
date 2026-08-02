package com.gondroid.quoteanime.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateHabitWidgetWorker

/**
 * Unlike [RoutineSummaryWidgetReceiver], each instance of this widget is bound to a
 * different habit (picked via [com.gondroid.quoteanime.presentation.widget.HabitWidgetConfigureActivity]
 * when the widget is added — see that Activity's `android:configure` wiring in
 * habit_widget_info.xml). `onUpdate` only matters for periodic refresh and reboot; the
 * initial data load happens right after configuration, not here.
 */
class HabitWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = HabitWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueUpdateAllWork(context)
    }

    companion object {
        fun enqueueUpdateAllWork(context: Context) {
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<UpdateHabitWidgetWorker>().build())
        }
    }
}

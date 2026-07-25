package com.gondroid.quoteanime.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateQuoteWidgetWorker

/**
 * Punto de entrada del sistema Android hacia el widget.
 * onUpdate se llama cuando:
 *   - El usuario añade el widget a la pantalla de inicio.
 *   - El dispositivo reinicia (WorkManager restaura sus workers, pero
 *     Android también invoca onUpdate para restaurar el estado visual).
 */
class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()

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
                .enqueue(OneTimeWorkRequestBuilder<UpdateQuoteWidgetWorker>().build())
        }
    }
}

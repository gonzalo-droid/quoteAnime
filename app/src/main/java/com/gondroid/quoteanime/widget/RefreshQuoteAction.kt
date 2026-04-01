package com.gondroid.quoteanime.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.worker.UpdateQuoteWidgetWorker

/**
 * Acción ejecutada cuando el usuario toca el botón de refrescar en el widget.
 * Muestra el estado de carga y encola el worker que obtiene una nueva frase.
 */
class RefreshQuoteAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Mostrar loading inmediatamente para dar feedback al usuario
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[QuoteWidgetState.IS_LOADING] = true
                this[QuoteWidgetState.HAS_ERROR] = false
            }
        }
        QuoteWidget().update(context, glanceId)

        // Encolar el worker que obtiene la frase desde Firestore
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<UpdateQuoteWidgetWorker>().build())
    }
}

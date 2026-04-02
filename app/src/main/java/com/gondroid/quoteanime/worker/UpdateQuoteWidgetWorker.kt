package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.widget.QuoteWidget
import com.gondroid.quoteanime.widget.QuoteWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class UpdateQuoteWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds(QuoteWidget::class.java)

        if (glanceIds.isEmpty()) return Result.success()

        return runCatching {
            val preferences = getUserPreferences().first()
            val quote       = getRandomQuote(preferences.selectedCategoryIds)
            val widgetSize  = preferences.widgetSize.name

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        if (quote != null) {
                            this[QuoteWidgetState.QUOTE_TEXT]   = quote.quote
                            this[QuoteWidgetState.QUOTE_AUTHOR] = quote.author
                            this[QuoteWidgetState.QUOTE_ID]     = quote.id
                            this[QuoteWidgetState.QUOTE_ANIME]  = quote.anime
                            this[QuoteWidgetState.WIDGET_SIZE]  = widgetSize
                            this[QuoteWidgetState.IS_LOADING]   = false
                            this[QuoteWidgetState.HAS_ERROR]    = false
                        } else {
                            this[QuoteWidgetState.IS_LOADING] = false
                            this[QuoteWidgetState.HAS_ERROR]  = true
                        }
                    }
                }
                QuoteWidget().update(context, glanceId)
            }
            Result.success()
        }.getOrElse {
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[QuoteWidgetState.IS_LOADING] = false
                        this[QuoteWidgetState.HAS_ERROR]  = true
                    }
                }
                QuoteWidget().update(context, glanceId)
            }
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

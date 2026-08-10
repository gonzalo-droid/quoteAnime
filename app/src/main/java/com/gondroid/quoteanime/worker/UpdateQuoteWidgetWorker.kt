package com.gondroid.quoteanime.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.imageLoader
import coil.request.ImageRequest
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.presentation.components.optimizedForDisplay
import com.gondroid.quoteanime.widget.QuoteWidget
import com.gondroid.quoteanime.widget.QuoteWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

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
            // Preferences (the widget's Glance state store) can only hold primitives, not a
            // Bitmap, so the photo is downloaded once here, cached to a file, and only its
            // content:// URI is written to state — RemoteViews/Icon know how to resolve that
            // URI in the launcher's process (same mechanism the system uses for any widget
            // that shows a private image), no manual permission grant needed.
            val backgroundUri = quote?.imageUrl?.let { downloadBackgroundImage(context, it) }

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        if (quote != null) {
                            this[QuoteWidgetState.QUOTE_TEXT]   = quote.quote.orEmpty()
                            this[QuoteWidgetState.QUOTE_AUTHOR] = quote.author.orEmpty()
                            this[QuoteWidgetState.QUOTE_ID]     = quote.id
                            this[QuoteWidgetState.QUOTE_ANIME]  = quote.anime.orEmpty()
                            this[QuoteWidgetState.IS_LOADING]   = false
                            this[QuoteWidgetState.HAS_ERROR]    = false
                            if (backgroundUri != null) {
                                this[QuoteWidgetState.BACKGROUND_IMAGE_URI] = backgroundUri
                            } else {
                                remove(QuoteWidgetState.BACKGROUND_IMAGE_URI)
                            }
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

    /** Returns a content:// URI for the locally-cached copy of [imageUrl], or null if the
     *  download/decode fails — callers fall back to the plain gradient background. */
    private suspend fun downloadBackgroundImage(context: Context, imageUrl: String): String? =
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(imageUrl.optimizedForDisplay())
                .allowHardware(false) // needed to read pixels via BitmapDrawable below
                .build()
            val bitmap = (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
                ?: return@runCatching null
            val file = File(context.cacheDir, "widget_quote_bg.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file).toString()
        }.getOrNull()
}

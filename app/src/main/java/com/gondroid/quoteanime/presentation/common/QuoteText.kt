package com.gondroid.quoteanime.presentation.common

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.model.textFor
import java.util.Locale

/*
 * Picks the quote's text for the language the app is showing — Spanish (`quote`) or English
 * (`quoteEn`, with the Spanish as fallback, see [textFor]).
 *
 * The language comes from the configuration, not `Locale.getDefault()`: the configuration is
 * what the per-app language (Settings › Language) changes, and composables reading
 * [LocalConfiguration] recompose when it does.
 */

/** The language this configuration shows the app in. */
val Configuration.appLocale: Locale
    get() = locales[0] ?: Locale.getDefault()

/** The quote text in the language of this context — for notifications, widgets and images. */
fun Context.quoteText(quote: Quote): String = quote.textFor(resources.configuration.appLocale)

/** The quote text in the language on screen. */
@Composable
fun Quote.localizedText(): String = textFor(LocalConfiguration.current.appLocale)

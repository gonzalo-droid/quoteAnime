package com.gondroid.quoteanime.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Claves del estado del widget almacenado en PreferencesGlanceStateDefinition.
 * Cada instancia del widget en la pantalla de inicio tiene su propio DataStore.
 */
object QuoteWidgetState {
    val QUOTE_TEXT = stringPreferencesKey("widget_quote_text")
    val QUOTE_AUTHOR = stringPreferencesKey("widget_quote_author")
    val IS_LOADING = booleanPreferencesKey("widget_is_loading")
    val HAS_ERROR = booleanPreferencesKey("widget_has_error")
}

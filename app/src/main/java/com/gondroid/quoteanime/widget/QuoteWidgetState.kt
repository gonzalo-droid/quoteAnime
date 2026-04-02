package com.gondroid.quoteanime.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object QuoteWidgetState {
    val QUOTE_TEXT   = stringPreferencesKey("widget_quote_text")
    val QUOTE_AUTHOR = stringPreferencesKey("widget_quote_author")
    val QUOTE_ID     = stringPreferencesKey("widget_quote_id")
    val QUOTE_ANIME  = stringPreferencesKey("widget_quote_anime")
    val WIDGET_SIZE  = stringPreferencesKey("widget_size")
    val IS_LOADING   = booleanPreferencesKey("widget_is_loading")
    val HAS_ERROR    = booleanPreferencesKey("widget_has_error")
}

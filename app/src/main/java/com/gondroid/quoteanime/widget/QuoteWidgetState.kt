package com.gondroid.quoteanime.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object QuoteWidgetState {
    val QUOTE_TEXT   = stringPreferencesKey("widget_quote_text")
    val QUOTE_AUTHOR = stringPreferencesKey("widget_quote_author")
    val QUOTE_ID     = stringPreferencesKey("widget_quote_id")
    val QUOTE_ANIME  = stringPreferencesKey("widget_quote_anime")
    val IS_LOADING   = booleanPreferencesKey("widget_is_loading")
    val HAS_ERROR    = booleanPreferencesKey("widget_has_error")
    /** content:// URI (via FileProvider) of the quote's background photo, downloaded and
     *  cached locally by the worker — Preferences can't hold a Bitmap directly. Null/absent
     *  when the quote has no image or the download failed; the widget falls back to the
     *  plain gradient in that case. */
    val BACKGROUND_IMAGE_URI = stringPreferencesKey("widget_background_image_uri")
}

package com.gondroid.quoteanime.presentation.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/**
 * The app's own language, independent of the phone's. Backed by the system per-app language
 * ([LocaleManager], Android 13+), so the choice is the same one shown in the system's
 * Settings > Apps > Language page, survives restarts, and also applies to the notifications,
 * widgets and workers. Below Android 13 there is no such API without AppCompat, and the app
 * just follows the phone's language — the Settings row is hidden there.
 *
 * The offered languages must match `res/xml/locales_config.xml` and the `values-*` folders.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    SPANISH("es");

    companion object {
        val isPickerAvailable: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        /**
         * Maps the stored per-app locale back to an option. Only the language counts ("es-PE"
         * is Spanish); anything this app doesn't ship falls back to [SYSTEM] rather than
         * showing no selection.
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag.orEmpty().substringBefore('-').substringBefore('_').lowercase()
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun current(context: Context): AppLanguage {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) SYSTEM else fromTag(locales[0].toLanguageTag())
        }

        /** The system recreates the activity with the new configuration on its own. */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun apply(context: Context, language: AppLanguage) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (language == SYSTEM) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(language.tag)
        }
    }
}

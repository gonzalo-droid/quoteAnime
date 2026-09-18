package com.gondroid.quoteanime.presentation.settings

import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize

data class SettingsUiState(
    /** Anime names for the selector (`Quote.anime`), sorted. Not the Catalogue's emotions. */
    val animes: List<String> = emptyList(),
    /** True until the anime list first arrives; the rest of the screen doesn't wait for it. */
    val animesLoading: Boolean = true,
    /** The selected anime names; empty = every anime. Saved as `selectedCategoryIds`. */
    val selectedAnimes: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val notificationStartHour: Int = 8,
    val notificationStartMinute: Int = 0,
    val notificationEndHour: Int = 22,
    val notificationEndMinute: Int = 0,
    val notificationFrequency: Int = 1,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val widgetUpdateTimesPerDay: Int = 2,
    val isLoading: Boolean = true,
    val permissionDeniedPermanently: Boolean = false,
    val isPremium: Boolean = false
) {
    val allAnimesSelected: Boolean get() = selectedAnimes.isEmpty()

    fun toUserPreferences() = UserPreferences(
        selectedCategoryIds     = selectedAnimes,
        notificationsEnabled    = notificationsEnabled,
        notificationStartHour   = notificationStartHour,
        notificationStartMinute = notificationStartMinute,
        notificationEndHour     = notificationEndHour,
        notificationEndMinute   = notificationEndMinute,
        notificationFrequency   = notificationFrequency,
        widgetSize              = widgetSize,
        widgetUpdateTimesPerDay = widgetUpdateTimesPerDay
    )
}

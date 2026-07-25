package com.gondroid.quoteanime.presentation.settings

import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize

data class SettingsUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val notificationStartHour: Int = 8,
    val notificationStartMinute: Int = 0,
    val notificationEndHour: Int = 22,
    val notificationEndMinute: Int = 0,
    val notificationFrequency: Int = 1,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val widgetUpdateTimesPerDay: Int = 2,
    val isLoading: Boolean = true,
    val permissionDeniedPermanently: Boolean = false
) {
    val allCategoriesSelected: Boolean get() = selectedCategoryIds.isEmpty()

    fun toUserPreferences() = UserPreferences(
        selectedCategoryIds     = selectedCategoryIds,
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

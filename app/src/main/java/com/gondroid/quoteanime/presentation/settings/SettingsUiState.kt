package com.gondroid.quoteanime.presentation.settings

import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences

data class SettingsUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY,
    val isLoading: Boolean = true,
    val permissionDeniedPermanently: Boolean = false
) {
    /** Set vacío = todas las categorías activas */
    val allCategoriesSelected: Boolean get() = selectedCategoryIds.isEmpty()

    fun toUserPreferences() = UserPreferences(
        selectedCategoryIds = selectedCategoryIds,
        notificationsEnabled = notificationsEnabled,
        notificationHour = notificationHour,
        notificationMinute = notificationMinute,
        notificationFrequency = notificationFrequency
    )
}

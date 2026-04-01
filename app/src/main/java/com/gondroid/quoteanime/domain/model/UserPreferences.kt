package com.gondroid.quoteanime.domain.model

data class UserPreferences(
    val selectedCategoryIds: Set<String> = emptySet(),   // vacío = todas las categorías
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY
)

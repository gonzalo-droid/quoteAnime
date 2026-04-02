package com.gondroid.quoteanime.domain.model

data class UserPreferences(
    val selectedCategoryIds: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val notificationStartHour: Int = 8,
    val notificationStartMinute: Int = 0,
    val notificationEndHour: Int = 22,
    val notificationEndMinute: Int = 0,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val widgetUpdateTimesPerDay: Int = 2
)

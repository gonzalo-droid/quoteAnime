package com.gondroid.quoteanime.domain.model

data class UserPreferences(
    /**
     * The Settings anime selection: **anime names** (`Quote.anime`), empty = every anime. The
     * name predates the selector and is kept for the DataStore key and parity with iOS; it
     * holds no emotion categories. See `AnimeSelection.kt`.
     */
    val selectedCategoryIds: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val notificationStartHour: Int = 8,
    val notificationStartMinute: Int = 0,
    val notificationEndHour: Int = 22,
    val notificationEndMinute: Int = 0,
    val notificationFrequency: Int = 1, // times per day, range 1–10
    val lastNotificationQuoteId: String = "",
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val widgetUpdateTimesPerDay: Int = 2
)

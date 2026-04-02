package com.gondroid.quoteanime.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SELECTED_CATEGORY_IDS       = stringSetPreferencesKey("selected_category_ids")
        val NOTIFICATIONS_ENABLED       = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_START_HOUR     = intPreferencesKey("notification_start_hour")
        val NOTIFICATION_START_MINUTE   = intPreferencesKey("notification_start_minute")
        val NOTIFICATION_END_HOUR       = intPreferencesKey("notification_end_hour")
        val NOTIFICATION_END_MINUTE     = intPreferencesKey("notification_end_minute")
        val NOTIFICATION_FREQUENCY      = stringPreferencesKey("notification_frequency")
        val WIDGET_SIZE                 = stringPreferencesKey("widget_size")
        val WIDGET_UPDATE_TIMES_PER_DAY = intPreferencesKey("widget_update_times_per_day")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            selectedCategoryIds     = prefs[Keys.SELECTED_CATEGORY_IDS] ?: emptySet(),
            notificationsEnabled    = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            notificationStartHour   = prefs[Keys.NOTIFICATION_START_HOUR] ?: 8,
            notificationStartMinute = prefs[Keys.NOTIFICATION_START_MINUTE] ?: 0,
            notificationEndHour     = prefs[Keys.NOTIFICATION_END_HOUR] ?: 22,
            notificationEndMinute   = prefs[Keys.NOTIFICATION_END_MINUTE] ?: 0,
            notificationFrequency   = prefs[Keys.NOTIFICATION_FREQUENCY]
                ?.let { runCatching { NotificationFrequency.valueOf(it) }.getOrNull() }
                ?: NotificationFrequency.DAILY,
            widgetSize              = prefs[Keys.WIDGET_SIZE]
                ?.let { runCatching { WidgetSize.valueOf(it) }.getOrNull() }
                ?: WidgetSize.MEDIUM,
            widgetUpdateTimesPerDay = prefs[Keys.WIDGET_UPDATE_TIMES_PER_DAY] ?: 2
        )
    }

    suspend fun updateSelectedCategories(ids: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_CATEGORY_IDS] = ids }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotificationTimeRange(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    ) {
        dataStore.edit {
            it[Keys.NOTIFICATION_START_HOUR]   = startHour
            it[Keys.NOTIFICATION_START_MINUTE] = startMinute
            it[Keys.NOTIFICATION_END_HOUR]     = endHour
            it[Keys.NOTIFICATION_END_MINUTE]   = endMinute
        }
    }

    suspend fun updateNotificationFrequency(frequency: NotificationFrequency) {
        dataStore.edit { it[Keys.NOTIFICATION_FREQUENCY] = frequency.name }
    }

    suspend fun updateWidgetSize(size: WidgetSize) {
        dataStore.edit { it[Keys.WIDGET_SIZE] = size.name }
    }

    suspend fun updateWidgetUpdateTimesPerDay(times: Int) {
        dataStore.edit { it[Keys.WIDGET_UPDATE_TIMES_PER_DAY] = times }
    }
}

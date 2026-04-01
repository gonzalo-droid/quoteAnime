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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val SELECTED_CATEGORY_IDS = stringSetPreferencesKey("selected_category_ids")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val NOTIFICATION_FREQUENCY = stringPreferencesKey("notification_frequency")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            selectedCategoryIds = prefs[Keys.SELECTED_CATEGORY_IDS] ?: emptySet(),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 8,
            notificationMinute = prefs[Keys.NOTIFICATION_MINUTE] ?: 0,
            notificationFrequency = prefs[Keys.NOTIFICATION_FREQUENCY]
                ?.let { runCatching { NotificationFrequency.valueOf(it) }.getOrNull() }
                ?: NotificationFrequency.DAILY
        )
    }

    suspend fun updateSelectedCategories(categoryIds: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_CATEGORY_IDS] = categoryIds }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotificationTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.NOTIFICATION_HOUR] = hour
            it[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun updateNotificationFrequency(frequency: NotificationFrequency) {
        dataStore.edit { it[Keys.NOTIFICATION_FREQUENCY] = frequency.name }
    }
}

package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updateSelectedCategories(categoryIds: Set<String>)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateNotificationTime(hour: Int, minute: Int)
    suspend fun updateNotificationFrequency(frequency: NotificationFrequency)
}

package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updateSelectedCategories(categoryIds: Set<String>)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateNotificationTimeRange(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    )
    suspend fun updateNotificationFrequency(frequency: NotificationFrequency)
    suspend fun updateWidgetSize(size: WidgetSize)
    suspend fun updateWidgetUpdateTimesPerDay(times: Int)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted()
}

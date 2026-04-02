package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> = dataStore.userPreferences

    override suspend fun updateSelectedCategories(categoryIds: Set<String>) =
        dataStore.updateSelectedCategories(categoryIds)

    override suspend fun updateNotificationsEnabled(enabled: Boolean) =
        dataStore.updateNotificationsEnabled(enabled)

    override suspend fun updateNotificationTimeRange(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    ) = dataStore.updateNotificationTimeRange(startHour, startMinute, endHour, endMinute)

    override suspend fun updateNotificationFrequency(frequency: NotificationFrequency) =
        dataStore.updateNotificationFrequency(frequency)

    override suspend fun updateWidgetSize(size: WidgetSize) =
        dataStore.updateWidgetSize(size)

    override suspend fun updateWidgetUpdateTimesPerDay(times: Int) =
        dataStore.updateWidgetUpdateTimesPerDay(times)
}

package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.UserPreferences
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

    override suspend fun updateNotificationTime(hour: Int, minute: Int) =
        dataStore.updateNotificationTime(hour, minute)

    override suspend fun updateNotificationFrequency(frequency: NotificationFrequency) =
        dataStore.updateNotificationFrequency(frequency)
}

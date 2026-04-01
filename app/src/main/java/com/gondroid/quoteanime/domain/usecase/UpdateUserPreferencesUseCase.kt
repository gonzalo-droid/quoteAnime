package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class UpdateUserPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend fun setCategories(categoryIds: Set<String>) =
        repository.updateSelectedCategories(categoryIds)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        repository.updateNotificationsEnabled(enabled)

    suspend fun setNotificationTime(hour: Int, minute: Int) =
        repository.updateNotificationTime(hour, minute)

    suspend fun setFrequency(frequency: NotificationFrequency) =
        repository.updateNotificationFrequency(frequency)
}

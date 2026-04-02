package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class UpdateUserPreferencesUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend fun setCategories(categoryIds: Set<String>) =
        repository.updateSelectedCategories(categoryIds)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        repository.updateNotificationsEnabled(enabled)

    suspend fun setNotificationTimeRange(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    ) = repository.updateNotificationTimeRange(startHour, startMinute, endHour, endMinute)

    suspend fun setFrequency(frequency: NotificationFrequency) =
        repository.updateNotificationFrequency(frequency)

    suspend fun setWidgetSize(size: WidgetSize) =
        repository.updateWidgetSize(size)

    suspend fun setWidgetUpdateTimesPerDay(times: Int) =
        repository.updateWidgetUpdateTimesPerDay(times)
}

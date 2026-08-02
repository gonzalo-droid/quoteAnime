package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

sealed interface CreateHabitResult {
    data class Success(val habit: Habit) : CreateHabitResult
    data class LimitReached(val max: Int) : CreateHabitResult
    data object BlankTitle : CreateHabitResult
    data object InvalidDateRange : CreateHabitResult
}

class CreateHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val premiumGate: PremiumGate,
    private val observePremiumStatus: ObservePremiumStatusUseCase
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        iconKey: String,
        colorIndex: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        reminderTime: LocalTime?,
        reminderDays: Set<DayOfWeek>,
        templateId: String?,
        coverAnimeSlug: String? = null
    ): CreateHabitResult {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return CreateHabitResult.BlankTitle
        if (endDate != null && endDate.isBefore(startDate)) return CreateHabitResult.InvalidDateRange

        val max = premiumGate.maxActiveHabits(observePremiumStatus().first())
        if (repository.countActiveHabits() >= max) return CreateHabitResult.LimitReached(max)

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            iconKey = iconKey,
            colorIndex = colorIndex,
            startDate = startDate,
            endDate = endDate,
            reminderTime = reminderTime,
            reminderDays = if (reminderTime == null) emptySet() else reminderDays,
            templateId = templateId,
            coverAnimeSlug = coverAnimeSlug,
            isArchived = false,
            createdAt = System.currentTimeMillis()
        )
        repository.saveHabit(habit)
        return CreateHabitResult.Success(habit)
    }
}

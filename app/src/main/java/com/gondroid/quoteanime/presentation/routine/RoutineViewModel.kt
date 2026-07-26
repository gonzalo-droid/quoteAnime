package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val getActiveHabits: GetActiveHabitsUseCase,
    private val getGlobalStreak: GetGlobalStreakUseCase,
    private val toggleHabitCompletion: ToggleHabitCompletionUseCase,
    private val archiveHabit: ArchiveHabitUseCase,
    private val reminderScheduler: HabitReminderScheduler,
    private val premiumGate: PremiumGate,
    /** Injected so tests can pin "today" instead of depending on the device clock. */
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState(maxHabits = premiumGate.maxActiveHabits))
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private fun today(): LocalDate = LocalDate.now(clock)

    init {
        observeRoutine()
    }

    private fun observeRoutine() {
        val today = today()
        viewModelScope.launch {
            combine(
                getActiveHabits(today),
                getGlobalStreak(today)
            ) { habits, streak -> habits to streak }
                .collect { (habits, streak) ->
                    _uiState.update {
                        it.copy(
                            habits = habits,
                            globalStreak = streak,
                            isLoading = false,
                            maxHabits = premiumGate.maxActiveHabits
                        )
                    }
                }
        }
    }

    fun onToggleDay(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            when (toggleHabitCompletion(habitId, date, today())) {
                is ToggleCompletionResult.Success -> Unit
                ToggleCompletionResult.FutureDate ->
                    _uiState.update { it.copy(message = RoutineMessage.FutureDayNotAllowed) }
                ToggleCompletionResult.OutsideHabitRange ->
                    _uiState.update { it.copy(message = RoutineMessage.OutsideHabitRange) }
                ToggleCompletionResult.HabitNotFound -> Unit
            }
        }
    }

    fun onArchiveHabit(habitId: String) {
        viewModelScope.launch {
            archiveHabit(habitId)
            reminderScheduler.cancel(habitId)
        }
    }

    /** Consumed by the UI after showing the snackbar. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}

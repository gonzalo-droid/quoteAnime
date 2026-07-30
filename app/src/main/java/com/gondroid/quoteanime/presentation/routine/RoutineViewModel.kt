package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetArchivedHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.IsRoutineIntroSeenUseCase
import com.gondroid.quoteanime.domain.usecase.SetRoutineIntroSeenUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.domain.usecase.UnarchiveHabitUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val getActiveHabits: GetActiveHabitsUseCase,
    private val getArchivedHabits: GetArchivedHabitsUseCase,
    private val getGlobalStreak: GetGlobalStreakUseCase,
    private val toggleHabitCompletion: ToggleHabitCompletionUseCase,
    private val archiveHabit: ArchiveHabitUseCase,
    private val unarchiveHabit: UnarchiveHabitUseCase,
    private val isRoutineIntroSeen: IsRoutineIntroSeenUseCase,
    private val setRoutineIntroSeen: SetRoutineIntroSeenUseCase,
    private val reminderScheduler: HabitReminderScheduler,
    private val premiumGate: PremiumGate,
    private val analytics: RoutineAnalytics,
    /** Direct repository + streak calculation access is only used for the pre/post streak
     *  comparison in [trackStreakChange] below — everything else in this ViewModel goes
     *  through use cases, matching how HabitEditorViewModel already injects the repository
     *  directly for the same kind of one-off read. */
    private val habitRepository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase,
    /** Injected so tests can pin "today" instead of depending on the device clock. */
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState(maxHabits = premiumGate.maxActiveHabits))
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(RoutineFilter.ACTIVE)

    private fun today(): LocalDate = LocalDate.now(clock)

    init {
        analytics.trackTabOpened()
        observeRoutine()
        observeIntro()
    }

    private fun observeRoutine() {
        val today = today()
        viewModelScope.launch {
            // Active habits are always observed (regardless of the selected filter) so
            // canAddHabit stays correct even while browsing the archived tab.
            combine(
                getActiveHabits(today),
                getArchivedHabits(today),
                getGlobalStreak(today),
                _filter
            ) { active, archived, streak, filter ->
                RoutineSnapshot(active, archived, streak, filter)
            }.collect { snapshot ->
                _uiState.update {
                    it.copy(
                        habits = if (snapshot.filter == RoutineFilter.ACTIVE) snapshot.active else snapshot.archived,
                        activeCount = snapshot.active.size,
                        globalStreak = snapshot.streak,
                        isLoading = false,
                        maxHabits = premiumGate.maxActiveHabits,
                        filter = snapshot.filter,
                        today = today
                    )
                }
            }
        }
    }

    fun onFilterChanged(filter: RoutineFilter) {
        _filter.value = filter
    }

    /** Resolves "today" fresh at the moment of the tap, unlike the cached RoutineUiState.today
     *  used for display — this is what prevents the mark-today action from silently writing
     *  to a stale day if the screen survives a midnight rollover. */
    fun onToggleToday(habitId: String) = onToggleDay(habitId, today())

    fun onToggleDay(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            val previousStreak = _uiState.value.habits
                .find { it.habit.id == habitId }
                ?.streak?.current ?: 0

            val result = toggleHabitCompletion(habitId, date, today())
            when (result) {
                is ToggleCompletionResult.Success -> {
                    if (result.completed) {
                        analytics.trackHabitCompleted(
                            habitId = habitId,
                            isRetroactive = date != today(),
                            source = RoutineAnalytics.SOURCE_APP
                        )
                    }
                    trackStreakChange(habitId, previousStreak)
                }
                ToggleCompletionResult.FutureDate ->
                    _uiState.update { it.copy(message = RoutineMessage.FutureDayNotAllowed) }
                ToggleCompletionResult.OutsideHabitRange ->
                    _uiState.update { it.copy(message = RoutineMessage.OutsideHabitRange) }
                ToggleCompletionResult.HabitNotFound -> Unit
            }
        }
    }

    /**
     * Compares the habit's streak right before and after this toggle, computed directly
     * from the repository rather than waiting on the next `getActiveHabits` emission so the
     * comparison can't race the reactive state update.
     *
     * This only catches a break caused by unmarking a day within this same action. A streak
     * silently dropping to 0 because a day was missed entirely (no toggle at all) would need
     * to observe the calendar rolling over past a habit's missed day — this toggle-driven
     * ViewModel has no natural hook for that; it would need a scheduled check, which is out
     * of scope for this fix.
     */
    private suspend fun trackStreakChange(habitId: String, previousStreak: Int) {
        val dates = habitRepository.getCompletions(habitId).first()
        val newStreak = calculateStreak(dates, today())
        when {
            newStreak.current in STREAK_MILESTONES && newStreak.current > previousStreak ->
                analytics.trackStreakMilestone(newStreak.current)
            previousStreak > 0 && newStreak.current == 0 ->
                analytics.trackStreakBroken(previousStreak)
        }
    }

    fun onArchiveHabit(habitId: String) {
        viewModelScope.launch {
            val habit = _uiState.value.habits.find { it.habit.id == habitId }?.habit
            archiveHabit(habitId)
            reminderScheduler.cancel(habitId)
            if (habit != null) {
                val daysActive = (clock.millis() - habit.createdAt) / MILLIS_PER_DAY
                analytics.trackHabitArchived(daysActive)
            }
        }
    }

    fun onUnarchiveHabit(habitId: String) {
        viewModelScope.launch { unarchiveHabit(habitId) }
    }

    /** Consumed by the UI after showing the snackbar. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun observeIntro() {
        viewModelScope.launch {
            isRoutineIntroSeen().collect { seen ->
                _uiState.update { it.copy(showIntro = !seen) }
            }
        }
    }

    fun onIntroDismissed() {
        viewModelScope.launch {
            setRoutineIntroSeen()
            _uiState.update { it.copy(showIntro = false) }
        }
    }

    private companion object {
        val STREAK_MILESTONES = setOf(7, 21, 50, 100)
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}

private data class RoutineSnapshot(
    val active: List<HabitWithProgress>,
    val archived: List<HabitWithProgress>,
    val streak: StreakState,
    val filter: RoutineFilter
)

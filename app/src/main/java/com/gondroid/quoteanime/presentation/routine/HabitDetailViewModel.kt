package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCase
import com.gondroid.quoteanime.domain.usecase.DeleteHabitUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.domain.usecase.UnarchiveHabitUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import com.gondroid.quoteanime.notification.RoutineWidgetScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository,
    private val toggleHabitCompletion: ToggleHabitCompletionUseCase,
    private val archiveHabit: ArchiveHabitUseCase,
    private val unarchiveHabit: UnarchiveHabitUseCase,
    private val deleteHabit: DeleteHabitUseCase,
    private val calculateStreak: CalculateStreakUseCase,
    private val reminderScheduler: HabitReminderScheduler,
    private val routineWidgetScheduler: RoutineWidgetScheduler,
    private val analytics: RoutineAnalytics,
    private val clock: Clock
) : ViewModel() {

    private val habitId: String = requireNotNull(savedStateHandle["habitId"])

    private fun today(): LocalDate = LocalDate.now(clock)

    private val _uiState = MutableStateFlow(
        HabitDetailUiState(visibleMonth = YearMonth.from(today()), today = today())
    )
    val uiState: StateFlow<HabitDetailUiState> = _uiState.asStateFlow()

    init {
        analytics.trackHabitDetailOpened()
        loadHabit()
        observeCompletions()
    }

    private fun loadHabit() {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId)
            _uiState.update { it.copy(habit = habit, isLoading = false) }
        }
    }

    private fun observeCompletions() {
        viewModelScope.launch {
            repository.getCompletions(habitId).collect { dates ->
                _uiState.update {
                    it.copy(completions = dates.toSet(), streak = calculateStreak(dates, today()))
                }
            }
        }
    }

    /** Shared by the big heatmap and the month calendar — both read/write the same completions. */
    fun onDayClick(date: LocalDate) {
        viewModelScope.launch {
            when (val result = toggleHabitCompletion(habitId, date, today())) {
                is ToggleCompletionResult.Success -> {
                    _uiState.update { it.copy(selectedDate = date) }
                    if (result.completed) {
                        analytics.trackHabitCompleted(
                            habitId = habitId,
                            isRetroactive = date != today(),
                            source = RoutineAnalytics.SOURCE_APP
                        )
                    }
                    routineWidgetScheduler.triggerImmediateUpdate()
                }
                ToggleCompletionResult.FutureDate ->
                    _uiState.update { it.copy(message = HabitDetailMessage.FutureDayNotAllowed) }
                ToggleCompletionResult.OutsideHabitRange ->
                    _uiState.update { it.copy(message = HabitDetailMessage.OutsideHabitRange) }
                ToggleCompletionResult.HabitNotFound -> Unit
            }
        }
    }

    fun onMonthChanged(delta: Long) =
        _uiState.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(delta)) }

    /** Consumed by the UI after showing the snackbar. */
    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onArchive() {
        val habitSnapshot = _uiState.value.habit ?: return
        viewModelScope.launch {
            archiveHabit(habitId)
            reminderScheduler.cancel(habitId)
            val daysActive = (clock.millis() - habitSnapshot.createdAt) / MILLIS_PER_DAY
            analytics.trackHabitArchived(daysActive)
            _uiState.update { it.copy(isArchived = true) }
            routineWidgetScheduler.triggerImmediateUpdate()
        }
    }

    /** Restores an archived habit in place — the screen stays open, its actions just
     *  switch back to the active set (Archive instead of Restore). */
    fun onUnarchive() {
        val habitSnapshot = _uiState.value.habit ?: return
        viewModelScope.launch {
            unarchiveHabit(habitId)
            val restored = habitSnapshot.copy(isArchived = false)
            reminderScheduler.schedule(restored)
            _uiState.update { it.copy(habit = restored) }
            routineWidgetScheduler.triggerImmediateUpdate()
        }
    }

    /** Permanent, unlike archiving — the UI must confirm before calling this. */
    fun onDelete() {
        viewModelScope.launch {
            deleteHabit(habitId)
            reminderScheduler.cancel(habitId)
            _uiState.update { it.copy(isDeleted = true) }
            routineWidgetScheduler.triggerImmediateUpdate()
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
}

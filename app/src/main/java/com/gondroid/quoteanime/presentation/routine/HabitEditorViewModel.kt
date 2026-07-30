package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateHabitResult
import com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitTemplates: GetHabitTemplatesUseCase,
    private val createHabit: CreateHabitUseCase,
    private val updateHabit: UpdateHabitUseCase,
    private val repository: HabitRepository,
    private val reminderScheduler: HabitReminderScheduler,
    private val analytics: RoutineAnalytics,
    private val quoteRemoteDataSource: QuoteRemoteDataSource,
    private val clock: Clock
) : ViewModel() {

    private val editedHabitId: String? = savedStateHandle["habitId"]

    private val _uiState = MutableStateFlow(
        HabitEditorUiState(habitId = editedHabitId, startDate = LocalDate.now(clock))
    )
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    /** Anime slug -> image URLs, fetched once; empty until [loadAnimeImages] resolves. */
    private var animeImages: Map<String, List<String>> = emptyMap()

    init {
        loadTemplates()
        loadAnimeImages()
        editedHabitId?.let(::loadHabit)
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            getHabitTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    /** Re-resolves the current selection's preview once images arrive, covering the race
     *  with [loadHabit]/[onTemplateSelected] running before this fetch completes. */
    private fun loadAnimeImages() {
        viewModelScope.launch {
            animeImages = runCatching { quoteRemoteDataSource.getAnimeImages() }.getOrDefault(emptyMap())
            _uiState.update { it.copy(themedBackgroundUrl = resolveThemedBackground(it.coverAnimeSlug)) }
        }
    }

    private fun resolveThemedBackground(slug: String?): String? =
        slug?.let { animeImages[it]?.firstOrNull() }

    private fun loadHabit(habitId: String) {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            _uiState.update {
                it.copy(
                    title = habit.title,
                    description = habit.description.orEmpty(),
                    iconKey = habit.iconKey,
                    templateId = habit.templateId,
                    colorIndex = habit.colorIndex,
                    startDate = habit.startDate,
                    endDate = habit.endDate,
                    reminderEnabled = habit.reminderTime != null,
                    reminderTime = habit.reminderTime ?: it.reminderTime,
                    reminderDays = habit.reminderDays.ifEmpty { it.reminderDays },
                    coverAnimeSlug = habit.coverAnimeSlug,
                    themedBackgroundUrl = resolveThemedBackground(habit.coverAnimeSlug)
                )
            }
        }
    }

    fun onTitleChanged(title: String) =
        _uiState.update { it.copy(title = title, error = null) }

    fun onDescriptionChanged(description: String) =
        _uiState.update { it.copy(description = description) }

    /**
     * [resolvedTitle] is the already-localized display text for [template] (resolved in
     * composable scope by the caller) so the habit persists with legible text instead of
     * the raw "template_xxx" string-resource key.
     */
    fun onTemplateSelected(template: HabitTemplate, resolvedTitle: String) = _uiState.update {
        it.copy(
            title = resolvedTitle,
            iconKey = template.iconKey,
            templateId = template.id,
            colorIndex = template.themeColorIndex ?: it.colorIndex,
            coverAnimeSlug = template.themeAnimeSlug,
            themedBackgroundUrl = resolveThemedBackground(template.themeAnimeSlug),
            error = null
        )
    }

    fun onColorSelected(colorIndex: Int) = _uiState.update { it.copy(colorIndex = colorIndex) }

    fun onIconSelected(iconKey: String) = _uiState.update { it.copy(iconKey = iconKey) }

    fun onStartDateChanged(date: LocalDate) =
        _uiState.update { it.copy(startDate = date, error = null) }

    fun onEndDateChanged(date: LocalDate?) =
        _uiState.update { it.copy(endDate = date, error = null) }

    fun onReminderToggled(enabled: Boolean) =
        _uiState.update { it.copy(reminderEnabled = enabled) }

    fun onReminderTimeChanged(time: LocalTime) =
        _uiState.update { it.copy(reminderTime = time) }

    fun onReminderDayToggled(day: DayOfWeek) = _uiState.update { state ->
        val days = if (day in state.reminderDays) state.reminderDays - day else state.reminderDays + day
        state.copy(reminderDays = days)
    }

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.isEditing) saveExisting(state) else saveNew(state)
        }
    }

    private suspend fun saveNew(state: HabitEditorUiState) {
        val result = createHabit(
            title = state.title,
            description = state.description,
            iconKey = state.iconKey,
            colorIndex = state.colorIndex,
            startDate = state.startDate,
            endDate = state.endDate,
            reminderTime = if (state.reminderEnabled) state.reminderTime else null,
            reminderDays = if (state.reminderEnabled) state.reminderDays else emptySet(),
            templateId = state.templateId,
            coverAnimeSlug = state.coverAnimeSlug
        )
        when (result) {
            is CreateHabitResult.Success -> {
                reminderScheduler.schedule(result.habit)
                analytics.trackHabitCreated(
                    templateId = state.templateId,
                    isCustom = state.templateId == null,
                    hasReminder = state.reminderEnabled,
                    hasEndDate = state.endDate != null
                )
                _uiState.update { it.copy(isSaved = true) }
            }
            is CreateHabitResult.LimitReached ->
                _uiState.update { it.copy(error = HabitEditorError.LimitReached(result.max)) }
            CreateHabitResult.BlankTitle ->
                _uiState.update { it.copy(error = HabitEditorError.BlankTitle) }
            CreateHabitResult.InvalidDateRange ->
                _uiState.update { it.copy(error = HabitEditorError.InvalidDateRange) }
        }
    }

    private suspend fun saveExisting(state: HabitEditorUiState) {
        val existing = repository.getHabit(state.habitId!!) ?: return
        val edited = existing.copy(
            title = state.title,
            description = state.description.trim().takeIf { it.isNotEmpty() },
            iconKey = state.iconKey,
            colorIndex = state.colorIndex,
            startDate = state.startDate,
            endDate = state.endDate,
            reminderTime = if (state.reminderEnabled) state.reminderTime else null,
            reminderDays = if (state.reminderEnabled) state.reminderDays else emptySet(),
            templateId = state.templateId,
            coverAnimeSlug = state.coverAnimeSlug
        )
        when (updateHabit(edited)) {
            is UpdateHabitResult.Success -> {
                reminderScheduler.schedule(edited)
                _uiState.update { it.copy(isSaved = true) }
            }
            UpdateHabitResult.BlankTitle ->
                _uiState.update { it.copy(error = HabitEditorError.BlankTitle) }
            UpdateHabitResult.InvalidDateRange ->
                _uiState.update { it.copy(error = HabitEditorError.InvalidDateRange) }
            UpdateHabitResult.HabitNotFound -> Unit
        }
    }
}

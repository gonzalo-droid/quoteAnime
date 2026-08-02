package com.gondroid.quoteanime.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.SetOnboardingCompletedUseCase
import com.gondroid.quoteanime.domain.usecase.SetRoutineIntroSeenUseCase
import com.gondroid.quoteanime.presentation.routine.HabitEditorError
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
class OnboardingViewModel @Inject constructor(
    private val setOnboardingCompleted: SetOnboardingCompletedUseCase,
    private val setRoutineIntroSeen: SetRoutineIntroSeenUseCase,
    private val getHabitTemplates: GetHabitTemplatesUseCase,
    private val createHabit: CreateHabitUseCase,
    private val observePremiumStatus: ObservePremiumStatusUseCase,
    private val analytics: RoutineAnalytics,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeTemplatesAndPremium()
    }

    /**
     * Combined (not two separate collectors) so the auto-select below always sees the two
     * values together — reading a separately-collected `state.isPremium` here raced the
     * templates emission and could still resolve to the pre-load default (false) even for
     * an already-premium user, since flowOf(...) fires synchronously the moment it's collected.
     */
    private fun observeTemplatesAndPremium() {
        viewModelScope.launch {
            combine(getHabitTemplates(), observePremiumStatus()) { templates, isPremium ->
                templates to isPremium
            }.collect { (templates, isPremium) ->
                _uiState.update { state ->
                    // Mirrors the habit editor: a free-tier user always starts from a
                    // selectable (non-locked) suggestion instead of a blank/disabled Create
                    // button, and never has their pick silently swapped once made.
                    val selected = state.selectedTemplateId?.let { id -> templates.find { it.id == id } }
                        ?: templates.firstOrNull { !it.isPremiumOnly || isPremium }
                    state.copy(templates = templates, isPremium = isPremium, selectedTemplateId = selected?.id)
                }
            }
        }
    }

    fun onTemplateSelected(template: HabitTemplate) {
        _uiState.update { it.copy(selectedTemplateId = template.id, error = null) }
    }

    fun onOnboardingFinished(onDone: () -> Unit) {
        viewModelScope.launch { finishOnboarding(onDone) }
    }

    /**
     * [resolvedTitle] is the already-localized display text for the selected template
     * (resolved in composable scope by the caller) so the habit persists with legible
     * text instead of the raw "template_xxx" string-resource key.
     */
    fun onCreateHabit(resolvedTitle: String, onFinished: () -> Unit) {
        val state = _uiState.value
        val template = state.templates.find { it.id == state.selectedTemplateId } ?: return
        viewModelScope.launch {
            val result = createHabit(
                title = resolvedTitle,
                iconKey = template.iconKey,
                colorIndex = 0,
                startDate = LocalDate.now(clock),
                endDate = null,
                reminderTime = null,
                reminderDays = emptySet(),
                templateId = template.id
            )
            when (result) {
                is CreateHabitResult.Success -> {
                    analytics.trackHabitCreated(
                        templateId = template.id,
                        isCustom = false,
                        hasReminder = false,
                        hasEndDate = false
                    )
                    finishOnboarding(onFinished)
                }
                // Surfaced instead of silently doing nothing — the sheet used to just sit
                // there with a Create button that appeared to do nothing on failure.
                is CreateHabitResult.LimitReached ->
                    _uiState.update { it.copy(error = HabitEditorError.LimitReached(result.max)) }
                CreateHabitResult.BlankTitle ->
                    _uiState.update { it.copy(error = HabitEditorError.BlankTitle) }
                CreateHabitResult.InvalidDateRange ->
                    _uiState.update { it.copy(error = HabitEditorError.InvalidDateRange) }
            }
        }
    }

    private suspend fun finishOnboarding(onDone: () -> Unit) {
        setOnboardingCompleted()
        // Either path through onboarding (picking a habit or tapping Skip) already frames
        // the new routine feature, so the standalone intro dialog would be redundant.
        setRoutineIntroSeen()
        onDone()
    }
}

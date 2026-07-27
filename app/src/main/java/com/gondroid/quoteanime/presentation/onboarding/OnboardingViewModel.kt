package com.gondroid.quoteanime.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.analytics.RoutineAnalytics
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.SetOnboardingCompletedUseCase
import com.gondroid.quoteanime.domain.usecase.SetRoutineIntroSeenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val analytics: RoutineAnalytics,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            getHabitTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    fun onTemplateSelected(template: HabitTemplate) {
        _uiState.update { it.copy(selectedTemplateId = template.id) }
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
                is CreateHabitResult.Success -> analytics.trackHabitCreated(
                    templateId = template.id,
                    isCustom = false,
                    hasReminder = false,
                    hasEndDate = false
                )
                is CreateHabitResult.LimitReached -> Unit
                CreateHabitResult.BlankTitle -> Unit
                CreateHabitResult.InvalidDateRange -> Unit
            }
            finishOnboarding(onFinished)
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

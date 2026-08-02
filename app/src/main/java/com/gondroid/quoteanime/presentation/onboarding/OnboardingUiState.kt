package com.gondroid.quoteanime.presentation.onboarding

import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.presentation.routine.HabitEditorError

data class OnboardingUiState(
    val templates: List<HabitTemplate> = emptyList(),
    val selectedTemplateId: String? = null,
    val isPremium: Boolean = false,
    val error: HabitEditorError? = null
)

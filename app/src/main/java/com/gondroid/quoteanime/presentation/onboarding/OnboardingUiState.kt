package com.gondroid.quoteanime.presentation.onboarding

import com.gondroid.quoteanime.domain.model.HabitTemplate

data class OnboardingUiState(
    val templates: List<HabitTemplate> = emptyList(),
    val selectedTemplateId: String? = null
)

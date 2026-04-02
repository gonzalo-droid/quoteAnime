package com.gondroid.quoteanime.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.SetOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setOnboardingCompleted: SetOnboardingCompletedUseCase
) : ViewModel() {

    fun onOnboardingFinished(onDone: () -> Unit) {
        viewModelScope.launch {
            setOnboardingCompleted()
            onDone()
        }
    }
}

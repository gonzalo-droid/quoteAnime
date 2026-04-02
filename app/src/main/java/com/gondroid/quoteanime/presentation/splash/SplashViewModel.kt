package com.gondroid.quoteanime.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.GetOnboardingCompletedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination { Home, Onboarding }

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getOnboardingCompleted: GetOnboardingCompletedUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(2000)
            val completed = getOnboardingCompleted().first()
            _destination.value = if (completed) SplashDestination.Home else SplashDestination.Onboarding
        }
    }
}

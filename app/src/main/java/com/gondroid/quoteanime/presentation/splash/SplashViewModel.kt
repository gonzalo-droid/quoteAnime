package com.gondroid.quoteanime.presentation.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.usecase.GetOnboardingCompletedUseCase
import com.gondroid.quoteanime.presentation.navigation.Screen
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
    savedStateHandle: SavedStateHandle,
    private val getOnboardingCompleted: GetOnboardingCompletedUseCase
) : ViewModel() {

    companion object {
        const val INTRO_DURATION_MS = 2000L
    }

    /** A widget or reminder tap is waiting (see DeepLinkRouter): the user asked for a
     *  destination, so the branded intro is skipped — the onboarding check is not. */
    private val skipIntro: Boolean = savedStateHandle[Screen.Splash.ARG_SKIP_INTRO] ?: false

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            if (!skipIntro) delay(INTRO_DURATION_MS)
            val completed = getOnboardingCompleted().first()
            _destination.value = if (completed) SplashDestination.Home else SplashDestination.Onboarding
        }
    }
}

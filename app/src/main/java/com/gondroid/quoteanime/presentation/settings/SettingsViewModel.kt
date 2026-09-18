package com.gondroid.quoteanime.presentation.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.usecase.GetAnimesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.ObservePremiumStatusUseCase
import com.gondroid.quoteanime.domain.usecase.ReconcileAnimeSelectionUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationScheduler
import com.gondroid.quoteanime.notification.WidgetScheduler
import com.gondroid.quoteanime.worker.QuoteNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAnimes: GetAnimesUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val updatePreferences: UpdateUserPreferencesUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val widgetScheduler: WidgetScheduler,
    private val observePremiumStatus: ObservePremiumStatusUseCase,
    private val reconcileAnimeSelection: ReconcileAnimeSelectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // The anime list must not hold the whole screen hostage: it comes from the Realtime
        // Database, whose listener never answers offline without a cache. Until it does (or if it
        // fails) the rest of Settings is usable and only the anime section shows its own state.
        val animes = getAnimes()
            .map<List<String>, List<String>?> { it }
            .onStart { emit(null) }
            // A failed read shows the section's empty state; it must not cancel the combine
            // below and take the notification and widget settings down with it.
            .catch { emit(emptyList()) }

        viewModelScope.launch {
            combine(animes, getUserPreferences(), observePremiumStatus()) { animes, prefs, isPremium ->
                // Drops saved values that name no anime (emotions saved by the build that listed
                // them here) and saves the cleaned selection. Untouched until the list arrives.
                val selected = reconcileAnimeSelection(prefs.selectedCategoryIds, animes.orEmpty())
                _uiState.value.copy(
                    animes                  = animes.orEmpty(),
                    animesLoading           = animes == null,
                    selectedAnimes          = selected,
                    notificationsEnabled    = prefs.notificationsEnabled,
                    notificationStartHour   = prefs.notificationStartHour,
                    notificationStartMinute = prefs.notificationStartMinute,
                    notificationEndHour     = prefs.notificationEndHour,
                    notificationEndMinute   = prefs.notificationEndMinute,
                    notificationFrequency   = prefs.notificationFrequency,
                    widgetSize              = prefs.widgetSize,
                    widgetUpdateTimesPerDay = prefs.widgetUpdateTimesPerDay,
                    isLoading               = false,
                    isPremium               = isPremium
                )
            }.collect { _uiState.value = it }
        }
    }

    // ── Animes ────────────────────────────────────────────────────────────────
    // Both update the state right away instead of waiting for DataStore to echo the write: the
    // chips react on the same frame, and a second quick tap toggles on top of the first.
    fun onAnimeToggled(anime: String) {
        val newSelection = _uiState.value.selectedAnimes.toggle(anime)
        _uiState.update { it.copy(selectedAnimes = newSelection) }
        viewModelScope.launch {
            updatePreferences.setSelectedAnimes(newSelection)
            rescheduleNotificationIfEnabled(_uiState.value.copy(selectedAnimes = newSelection))
        }
    }

    fun onSelectAllAnimes() {
        _uiState.update { it.copy(selectedAnimes = emptySet()) }
        viewModelScope.launch {
            updatePreferences.setSelectedAnimes(emptySet())
            rescheduleNotificationIfEnabled(_uiState.value.copy(selectedAnimes = emptySet()))
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    fun onNotificationsEnabled() {
        viewModelScope.launch {
            updatePreferences.setNotificationsEnabled(true)
            rescheduleNotificationIfEnabled(_uiState.value.copy(notificationsEnabled = true))
        }
    }

    fun onNotificationsDisabled() {
        viewModelScope.launch {
            updatePreferences.setNotificationsEnabled(false)
            notificationScheduler.cancel()
        }
    }

    fun onTimeRangeChanged(
        startHour: Int, startMinute: Int,
        endHour: Int, endMinute: Int
    ) {
        viewModelScope.launch {
            updatePreferences.setNotificationTimeRange(startHour, startMinute, endHour, endMinute)
            rescheduleNotificationIfEnabled(
                _uiState.value.copy(
                    notificationStartHour   = startHour,
                    notificationStartMinute = startMinute,
                    notificationEndHour     = endHour,
                    notificationEndMinute   = endMinute
                )
            )
        }
    }

    fun onFrequencyChanged(timesPerDay: Int) {
        viewModelScope.launch {
            updatePreferences.setFrequency(timesPerDay)
            rescheduleNotificationIfEnabled(_uiState.value.copy(notificationFrequency = timesPerDay))
        }
    }

    fun onPermissionDeniedPermanently() {
        _uiState.update { it.copy(permissionDeniedPermanently = true) }
    }

    // ── Widget ────────────────────────────────────────────────────────────────
    fun onWidgetSizeChanged(size: WidgetSize) {
        viewModelScope.launch {
            updatePreferences.setWidgetSize(size)
            // Apply new size to widget instances immediately
            widgetScheduler.triggerImmediateUpdate()
        }
    }

    fun onWidgetUpdateTimesChanged(times: Int) {
        viewModelScope.launch {
            updatePreferences.setWidgetUpdateTimesPerDay(times)
            widgetScheduler.schedule(times)
        }
    }

    // ── Debug ─────────────────────────────────────────────────────────────────
    fun onTestNotification() {
        Log.d("testNotification", "init")

        WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<QuoteNotificationWorker>().build()
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun rescheduleNotificationIfEnabled(state: SettingsUiState) {
        if (state.notificationsEnabled) notificationScheduler.schedule(state.toUserPreferences())
    }

    private fun Set<String>.toggle(id: String): Set<String> =
        if (id in this) this - id else this + id
}

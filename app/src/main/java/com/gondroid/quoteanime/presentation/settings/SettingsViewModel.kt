package com.gondroid.quoteanime.presentation.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.WidgetSize
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationScheduler
import com.gondroid.quoteanime.notification.WidgetScheduler
import com.gondroid.quoteanime.worker.QuoteNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCategories: GetCategoriesUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val updatePreferences: UpdateUserPreferencesUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val widgetScheduler: WidgetScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(getCategories(), getUserPreferences()) { categories, prefs ->
                _uiState.value.copy(
                    categories              = categories,
                    selectedCategoryIds     = prefs.selectedCategoryIds,
                    notificationsEnabled    = prefs.notificationsEnabled,
                    notificationStartHour   = prefs.notificationStartHour,
                    notificationStartMinute = prefs.notificationStartMinute,
                    notificationEndHour     = prefs.notificationEndHour,
                    notificationEndMinute   = prefs.notificationEndMinute,
                    notificationFrequency   = prefs.notificationFrequency,
                    widgetSize              = prefs.widgetSize,
                    widgetUpdateTimesPerDay = prefs.widgetUpdateTimesPerDay,
                    isLoading               = false
                )
            }.collect { _uiState.value = it }
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────
    fun onCategoryToggled(categoryId: String) {
        viewModelScope.launch {
            val newIds = _uiState.value.selectedCategoryIds.toggle(categoryId)
            updatePreferences.setCategories(newIds)
            rescheduleNotificationIfEnabled(_uiState.value.copy(selectedCategoryIds = newIds))
        }
    }

    fun onSelectAllCategories() {
        viewModelScope.launch {
            updatePreferences.setCategories(emptySet())
            rescheduleNotificationIfEnabled(_uiState.value.copy(selectedCategoryIds = emptySet()))
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

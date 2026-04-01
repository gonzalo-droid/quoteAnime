package com.gondroid.quoteanime.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.usecase.GetCategoriesUseCase
import com.gondroid.quoteanime.domain.usecase.GetUserPreferencesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateUserPreferencesUseCase
import com.gondroid.quoteanime.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val getUserPreferences: GetUserPreferencesUseCase,
    private val updatePreferences: UpdateUserPreferencesUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getCategories(),
                getUserPreferences()
            ) { categories, prefs ->
                _uiState.value.copy(
                    categories = categories,
                    selectedCategoryIds = prefs.selectedCategoryIds,
                    notificationsEnabled = prefs.notificationsEnabled,
                    notificationHour = prefs.notificationHour,
                    notificationMinute = prefs.notificationMinute,
                    notificationFrequency = prefs.notificationFrequency,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onCategoryToggled(categoryId: String) {
        viewModelScope.launch {
            val newIds = _uiState.value.selectedCategoryIds.toggle(categoryId)
            updatePreferences.setCategories(newIds)
            rescheduleIfEnabled(_uiState.value.copy(selectedCategoryIds = newIds))
        }
    }

    fun onSelectAllCategories() {
        viewModelScope.launch {
            updatePreferences.setCategories(emptySet())
            rescheduleIfEnabled(_uiState.value.copy(selectedCategoryIds = emptySet()))
        }
    }

    /** Llamado desde la pantalla tras confirmar el permiso POST_NOTIFICATIONS */
    fun onNotificationsEnabled() {
        viewModelScope.launch {
            updatePreferences.setNotificationsEnabled(true)
            rescheduleIfEnabled(_uiState.value.copy(notificationsEnabled = true))
        }
    }

    fun onNotificationsDisabled() {
        viewModelScope.launch {
            updatePreferences.setNotificationsEnabled(false)
            notificationScheduler.cancel()
        }
    }

    fun onTimeChanged(hour: Int, minute: Int) {
        viewModelScope.launch {
            updatePreferences.setNotificationTime(hour, minute)
            rescheduleIfEnabled(
                _uiState.value.copy(notificationHour = hour, notificationMinute = minute)
            )
        }
    }

    fun onFrequencyChanged(frequency: NotificationFrequency) {
        viewModelScope.launch {
            updatePreferences.setFrequency(frequency)
            rescheduleIfEnabled(_uiState.value.copy(notificationFrequency = frequency))
        }
    }

    fun onPermissionDeniedPermanently() {
        _uiState.update { it.copy(permissionDeniedPermanently = true) }
    }

    private fun rescheduleIfEnabled(state: SettingsUiState) {
        if (state.notificationsEnabled) {
            notificationScheduler.schedule(state.toUserPreferences())
        }
    }

    private fun Set<String>.toggle(id: String): Set<String> =
        if (id in this) this - id else this + id
}

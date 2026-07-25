package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorSheet(
    onDismiss: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("habit_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.habit_editor_edit_title
                    else R.string.habit_editor_new_title
                ),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(stringResource(R.string.habit_editor_templates))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.templates.forEach { template ->
                    FilterChip(
                        selected = state.templateId == template.id,
                        onClick = { viewModel.onTemplateSelected(template) },
                        label = { Text(template.title) }
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.habit_editor_name)) },
                singleLine = true,
                isError = state.error is HabitEditorError.BlankTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_field")
            )

            Text(stringResource(R.string.habit_editor_color))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitPalette.COLORS.forEachIndexed { index, color ->
                    Column(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { viewModel.onColorSelected(index) }
                            .testTag("color_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (state.colorIndex == index) 2.dp else 0.dp,
                                    color = if (state.colorIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {}
                    }
                }
            }

            Text(stringResource(R.string.habit_editor_icon))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitIcons.ALL_KEYS.forEach { key ->
                    Column(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { viewModel.onIconSelected(key) }
                            .testTag("icon_$key"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = HabitIcons.iconFor(key),
                            contentDescription = null,
                            tint = if (state.iconKey == key) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (state.iconKey == key) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_start_date)) },
                supportingContent = { Text(state.startDate.toString()) },
                modifier = Modifier.clickable { showStartPicker = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_has_end_date)) },
                trailingContent = {
                    Switch(
                        checked = state.endDate != null,
                        onCheckedChange = { checked ->
                            if (checked) showEndPicker = true else viewModel.onEndDateChanged(null)
                        }
                    )
                }
            )
            if (state.endDate != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_end_date)) },
                    supportingContent = { Text(state.endDate.toString()) },
                    modifier = Modifier.clickable { showEndPicker = true }
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_reminder)) },
                trailingContent = {
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = viewModel::onReminderToggled,
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }
            )

            if (state.reminderEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_reminder_time)) },
                    supportingContent = { Text(state.reminderTime.toString()) },
                    modifier = Modifier.clickable { showTimePicker = true }
                )
                Text(stringResource(R.string.habit_editor_reminder_days))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.reminderDays,
                            onClick = { viewModel.onReminderDayToggled(day) },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            }
                        )
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = when (error) {
                        HabitEditorError.BlankTitle ->
                            stringResource(R.string.habit_editor_error_blank)
                        HabitEditorError.InvalidDateRange ->
                            stringResource(R.string.habit_editor_error_dates)
                        is HabitEditorError.LimitReached ->
                            stringResource(R.string.habit_editor_error_limit, error.max)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_save_button")
            ) {
                Text(stringResource(R.string.habit_editor_save))
            }
        }
    }

    if (showStartPicker) {
        DatePickerModal(
            initialDate = state.startDate,
            onDateSelected = { viewModel.onStartDateChanged(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerModal(
            initialDate = state.endDate ?: state.startDate,
            onDateSelected = { viewModel.onEndDateChanged(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
    if (showTimePicker) {
        TimePickerModal(
            initialTime = state.reminderTime,
            onTimeSelected = { viewModel.onReminderTimeChanged(it); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(
                        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    )
                }
            }) { Text(stringResource(R.string.habit_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.habit_editor_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
            }) { Text(stringResource(R.string.habit_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.habit_editor_cancel))
            }
        }
    ) {
        TimePicker(state = pickerState)
    }
}

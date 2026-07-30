package com.gondroid.quoteanime.presentation.routine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Matches the app-wide "12/03/2026" display format requested for the editor's date rows. */
private val DATE_DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** 14 palette colors laid out as exactly 2 rows of 7. */
private const val COLOR_COLUMNS = 7

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorSheet(
    onDismiss: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onReminderToggled(true)
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.habit_editor_reminder_permission_denied_message),
                    actionLabel = context.getString(R.string.habit_editor_reminder_permission_denied_action)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            }
        }
    }

    fun requestReminderToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.onReminderToggled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.onReminderToggled(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.onReminderToggled(true)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDismiss()
    }

    // Suggestions are 100% themed, so a new habit always starts from one instead of a
    // blank/generic default — the first suggestion is applied automatically until the
    // user picks a different one or types their own title. Never runs while editing an
    // existing habit, whose own selection (or lack of one) must be left untouched.
    val firstTemplate = state.templates.firstOrNull()
    val firstResolvedTitle = firstTemplate?.let { resolveTemplateTitle(it.title) }
    val firstResolvedDescription = firstTemplate?.let { resolveThemeDescription(it.themeKey) }
    LaunchedEffect(firstTemplate, state.isEditing, state.templateId) {
        if (!state.isEditing && state.templateId == null && firstTemplate != null) {
            viewModel.onTemplateSelected(firstTemplate, firstResolvedTitle.orEmpty(), firstResolvedDescription)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(modifier = Modifier.fillMaxSize()) {
        if (showIconPicker) {
            HabitIconPickerContent(
                selectedKey = state.iconKey,
                onIconSelected = { key -> viewModel.onIconSelected(key); showIconPicker = false },
                onBack = { showIconPicker = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                    // Resolve once per template so both the chip label and the click
                    // handler use the same legible text (not the raw resource key).
                    val resolvedTitle = resolveTemplateTitle(template.title)
                    val resolvedDescription = resolveThemeDescription(template.themeKey)
                    FilterChip(
                        selected = state.templateId == template.id,
                        onClick = { viewModel.onTemplateSelected(template, resolvedTitle, resolvedDescription) },
                        label = { Text(resolvedTitle) }
                    )
                }
            }

            state.themeKey?.let { themeKey ->
                ThemedSuggestionPreview(
                    iconKey = state.iconKey,
                    title = state.title,
                    description = state.description,
                    imageRes = HabitThemeImages.resFor(themeKey),
                    accentColor = HabitPalette.colorAt(state.colorIndex),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("themed_suggestion_preview")
                )
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

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text(stringResource(R.string.habit_editor_description)) },
                placeholder = { Text(stringResource(R.string.habit_editor_description_placeholder)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_description_field")
            )

            Text(stringResource(R.string.habit_editor_color))
            // Two explicit rows of 7 (not a width-dependent FlowRow) so the layout is exactly
            // 7 columns on every screen size, per design — each swatch takes equal weight so
            // all 7 always fit one row instead of wrapping early on narrow phones.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitPalette.COLORS.chunked(COLOR_COLUMNS).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowColors.forEach { color ->
                            val index = HabitPalette.COLORS.indexOf(color)
                            val colorDescription = stringResource(R.string.habit_editor_color_option, index + 1)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { viewModel.onColorSelected(index) }
                                    .semantics { contentDescription = colorDescription }
                                    .testTag("color_$index"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.6f)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (state.colorIndex == index) 2.dp else 0.dp,
                                            color = if (state.colorIndex == index) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // The icon is no longer chosen inline: this row shows the current pick and a
            // chevron that opens the full-screen categorized picker (HabitIconPickerContent).
            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_icon)) },
                supportingContent = { Text(describeIcon(state.iconKey)) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HabitIcons.iconFor(state.iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .clickable { showIconPicker = true }
                    .testTag("habit_icon_row")
            )

            // Grouped in one bordered card — same treatment as the reminder card below it,
            // so the form's lower section reads as a consistent set of grouped controls
            // instead of a mix of loose rows and cards.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_start_date)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Text(
                            text = DATE_DISPLAY_FORMAT.format(state.startDate),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { showStartPicker = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.habit_editor_end_date)) },
                        trailingContent = {
                            Text(
                                text = DATE_DISPLAY_FORMAT.format(state.endDate),
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable { showEndPicker = true }
                    )
                }
            }

            // Grouped in one bordered card instead of loose ListItems, so the switch and its
            // time/days detail read as a single unit rather than unrelated rows in the list.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_reminder)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = { enabled -> requestReminderToggle(enabled) },
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }
                )

                if (state.reminderEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.habit_editor_reminder_time),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = state.reminderTime.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                val selected = day in state.reminderDays
                                DayDot(
                                    label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    fullLabel = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                    selected = selected,
                                    onToggle = { viewModel.onReminderDayToggled(day) },
                                    modifier = Modifier.testTag("reminder_day_${day.name}")
                                )
                            }
                        }
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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

/** Compact circular day toggle used by the reminder card, matching the approved mockup's
 *  "day-dot" design instead of a full-size FilterChip. */
@Composable
private fun DayDot(
    label: String,
    fullLabel: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .toggleable(value = selected, onValueChange = { onToggle() }, role = Role.Checkbox)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .semantics { contentDescription = fullLabel },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Themed-cover card shown once a themed template ([HabitTemplate.themeKey]) is selected. */
@Composable
private fun ThemedSuggestionPreview(
    iconKey: String,
    title: String,
    description: String,
    imageRes: Int?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.25f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = HabitIcons.iconFor(iconKey),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2
                )
            }
        }
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

package com.gondroid.quoteanime.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme



@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWidgetTutorial: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToWebView: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationsEnabled()
        } else {
            viewModel.onPermissionDeniedPermanently()
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.notification_permission_denied_message),
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

    fun requestNotificationToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.onNotificationsDisabled(); return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.onNotificationsEnabled()
            else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onNotificationsEnabled()
        }
    }

    Scaffold(
        topBar = { SettingsTopBar(onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                item { SectionDivider() }

                item {
                    PremiumSettingsRow(
                        isPremium = uiState.isPremium,
                        onClick = onNavigateToPaywall
                    )
                }

                item { SectionDivider() }

                item {
                    SectionHeader(stringResource(R.string.notifications))
                    NotificationSection(
                        uiState = uiState,
                        onToggle = ::requestNotificationToggle,
                        onTimeRangeChanged = viewModel::onTimeRangeChanged,
                        onFrequencyChanged = viewModel::onFrequencyChanged
                    )
                }

                item { SectionDivider() }

                item {
                    SectionHeader(stringResource(R.string.widget))
                    WidgetSection(
                        widgetUpdateTimesPerDay = uiState.widgetUpdateTimesPerDay,
                        onUpdateTimesChanged = viewModel::onWidgetUpdateTimesChanged,
                        onNavigateToTutorial = onNavigateToWidgetTutorial
                    )
                }

                item { SectionDivider() }

                item {
                    SectionHeader(stringResource(R.string.rating))
                    RatingSection()
                }

                item { SectionDivider() }

                // Hidden for now: the Instagram and Facebook accounts are blocked, so the
                // links would dead-end. Restore this item (and its divider) once they're back.
                /*
                item {
                    SectionHeader(stringResource(R.string.follow_us))
                    SocialSection(onNavigateToWebView)
                }

                item { SectionDivider() }
                */

                item {
                    SectionHeader(stringResource(R.string.version))
                    InformationSection(
                        versionName = versionName,
                        onNavigateToWebView = onNavigateToWebView
                    )
                }

                item { SectionDivider() }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.settings),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

internal val listItemColors
    @Composable get() = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background
    )

@Composable
private fun PremiumSettingsRow(isPremium: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_premium_title)) },
        supportingContent = {
            Text(
                if (isPremium) stringResource(R.string.settings_premium_subtitle_active)
                else stringResource(R.string.settings_premium_subtitle_free)
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = listItemColors,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSection(
    uiState: SettingsUiState,
    onToggle: (Boolean) -> Unit,
    onTimeRangeChanged: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onFrequencyChanged: (Int) -> Unit
) {
    // which picker is open: null | "start" | "end"
    var openPicker by remember { mutableStateOf<String?>(null) }

    ListItem(
        headlineContent = {
            Text(stringResource(R.string.notification_toggle_title), color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                stringResource(R.string.notification_toggle_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = uiState.notificationsEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        },
        colors = listItemColors
    )

    if (uiState.notificationsEnabled) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )

        ListItem(
            headlineContent = {
                Text(stringResource(R.string.notification_schedule_title), color = MaterialTheme.colorScheme.onBackground)
            },
            supportingContent = {
                Text(
                    stringResource(R.string.notification_schedule_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            },
            colors = listItemColors
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeRangeChip(
                label = stringResource(R.string.notification_time_from),
                time = formatTo12h(uiState.notificationStartHour, uiState.notificationStartMinute),
                amPm = amPmLabel(uiState.notificationStartHour),
                onClick = { openPicker = "start" },
                modifier = Modifier.weight(1f)
            )
            Text(
                "→",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            TimeRangeChip(
                label = stringResource(R.string.notification_time_to),
                time = formatTo12h(uiState.notificationEndHour, uiState.notificationEndMinute),
                amPm = amPmLabel(uiState.notificationEndHour),
                onClick = { openPicker = "end" },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline
        )

        // Frequency
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.notification_frequency_title), color = MaterialTheme.colorScheme.onBackground)
            },
            supportingContent = {
                Column {
                    Text(
                        pluralStringResource(
                            R.plurals.notification_frequency_per_day,
                            uiState.notificationFrequency,
                            uiState.notificationFrequency
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = uiState.notificationFrequency.toFloat(),
                        onValueChange = { onFrequencyChanged(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.frequency_per_day_short, 1),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.frequency_per_day_short, 10),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = listItemColors
        )
    }

    // Start time picker
    if (openPicker == "start") {
        TimePickerDialog(
            title = stringResource(R.string.time_picker_start_title),
            initialHour = uiState.notificationStartHour,
            initialMinute = uiState.notificationStartMinute,
            onDismiss = { openPicker = null },
            onConfirm = { h, m ->
                onTimeRangeChanged(h, m, uiState.notificationEndHour, uiState.notificationEndMinute)
                openPicker = null
            }
        )
    }

    // End time picker
    if (openPicker == "end") {
        TimePickerDialog(
            title = stringResource(R.string.time_picker_end_title),
            initialHour = uiState.notificationEndHour,
            initialMinute = uiState.notificationEndMinute,
            onDismiss = { openPicker = null },
            onConfirm = { h, m ->
                onTimeRangeChanged(
                    uiState.notificationStartHour,
                    uiState.notificationStartMinute,
                    h,
                    m
                )
                openPicker = null
            }
        )
    }
}

@Composable
private fun TimeRangeChip(
    label: String,
    time: String,
    amPm: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = amPm,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}

private fun formatTo12h(hour: Int, minute: Int): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%02d:%02d".format(h, minute)
}

private fun amPmLabel(hour: Int): String = if (hour < 12) "AM" else "PM"

// ── Widget ────────────────────────────────────────────────────────────────────
@Composable
private fun WidgetSection(
    widgetUpdateTimesPerDay: Int,
    onUpdateTimesChanged: (Int) -> Unit,
    onNavigateToTutorial: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text("Cómo agregar el widget", color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                "Tutorial paso a paso para añadirlo a tu pantalla de inicio",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onNavigateToTutorial),
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text("Tamaño del widget", color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                "Mantén presionado el widget en tu pantalla de inicio y arrastra las esquinas para ajustar el tamaño. El contenido se adapta automáticamente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text("Actualizaciones del widget", color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Column {
                Text(
                    pluralStringResource(
                        R.plurals.widget_update_frequency,
                        widgetUpdateTimesPerDay,
                        widgetUpdateTimesPerDay
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = widgetUpdateTimesPerDay.toFloat(),
                    onValueChange = { onUpdateTimesChanged(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.frequency_per_day_short, 1), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.frequency_per_day_short, 8), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = listItemColors
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text(stringResource(R.string.action_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}


@Preview(name = "Settings — notificaciones OFF", showSystemUi = true)
@Composable
private fun PreviewSettingsOff() {
    QuoteAnimeTheme {
        val uiState = SettingsUiState(isLoading = false, notificationsEnabled = false)
        Scaffold(
            topBar = { SettingsTopBar(onNavigateBack = {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item { SectionDivider() }
                item {
                    SectionHeader("Notificaciones")
                    NotificationSection(uiState = uiState, onToggle = {}, onTimeRangeChanged = { _, _, _, _ -> }, onFrequencyChanged = {})
                }
                item { SectionDivider() }
                item {
                    SectionHeader("Apóyanos")
                    RatingSection()
                }
                item { SectionDivider() }
                item {
                    SectionHeader("Síguenos")
                    SocialSection()
                }
                item { SectionDivider() }
                item { InformationSection(versionName = "1.2.0") }
            }
        }
    }
}

@Preview(name = "Settings — notificaciones ON", showSystemUi = true)
@Composable
private fun PreviewSettingsOn() {
    QuoteAnimeTheme {
        val uiState = SettingsUiState(
            isLoading = false,
            notificationsEnabled = true,
            notificationStartHour = 8,
            notificationStartMinute = 0,
            notificationEndHour = 22,
            notificationEndMinute = 0,
            notificationFrequency = 3
        )
        Scaffold(
            topBar = { SettingsTopBar(onNavigateBack = {}) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item { SectionDivider() }
                item {
                    SectionHeader("Notificaciones")
                    NotificationSection(uiState = uiState, onToggle = {}, onTimeRangeChanged = { _, _, _, _ -> }, onFrequencyChanged = {})
                }
                item { SectionDivider() }
                item {
                    SectionHeader("Calificación")
                    RatingSection()
                }
                item { SectionDivider() }
                item {
                    SectionHeader("Síguenos")
                    SocialSection()
                }
                item { SectionDivider() }
                item {
                    SectionHeader("Información")
                    InformationSection(versionName = "1.2.0")
                }
            }
        }
    }
}

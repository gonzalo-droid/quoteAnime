package com.gondroid.quoteanime.presentation.settings

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.NotificationFrequency
import com.gondroid.quoteanime.domain.model.WidgetSize
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                    message = "Permiso denegado. Actívalo desde Ajustes.",
                    actionLabel = "Ajustes"
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
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {

                /*item {
                    SectionHeader("Categorías")
                    CategorySection(
                        categories        = uiState.categories,
                        selectedIds       = uiState.selectedCategoryIds,
                        allSelected       = uiState.allCategoriesSelected,
                        onCategoryToggled = viewModel::onCategoryToggled,
                        onSelectAll       = viewModel::onSelectAllCategories
                    )
                }*/

                item { SectionDivider() }

                item {
                    SectionHeader("Notificaciones")
                    NotificationSection(
                        uiState = uiState,
                        onToggle = ::requestNotificationToggle,
                        onTimeRangeChanged = viewModel::onTimeRangeChanged,
                        onFrequencyChanged = viewModel::onFrequencyChanged
                    )
                }

                item { SectionDivider() }

                item {
                    SectionHeader("Widget")
                    WidgetSection(
                        widgetSize = uiState.widgetSize,
                        widgetUpdateTimesPerDay = uiState.widgetUpdateTimesPerDay,
                        onSizeChanged = viewModel::onWidgetSizeChanged,
                        onUpdateTimesChanged = viewModel::onWidgetUpdateTimesChanged
                    )
                }

                item { SectionDivider() }

                item {
                    SectionHeader("Debug")
                    TestNotificationButton(onClick = viewModel::onTestNotification)
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Debug ─────────────────────────────────────────────────────────────────────
@Composable
private fun TestNotificationButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Probar notificación ahora")
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
                "Personalización",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
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
private fun SectionHeader(title: String) {
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

private val listItemColors
    @Composable get() = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background
    )

// ── Categorías ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    categories: List<Category>,
    selectedIds: Set<String>,
    allSelected: Boolean,
    onCategoryToggled: (String) -> Unit,
    onSelectAll: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (categories.isEmpty()) {
            Text(
                "No hay categorías disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Sin selección = todas las categorías",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(selected = allSelected, onClick = onSelectAll, label = { Text("Todas") })
                categories.forEach { cat ->
                    FilterChip(
                        selected = cat.id in selectedIds,
                        onClick = { onCategoryToggled(cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSection(
    uiState: SettingsUiState,
    onToggle: (Boolean) -> Unit,
    onTimeRangeChanged: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onFrequencyChanged: (NotificationFrequency) -> Unit
) {
    // which picker is open: null | "start" | "end"
    var openPicker by remember { mutableStateOf<String?>(null) }

    ListItem(
        headlineContent = {
            Text("Activar notificaciones", color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                "Recibe una frase motivacional en tu horario",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = uiState.notificationsEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
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
                Text("Horario permitido", color = MaterialTheme.colorScheme.onBackground)
            },
            supportingContent = {
                Text(
                    "Recibirás notificaciones solo dentro de este rango",
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
                label = "Desde",
                time = "%02d:%02d".format(
                    uiState.notificationStartHour,
                    uiState.notificationStartMinute
                ),
                onClick = { openPicker = "start" },
                modifier = Modifier.weight(1f)
            )
            Text(
                "→",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            TimeRangeChip(
                label = "Hasta",
                time = "%02d:%02d".format(
                    uiState.notificationEndHour,
                    uiState.notificationEndMinute
                ),
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
                Text("Frecuencia", color = MaterialTheme.colorScheme.onBackground)
            },
            supportingContent = {
                Spacer(Modifier.height(8.dp))
            },
            colors = listItemColors
        )
        FrequencySelector(
            selected = uiState.notificationFrequency,
            onSelected = onFrequencyChanged
        )
    }

    // Start time picker
    if (openPicker == "start") {
        TimePickerDialog(
            title = "Hora de inicio",
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
            title = "Hora de fin",
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
    }
}

// ── Widget ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSection(
    widgetSize: WidgetSize,
    widgetUpdateTimesPerDay: Int,
    onSizeChanged: (WidgetSize) -> Unit,
    onUpdateTimesChanged: (Int) -> Unit
) {
    ListItem(
        headlineContent = {
            Text("Tamaño del widget", color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Column {
                Spacer(Modifier.height(8.dp))

                val sizes = WidgetSize.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    sizes.forEachIndexed { index, size ->
                        SegmentedButton(
                            selected = widgetSize == size,
                            onClick = { onSizeChanged(size) },
                            shape = SegmentedButtonDefaults.itemShape(index, sizes.size),
                            label = {
                                Text(
                                    size.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
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
                    "Nueva frase $widgetUpdateTimesPerDay ${if (widgetUpdateTimesPerDay == 1) "vez" else "veces"} al día",
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
                        "1×/día", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "8×/día", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = listItemColors
    )
}

// ── Frequency selector ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    selected: NotificationFrequency,
    onSelected: (NotificationFrequency) -> Unit
) {
    val options = NotificationFrequency.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, frequency ->
            SegmentedButton(
                selected = selected == frequency,
                onClick = { onSelected(frequency) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(frequency.label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

// ── Time picker dialog ────────────────────────────────────────────────────────
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
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

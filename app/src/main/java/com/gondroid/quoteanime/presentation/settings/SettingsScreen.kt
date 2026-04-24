package com.gondroid.quoteanime.presentation.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Category
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme

private const val PRIVACY_POLICY_URL = "https://quote-anime-web.vercel.app/privacy-policy"
private const val TERM_AND_CONDITIONS_URL = "https://quote-anime-web.vercel.app/terms-and-conditions"


@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWidgetTutorial: () -> Unit = {},
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

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

                item {
                    SectionHeader(stringResource(R.string.follow_us))
                    SocialSection()
                }

                item { SectionDivider() }

                item {
                    SectionHeader(stringResource(R.string.version))
                    InformationSection(versionName = versionName)
                }

                item { SectionDivider() }

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
                "Ajustes",
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
    onFrequencyChanged: (Int) -> Unit
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
                label = "Hasta",
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
                Text("Frecuencia", color = MaterialTheme.colorScheme.onBackground)
            },
            supportingContent = {
                Column {
                    Text(
                        "${uiState.notificationFrequency} ${if (uiState.notificationFrequency == 1) "vez" else "veces"} al día",
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
                            "1×/día",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "10×/día",
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


private fun openPlayStore(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "market://details?id=${context.packageName}".toUri()
            )
        )
    }.onFailure {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
            )
        )
    }
}

@Composable
private fun RatingSection() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.rating_app),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.subtitle_rating_section),
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
        modifier = Modifier.clickable {
            if (activity != null) {
                scope.launch {
                    runCatching {
                        val manager = ReviewManagerFactory.create(context)
                        val reviewInfo = manager.requestReview()
                        manager.launchReview(activity, reviewInfo)
                    }.onFailure {
                        openPlayStore(context)
                    }
                }
            } else {
                openPlayStore(context)
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
            Text(
                stringResource(R.string.share_app),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        supportingContent = {
            Text(
                stringResource(R.string.share_app_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { shareApp(context) },
        colors = listItemColors
    )
}

private fun shareApp(context: android.content.Context) {
    val message = context.getString(R.string.share_app_message)
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

// ── Social ───────────────────────────────────────────────────────────────────
@Composable
private fun SocialSection() {
    val context = LocalContext.current

    SocialItem(
        iconRes = R.drawable.ic_instagram,
        name = stringResource(R.string.social_instagram),
        handle = stringResource(R.string.social_instagram_handle),
        iconTint = Color(0xFFE1306C),
        onClick = { openUrl(context, "https://www.instagram.com/animequoteapp/") }
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    SocialItem(
        iconRes = R.drawable.ic_facebook,
        name = stringResource(R.string.social_facebook),
        handle = stringResource(R.string.social_facebook_handle),
        iconTint = Color(0xFF1877F2),
        onClick = { openUrl(context, "https://www.facebook.com/share/1Ay18mtNZh/?mibextid=wwXIfr") }
    )

    /*
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    SocialItem(
        iconRes = R.drawable.ic_tiktok,
        name = stringResource(R.string.social_tiktok),
        handle = stringResource(R.string.social_tiktok_handle),
        iconTint = Color.White,
        onClick = { openUrl(context, "https://www.tiktok.com/@frasesanime") }
    )*/
}

@Composable
private fun SocialItem(
    iconRes: Int,
    name: String,
    handle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = name,
                tint = iconTint
            )
        },
        headlineContent = {
            Text(name, color = MaterialTheme.colorScheme.onBackground)
        },
        supportingContent = {
            Text(
                handle,
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
        modifier = Modifier.clickable(onClick = onClick),
        colors = listItemColors
    )
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


@Composable
private fun InformationSection(versionName: String) {
    val context = LocalContext.current

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.politics_privacy),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { openUrl(context, PRIVACY_POLICY_URL) },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.terms_and_conditions),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable { openUrl(context, TERM_AND_CONDITIONS_URL) },
        colors = listItemColors
    )

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline
    )

    ListItem(
        headlineContent = {
            Text("Versión", color = MaterialTheme.colorScheme.onBackground)
        },
        trailingContent = {
            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = listItemColors
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

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

@Preview(name = "Rating + Share", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewRatingSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader("Calificación")
            RatingSection()
        }
    }
}

@Preview(name = "Síguenos", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewSocialSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader("Síguenos")
            SocialSection()
        }
    }
}

@Preview(name = "Información", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun PreviewInformationSection() {
    QuoteAnimeTheme {
        androidx.compose.foundation.layout.Column {
            SectionHeader("Información")
            InformationSection(versionName = "1.0.4")
        }
    }
}
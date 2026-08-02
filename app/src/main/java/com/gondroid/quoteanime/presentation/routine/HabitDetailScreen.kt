package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Weeks of history shown by the big heatmap — wider than the compact card's view since
 *  this screen has the room for it (~6 months). */
private const val DETAIL_HEATMAP_WEEKS = 26

private val DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    onNavigateBack: () -> Unit,
    onEditHabit: (String) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.isArchived, state.isDeleted) {
        if (state.isArchived || state.isDeleted) onNavigateBack()
    }

    // Fullscreen modal (not a push-navigation destination): opens fully expanded and can be
    // swiped down to dismiss, matching HabitEditorSheet's pattern.
    ModalBottomSheet(
        onDismissRequest = onNavigateBack,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        HabitDetailContent(
            state = state,
            onNavigateBack = onNavigateBack,
            onEditHabit = { state.habit?.let { onEditHabit(it.id) } },
            onDayClick = viewModel::onDayClick,
            onMonthChanged = viewModel::onMonthChanged,
            onArchive = viewModel::onArchive,
            onUnarchive = viewModel::onUnarchive,
            onDelete = viewModel::onDelete,
            onMessageShown = viewModel::onMessageShown
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailContent(
    state: HabitDetailUiState,
    onNavigateBack: () -> Unit,
    onEditHabit: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onMonthChanged: (Long) -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val futureMessage = stringResource(R.string.routine_message_future_day)
    val outsideMessage = stringResource(R.string.routine_message_outside_range)
    // Both destructive-ish actions always confirm first — archiving is reversible but hides
    // the habit, deleting is permanent — neither should fire straight off a single tap.
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    LaunchedEffect(state.message) {
        val message = when (state.message) {
            HabitDetailMessage.FutureDayNotAllowed -> futureMessage
            HabitDetailMessage.OutsideHabitRange -> outsideMessage
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    val habit = state.habit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (habit != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        HabitPalette.colorAt(habit.colorIndex).copy(alpha = 0.16f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitIcons.iconFor(habit.iconKey),
                                    contentDescription = null,
                                    tint = HabitPalette.colorAt(habit.colorIndex),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (habit == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .testTag("habit_detail_screen"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = habit.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.habit_editor_description_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HabitHeatmap(
                completions = state.completions,
                colorIndex = habit.colorIndex,
                today = state.today,
                startDate = habit.startDate,
                endDate = habit.endDate,
                onDayClick = onDayClick,
                weeks = DETAIL_HEATMAP_WEEKS,
                showMonthLabels = true,
                showDayLabels = true,
                modifier = Modifier.padding(top = 8.dp)
            )

            state.selectedDate?.let { date ->
                SelectedDayCallout(
                    date = date,
                    isCompleted = date in state.completions,
                    accent = HabitPalette.colorAt(habit.colorIndex)
                )
            }

            StatRow(
                streak = state.streak,
                accent = HabitPalette.colorAt(habit.colorIndex),
                isArchived = habit.isArchived,
                onEdit = onEditHabit,
                onArchive = { pendingAction = PendingAction.Archive },
                onUnarchive = onUnarchive,
                onDelete = { pendingAction = PendingAction.Delete }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            HabitCalendarMonth(
                month = state.visibleMonth,
                completions = state.completions,
                today = state.today,
                colorIndex = habit.colorIndex,
                onDayClick = onDayClick
            )

            MonthFooter(
                visibleMonth = state.visibleMonth,
                onMonthChanged = onMonthChanged,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )
        }
    }

    pendingAction?.let { action ->
        val (titleRes, bodyRes, confirmRes) = when (action) {
            PendingAction.Archive -> Triple(
                R.string.routine_confirm_archive_title,
                R.string.routine_confirm_archive_body,
                R.string.routine_archive
            )
            PendingAction.Delete -> Triple(
                R.string.habit_detail_confirm_delete_title,
                R.string.habit_detail_confirm_delete_body,
                R.string.habit_detail_delete
            )
        }
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(stringResource(titleRes)) },
            text = { Text(stringResource(bodyRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            PendingAction.Archive -> onArchive()
                            PendingAction.Delete -> onDelete()
                        }
                        pendingAction = null
                    },
                    modifier = Modifier.testTag("confirm_${action.name}")
                ) {
                    Text(stringResource(confirmRes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(stringResource(R.string.habit_editor_cancel))
                }
            }
        )
    }
}

private enum class PendingAction { Archive, Delete }

@Composable
private fun SelectedDayCallout(date: LocalDate, isCompleted: Boolean, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .testTag("habit_detail_selected_day"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.habit_detail_selected_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = DATE_DISPLAY_FORMAT.format(date),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = stringResource(
                if (isCompleted) R.string.habit_detail_state_completed
                else R.string.habit_detail_state_not_completed
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isCompleted) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatRow(
    streak: StreakState,
    accent: Color,
    isArchived: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = stringResource(R.string.routine_streak_days, streak.current),
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Text(text = streak.current.toString(), style = MaterialTheme.typography.labelMedium, color = accent)
        }
        Spacer(modifier = Modifier.weight(1f))
        // A single-item overflow menu is an anti-pattern (and, anchored this close to the
        // trailing edge, has no room to open without overlapping the Edit button) — every
        // action is just as visible as Edit, side by side, no menu needed.
        SquareIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.routine_edit),
            onClick = onEdit,
            modifier = Modifier.testTag("habit_detail_edit")
        )
        if (isArchived) {
            SquareIconButton(
                icon = Icons.Filled.Unarchive,
                contentDescription = stringResource(R.string.routine_restore),
                onClick = onUnarchive,
                modifier = Modifier.testTag("habit_detail_unarchive")
            )
        } else {
            SquareIconButton(
                icon = Icons.Filled.Archive,
                contentDescription = stringResource(R.string.routine_archive),
                onClick = onArchive,
                modifier = Modifier.testTag("habit_detail_archive")
            )
        }
        SquareIconButton(
            icon = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.habit_detail_delete),
            onClick = onDelete,
            modifier = Modifier.testTag("habit_detail_delete")
        )
    }
}

@Composable
private fun MonthFooter(visibleMonth: YearMonth, onMonthChanged: (Long) -> Unit, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val monthLabel = remember(visibleMonth, locale) {
        monthFormatter.format(visibleMonth.atDay(1)).replaceFirstChar { it.titlecase(locale) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(text = monthLabel, style = MaterialTheme.typography.labelMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SquareIconButton(
                icon = Icons.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.habit_detail_previous_month),
                onClick = { onMonthChanged(-1L) },
                modifier = Modifier.testTag("habit_detail_prev_month")
            )
            SquareIconButton(
                icon = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.habit_detail_next_month),
                onClick = { onMonthChanged(1L) },
                modifier = Modifier.testTag("habit_detail_next_month")
            )
        }
    }
}

@Composable
private fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

private fun previewDetailState(archived: Boolean = false): HabitDetailUiState {
    val today = LocalDate.now()
    return HabitDetailUiState(
        habit = Habit(
            id = "h1",
            title = "Entrenar",
            description = "20 minutos de cardio, todos los días.",
            iconKey = "dumbbell",
            colorIndex = 0,
            startDate = today.minusMonths(3),
            isArchived = archived
        ),
        completions = setOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4)),
        streak = StreakState(current = 4, best = 14, lastCompletedDate = today, completedToday = true),
        visibleMonth = java.time.YearMonth.from(today),
        isLoading = false
    )
}

@Preview(name = "Active habit", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitDetailContentActivePreview() {
    QuoteAnimeTheme {
        HabitDetailContent(
            state = previewDetailState(),
            onNavigateBack = {},
            onEditHabit = {},
            onDayClick = {},
            onMonthChanged = {},
            onArchive = {},
            onUnarchive = {},
            onDelete = {},
            onMessageShown = {}
        )
    }
}

@Preview(name = "Archived habit", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitDetailContentArchivedPreview() {
    QuoteAnimeTheme {
        HabitDetailContent(
            state = previewDetailState(archived = true),
            onNavigateBack = {},
            onEditHabit = {},
            onDayClick = {},
            onMonthChanged = {},
            onArchive = {},
            onUnarchive = {},
            onDelete = {},
            onMessageShown = {}
        )
    }
}

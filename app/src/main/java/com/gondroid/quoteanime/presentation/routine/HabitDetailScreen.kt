package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Weeks of history shown by the big heatmap — wider than the compact card's view since
 *  this screen has the room for it (~6 months). */
private const val DETAIL_HEATMAP_WEEKS = 26

private val DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun HabitDetailScreen(
    onNavigateBack: () -> Unit,
    onEditHabit: (String) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isArchived) {
        if (state.isArchived) onNavigateBack()
    }

    HabitDetailContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onEditHabit = { state.habit?.let { onEditHabit(it.id) } },
        onDayClick = viewModel::onDayClick,
        onMonthChanged = viewModel::onMonthChanged,
        onArchive = viewModel::onArchive,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun HabitDetailContent(
    state: HabitDetailUiState,
    onNavigateBack: () -> Unit,
    onEditHabit: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onMonthChanged: (Long) -> Unit,
    onArchive: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val futureMessage = stringResource(R.string.routine_message_future_day)
    val outsideMessage = stringResource(R.string.routine_message_outside_range)

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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        val habit = state.habit
        if (habit == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("habit_detail_screen"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DetailHeader(habit = habit, onNavigateBack = onNavigateBack)

            HabitHeatmap(
                completions = state.completions,
                colorIndex = habit.colorIndex,
                today = state.today,
                startDate = habit.startDate,
                endDate = habit.endDate,
                onDayClick = onDayClick,
                weeks = DETAIL_HEATMAP_WEEKS,
                showMonthLabels = true,
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
                onEdit = onEditHabit,
                onArchive = onArchive
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
}

@Composable
private fun DetailHeader(habit: Habit, onNavigateBack: () -> Unit) {
    val accent = HabitPalette.colorAt(habit.colorIndex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = HabitIcons.iconFor(habit.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = habit.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.habit_editor_description_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_back))
        }
    }
}

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
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.habit_detail_no_streak_goal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
        SquareIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.routine_edit),
            onClick = onEdit,
            modifier = Modifier.testTag("habit_detail_edit")
        )
        Box {
            SquareIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.habit_card_more_options),
                onClick = { menuExpanded = true },
                modifier = Modifier.testTag("habit_detail_more_options")
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.routine_archive)) },
                    onClick = { menuExpanded = false; onArchive() }
                )
            }
        }
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
                size = 30.dp,
                modifier = Modifier.testTag("habit_detail_prev_month")
            )
            SquareIconButton(
                icon = Icons.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.habit_detail_next_month),
                onClick = { onMonthChanged(1L) },
                size = 30.dp,
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

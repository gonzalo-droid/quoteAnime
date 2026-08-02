package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import java.time.LocalDate

@Composable
fun RoutineScreen(
    viewModel: RoutineViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabitDetail: (String) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RoutineContent(
        state = state,
        today = state.today,
        onNavigateBack = onNavigateBack,
        onToggleToday = viewModel::onToggleToday,
        onToggleDay = viewModel::onToggleDay,
        onArchiveHabit = viewModel::onArchiveHabit,
        onUnarchiveHabit = viewModel::onUnarchiveHabit,
        onFilterChanged = viewModel::onFilterChanged,
        onAddHabit = onAddHabit,
        onEditHabit = onEditHabit,
        onOpenHabitDetail = onOpenHabitDetail,
        onMessageShown = viewModel::onMessageShown,
        onIntroDismissed = viewModel::onIntroDismissed,
        onNavigateToPaywall = onNavigateToPaywall
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineContent(
    state: RoutineUiState,
    today: LocalDate,
    onNavigateBack: () -> Unit,
    onToggleToday: (String) -> Unit,
    onToggleDay: (String, LocalDate) -> Unit,
    onArchiveHabit: (String) -> Unit,
    onUnarchiveHabit: (String) -> Unit,
    onFilterChanged: (RoutineFilter) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit,
    onOpenHabitDetail: (String) -> Unit,
    onMessageShown: () -> Unit,
    onIntroDismissed: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val futureMessage = stringResource(R.string.routine_message_future_day)
    val outsideMessage = stringResource(R.string.routine_message_outside_range)
    // Confirmed at the screen level (not per-card) so every card's "Archivar" reuses the
    // same dialog instead of each needing its own AlertDialog instance.
    var pendingArchiveHabitId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        val message = when (state.message) {
            RoutineMessage.FutureDayNotAllowed -> futureMessage
            RoutineMessage.OutsideHabitRange -> outsideMessage
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routine_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (state.filter == RoutineFilter.ACTIVE && state.canAddHabit) {
                        IconButton(
                            onClick = onAddHabit,
                            modifier = Modifier.testTag("add_habit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.routine_add_habit)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            // Both empty branches pad to 16dp on every side, matching the populated
            // LazyColumn's contentPadding below — otherwise the filter chips sit flush
            // against the edge here but 16dp inset there, visibly shifting position
            // whenever the tab's content goes from empty to populated (or back).
            state.isEmpty && state.filter == RoutineFilter.ACTIVE -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                RoutineFilterRow(filter = state.filter, onFilterChanged = onFilterChanged)
                EmptyRoutine(onAddHabit = onAddHabit, modifier = Modifier.fillMaxSize())
            }

            state.isEmpty -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                RoutineFilterRow(filter = state.filter, onFilterChanged = onFilterChanged)
                EmptyArchivedRoutine(modifier = Modifier.fillMaxSize())
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RoutineFilterRow(filter = state.filter, onFilterChanged = onFilterChanged)
                }
                items(items = state.habits, key = { it.habit.id }) { progress ->
                    HabitCard(
                        progress = progress,
                        today = today,
                        onToggleToday = { onToggleToday(progress.habit.id) },
                        onToggleDay = { date -> onToggleDay(progress.habit.id, date) },
                        onEdit = { onEditHabit(progress.habit.id) },
                        onRequestArchive = { pendingArchiveHabitId = progress.habit.id },
                        onUnarchive = { onUnarchiveHabit(progress.habit.id) },
                        onOpenDetail = { onOpenHabitDetail(progress.habit.id) }
                    )
                }
                if (state.filter == RoutineFilter.ACTIVE && !state.canAddHabit) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.routine_limit_reached, state.maxHabits),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = onNavigateToPaywall) {
                                Text(stringResource(R.string.routine_limit_reached_upgrade))
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showIntro) {
        AlertDialog(
            onDismissRequest = onIntroDismissed,
            title = { Text(stringResource(R.string.routine_intro_title)) },
            text = { Text(stringResource(R.string.routine_intro_body)) },
            confirmButton = {
                TextButton(
                    onClick = { onIntroDismissed(); onAddHabit() },
                    modifier = Modifier.testTag("intro_start")
                ) {
                    Text(stringResource(R.string.routine_intro_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onIntroDismissed) {
                    Text(stringResource(R.string.routine_intro_dismiss))
                }
            }
        )
    }

    pendingArchiveHabitId?.let { habitId ->
        AlertDialog(
            onDismissRequest = { pendingArchiveHabitId = null },
            title = { Text(stringResource(R.string.routine_confirm_archive_title)) },
            text = { Text(stringResource(R.string.routine_confirm_archive_body)) },
            confirmButton = {
                TextButton(
                    onClick = { onArchiveHabit(habitId); pendingArchiveHabitId = null },
                    modifier = Modifier.testTag("confirm_archive")
                ) {
                    Text(stringResource(R.string.routine_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchiveHabitId = null }) {
                    Text(stringResource(R.string.habit_editor_cancel))
                }
            }
        )
    }
}

@Composable
private fun RoutineFilterRow(filter: RoutineFilter, onFilterChanged: (RoutineFilter) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag("routine_filter_row")
    ) {
        FilterChip(
            selected = filter == RoutineFilter.ACTIVE,
            onClick = { onFilterChanged(RoutineFilter.ACTIVE) },
            label = { Text(stringResource(R.string.routine_filter_active)) }
        )
        FilterChip(
            selected = filter == RoutineFilter.ARCHIVED,
            onClick = { onFilterChanged(RoutineFilter.ARCHIVED) },
            label = { Text(stringResource(R.string.routine_filter_archived)) }
        )
    }
}

@Composable
private fun EmptyRoutine(onAddHabit: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.routine_empty_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.routine_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onAddHabit,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .testTag("empty_add_habit")
            ) {
                Text(stringResource(R.string.routine_add_habit))
            }
        }
    }
}

/** Unlike [EmptyRoutine], this has no CTA — you land here after archiving something, not
 *  before creating anything, so it explains what archiving does instead of prompting an
 *  action. The subtext line ("Todavía no archivaste ninguno") is de-emphasized so it reads
 *  as the actual empty-state confirmation rather than a continuation of the explainer. */
@Composable
private fun EmptyArchivedRoutine(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.routine_archived_empty_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.routine_archived_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = stringResource(R.string.routine_archived_empty_subtext),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

private fun previewHabitWithProgress(
    id: String,
    title: String,
    colorIndex: Int,
    completedToday: Boolean,
    today: LocalDate,
    archived: Boolean = false
): HabitWithProgress = HabitWithProgress(
    habit = Habit(
        id = id,
        title = title,
        iconKey = "dumbbell",
        colorIndex = colorIndex,
        startDate = today.minusMonths(2),
        isArchived = archived
    ),
    completions = setOf(today, today.minusDays(1), today.minusDays(3)),
    streak = StreakState(2, 11, today, completedToday),
    completionRate = 0.62f
)

private fun previewRoutineContent(state: RoutineUiState, today: LocalDate) = @Composable {
    QuoteAnimeTheme {
        RoutineContent(
            state = state,
            today = today,
            onNavigateBack = {},
            onToggleToday = {},
            onToggleDay = { _, _ -> },
            onArchiveHabit = {},
            onUnarchiveHabit = {},
            onFilterChanged = {},
            onAddHabit = {},
            onEditHabit = {},
            onOpenHabitDetail = {},
            onMessageShown = {},
            onIntroDismissed = {},
            onNavigateToPaywall = {}
        )
    }
}

@Preview(name = "Populated", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentPreview() {
    val today = LocalDate.now()
    previewRoutineContent(
        state = RoutineUiState(
            habits = listOf(
                previewHabitWithProgress("h1", "Entrenar", 0, completedToday = true, today = today),
                previewHabitWithProgress("h2", "Leer", 3, completedToday = false, today = today)
            ),
            activeCount = 2,
            globalStreak = StreakState(2, 11, today, true),
            isLoading = false,
            maxHabits = 3
        ),
        today = today
    )()
}

@Preview(name = "Empty, active tab", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentEmptyActivePreview() {
    val today = LocalDate.now()
    previewRoutineContent(
        state = RoutineUiState(isLoading = false, maxHabits = 3, filter = RoutineFilter.ACTIVE),
        today = today
    )()
}

@Preview(name = "Empty, archived tab", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentEmptyArchivedPreview() {
    val today = LocalDate.now()
    previewRoutineContent(
        state = RoutineUiState(isLoading = false, maxHabits = 3, filter = RoutineFilter.ARCHIVED),
        today = today
    )()
}

@Preview(name = "Limit reached", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentLimitReachedPreview() {
    val today = LocalDate.now()
    previewRoutineContent(
        state = RoutineUiState(
            habits = listOf(
                previewHabitWithProgress("h1", "Entrenar", 0, completedToday = true, today = today),
                previewHabitWithProgress("h2", "Leer", 3, completedToday = false, today = today),
                previewHabitWithProgress("h3", "Meditar", 5, completedToday = true, today = today)
            ),
            activeCount = 3,
            globalStreak = StreakState(2, 11, today, true),
            isLoading = false,
            maxHabits = 3
        ),
        today = today
    )()
}

@Preview(name = "Archived tab, populated", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentArchivedPreview() {
    val today = LocalDate.now()
    previewRoutineContent(
        state = RoutineUiState(
            habits = listOf(
                previewHabitWithProgress("h4", "Yoga", 2, completedToday = false, today = today, archived = true)
            ),
            activeCount = 0,
            globalStreak = StreakState(0, 11, today, false),
            isLoading = false,
            maxHabits = 3,
            filter = RoutineFilter.ARCHIVED
        ),
        today = today
    )()
}

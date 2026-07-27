package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RoutineContent(
        state = state,
        today = state.today,
        onToggleToday = viewModel::onToggleToday,
        onToggleDay = viewModel::onToggleDay,
        onArchiveHabit = viewModel::onArchiveHabit,
        onAddHabit = onAddHabit,
        onEditHabit = onEditHabit,
        onMessageShown = viewModel::onMessageShown,
        onIntroDismissed = viewModel::onIntroDismissed
    )
}

@Composable
fun RoutineContent(
    state: RoutineUiState,
    today: LocalDate,
    onToggleToday: (String) -> Unit,
    onToggleDay: (String, LocalDate) -> Unit,
    onArchiveHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    onEditHabit: (String) -> Unit,
    onMessageShown: () -> Unit,
    onIntroDismissed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val futureMessage = stringResource(R.string.routine_message_future_day)
    val outsideMessage = stringResource(R.string.routine_message_outside_range)

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.canAddHabit) {
                ExtendedFloatingActionButton(
                    onClick = onAddHabit,
                    modifier = Modifier.testTag("add_habit_fab"),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.routine_add_habit)) }
                )
            }
        }
    ) { padding ->
        when {
            state.isEmpty -> EmptyRoutine(
                onAddHabit = onAddHabit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RoutineHeader(state = state)
                }
                items(items = state.habits, key = { it.habit.id }) { progress ->
                    HabitCard(
                        progress = progress,
                        today = today,
                        onToggleToday = { onToggleToday(progress.habit.id) },
                        onToggleDay = { date -> onToggleDay(progress.habit.id, date) },
                        onEdit = { onEditHabit(progress.habit.id) },
                        onArchive = { onArchiveHabit(progress.habit.id) }
                    )
                }
                if (!state.canAddHabit) {
                    item {
                        Text(
                            text = stringResource(R.string.routine_limit_reached, state.maxHabits),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
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
}

@Composable
private fun RoutineHeader(state: RoutineUiState) {
    Column(modifier = Modifier.testTag("routine_header")) {
        Text(
            text = stringResource(R.string.routine_streak_days, state.globalStreak.current),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(
                R.string.routine_progress_today,
                state.completedToday,
                state.totalHabits
            ),
            style = MaterialTheme.typography.bodyMedium
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

@Preview(showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun RoutineContentPreview() {
    val today = LocalDate.now()
    val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusMonths(2)
    )
    QuoteAnimeTheme {
        RoutineContent(
            state = RoutineUiState(
                habits = listOf(
                    HabitWithProgress(
                        habit = habit,
                        completions = setOf(today, today.minusDays(1), today.minusDays(3)),
                        streak = StreakState(2, 11, today, true),
                        completionRate = 0.62f
                    )
                ),
                globalStreak = StreakState(2, 11, today, true),
                isLoading = false,
                maxHabits = 3
            ),
            today = today,
            onToggleToday = {},
            onToggleDay = { _, _ -> },
            onArchiveHabit = {},
            onAddHabit = {},
            onEditHabit = {},
            onMessageShown = {},
            onIntroDismissed = {}
        )
    }
}

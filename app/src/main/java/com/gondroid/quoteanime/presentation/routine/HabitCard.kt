package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HabitCard(
    progress: HabitWithProgress,
    today: LocalDate,
    onToggleToday: () -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onRequestArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = progress.habit
    val accent = HabitPalette.colorAt(habit.colorIndex)
    val themeImageRes = HabitThemeImages.resFor(habit.coverAnimeSlug)
    var menuExpanded by remember { mutableStateOf(false) }

    // Tapping the card body opens the detail screen; the toggle-today and overflow
    // IconButtons inside still consume their own taps first, so this never fires for them.
    Card(
        onClick = onOpenDetail,
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
        if (themeImageRes != null) {
            Image(
                painter = painterResource(id = themeImageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Keeps the themed image recognizable without compromising text legibility,
            // by scrimming it close to the card's normal solid background color.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f))
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = HabitIcons.iconFor(habit.iconKey),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                if (habit.isArchived) {
                    // Archived habits aren't actively tracked — the only action left is
                    // putting them back; no toggle-today, no edit/archive menu.
                    IconButton(
                        onClick = onUnarchive,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("unarchive_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Unarchive,
                            contentDescription = stringResource(R.string.routine_restore),
                            tint = accent
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleToday,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_today_${habit.id}")
                    ) {
                        Icon(
                            imageVector = if (progress.streak.completedToday) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.CheckCircle
                            },
                            contentDescription = stringResource(
                                if (progress.streak.completedToday) R.string.routine_unmark_today
                                else R.string.routine_mark_today
                            ),
                            tint = accent
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.habit_card_more_options)
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.routine_edit)) },
                                onClick = { menuExpanded = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.routine_archive)) },
                                onClick = { menuExpanded = false; onRequestArchive() }
                            )
                        }
                    }
                }
            }

            HabitHeatmap(
                completions = progress.completions,
                colorIndex = habit.colorIndex,
                today = today,
                startDate = habit.startDate,
                endDate = habit.endDate,
                onDayClick = onToggleDay,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.routine_current_streak, progress.streak.current),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.routine_best_streak, progress.streak.best),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(
                        R.string.routine_completion_rate,
                        (progress.completionRate * 100).roundToInt()
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        }
    }
}

private fun previewProgress(completedToday: Boolean, archived: Boolean = false): HabitWithProgress {
    val today = LocalDate.now()
    return HabitWithProgress(
        habit = Habit(
            id = "h1",
            title = "Entrenar",
            iconKey = "dumbbell",
            colorIndex = 0,
            startDate = today.minusMonths(2),
            isArchived = archived
        ),
        completions = setOf(today, today.minusDays(1), today.minusDays(3)),
        streak = StreakState(current = 2, best = 11, lastCompletedDate = today, completedToday = completedToday),
        completionRate = 0.62f
    )
}

@Preview(name = "Not completed today", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitCardIncompletePreview() {
    QuoteAnimeTheme {
        HabitCard(
            progress = previewProgress(completedToday = false),
            today = LocalDate.now(),
            onToggleToday = {},
            onToggleDay = {},
            onEdit = {},
            onRequestArchive = {},
            onUnarchive = {},
            onOpenDetail = {}
        )
    }
}

@Preview(name = "Completed today", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitCardCompletePreview() {
    QuoteAnimeTheme {
        HabitCard(
            progress = previewProgress(completedToday = true),
            today = LocalDate.now(),
            onToggleToday = {},
            onToggleDay = {},
            onEdit = {},
            onRequestArchive = {},
            onUnarchive = {},
            onOpenDetail = {}
        )
    }
}

@Preview(name = "Archived", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitCardArchivedPreview() {
    QuoteAnimeTheme {
        HabitCard(
            progress = previewProgress(completedToday = false, archived = true),
            today = LocalDate.now(),
            onToggleToday = {},
            onToggleDay = {},
            onEdit = {},
            onRequestArchive = {},
            onUnarchive = {},
            onOpenDetail = {}
        )
    }
}

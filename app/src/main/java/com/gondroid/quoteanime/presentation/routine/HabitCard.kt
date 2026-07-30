package com.gondroid.quoteanime.presentation.routine

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun HabitCard(
    progress: HabitWithProgress,
    today: LocalDate,
    onToggleToday: () -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundImageUrl: String? = null
) {
    val habit = progress.habit
    val accent = HabitPalette.colorAt(habit.colorIndex)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backgroundImageUrl)
                    .crossfade(true)
                    .build(),
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
                        onClick = { menuExpanded = false; onArchive() }
                    )
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

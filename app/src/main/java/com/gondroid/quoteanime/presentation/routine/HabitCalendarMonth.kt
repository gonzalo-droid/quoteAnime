package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Classic month calendar, complementing the heatmap with the same [completions] data read
 * a different way: a small dot marks completed days, today is highlighted, and days outside
 * [month] are dimmed. Tapping a day reuses the same toggle gesture as the heatmap.
 */
@Composable
fun HabitCalendarMonth(
    month: YearMonth,
    completions: Set<LocalDate>,
    today: LocalDate,
    colorIndex: Int,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = HabitPalette.colorAt(colorIndex)
    val locale = Locale.getDefault()
    val days = remember(month) { CalendarMonthGrid.daysFor(month) }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        days.chunked(CalendarMonthGrid.COLUMNS).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val inMonth = date.month == month.month && date.year == month.year
                    val isToday = date == today
                    val isCompleted = date in completions
                    val stateLabel = stringResource(
                        if (isCompleted) R.string.routine_heatmap_cell_completed
                        else R.string.routine_heatmap_cell_not_completed,
                        dateFormatter.format(date)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clickable { onDayClick(date) }
                            .semantics { contentDescription = stateLabel }
                            .testTag("habit_calendar_day_$date"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .aspectRatio(1f)
                                    .background(accent.copy(alpha = 0.22f), CircleShape)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    isToday -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .background(
                                        color = if (isCompleted) accent else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Calendar month", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitCalendarMonthPreview() {
    val today = LocalDate.now()
    val completions = (0 until 20).filter { it % 2 == 0 }.map { today.minusDays(it.toLong()) }.toSet()
    QuoteAnimeTheme {
        HabitCalendarMonth(
            month = YearMonth.from(today),
            completions = completions,
            today = today,
            colorIndex = 4,
            onDayClick = {}
        )
    }
}

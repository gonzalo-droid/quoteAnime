package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val GAP = 3.dp
private const val MIN_CELL_DP = 10f
private val DAY_LABEL_WIDTH = 16.dp

// Classic GitHub-contribution-graph convention: only label alternating
// weekdays (Mon/Wed/Fri) to avoid clutter at this cell density. Rows map to
// weekdays as row 0 = Monday ... row 6 = Sunday (see HeatmapGrid.gridStart).
private val DAY_LABEL_ROWS = listOf(0, 2, 4)

/**
 * Contribution-style grid: 17 columns (weeks) by 7 rows (weekdays). The cell
 * size is derived from the available width so the whole range fits a phone
 * screen without horizontal scrolling.
 *
 * The [Canvas] only draws the colored cells; tap handling and accessibility
 * both come from a transparent overlay of per-cell composables layered on
 * top at the exact same coordinates, so there is a single source of truth
 * for "what happens when you tap here" and each visible day is individually
 * focusable and announced by a screen reader.
 */
@Composable
fun HabitHeatmap(
    completions: Set<LocalDate>,
    colorIndex: Int,
    today: LocalDate,
    startDate: LocalDate,
    endDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    weeks: Int = GetActiveHabitsUseCase.VISIBLE_WEEKS,
    showMonthLabels: Boolean = false,
    showDayLabels: Boolean = false
) {
    val gridStart = remember(today, weeks) { HeatmapGrid.gridStart(today, weeks) }
    val activeColor = HabitPalette.colorAt(colorIndex)
    val emptyColor = Color.White.copy(alpha = 0.06f)
    val locale = Locale.getDefault()
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val heatmapDescription = stringResource(R.string.routine_heatmap_description)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_heatmap")
            .semantics { contentDescription = heatmapDescription }
    ) {
        val density = LocalDensity.current
        val gapPx = with(density) { GAP.toPx() }
        val totalGaps = gapPx * (weeks - 1)
        // When day-of-week labels are shown, a fixed-width column is reserved on the
        // left for them and the grid's available width shrinks accordingly. When
        // showDayLabels is false this is 0 and every computation below is identical
        // to the original layout, so the compact heatmap on habit cards is unaffected.
        val dayLabelWidthPx = if (showDayLabels) with(density) { DAY_LABEL_WIDTH.toPx() } else 0f
        val labelOffsetDp = if (showDayLabels) DAY_LABEL_WIDTH else 0.dp
        val cellPx = ((constraints.maxWidth - dayLabelWidthPx - totalGaps) / weeks)
            .coerceAtLeast(with(density) { MIN_CELL_DP.dp.toPx() })
        val cellDp = with(density) { cellPx.toDp() }
        val heightDp = with(density) {
            (cellPx * HeatmapGrid.ROWS + gapPx * (HeatmapGrid.ROWS - 1)).toDp()
        }
        val gridWidthDp = with(density) {
            (cellPx * weeks + totalGaps).toDp()
        }

        Column {
            if (showMonthLabels) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(bottom = 4.dp, start = labelOffsetDp)
                ) {
                    // A label is placed on whichever column contains the 1st of a month —
                    // the same convention contribution graphs use, and it keeps every
                    // label aligned to the exact column the Canvas below draws.
                    for (column in 0 until weeks) {
                        val monthStart = (0 until HeatmapGrid.ROWS)
                            .asSequence()
                            .map { row -> HeatmapGrid.dateAt(column, row, gridStart) }
                            .firstOrNull { it.dayOfMonth == 1 }
                        if (monthStart != null) {
                            val xDp = with(density) { (column * (cellPx + gapPx)).toDp() }
                            Text(
                                text = monthStart.month.getDisplayName(TextStyle.SHORT, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.offset(x = xDp)
                            )
                        }
                    }
                }
            }

            Box {
                if (showDayLabels) {
                    // Positioned at x = 0 of this Box, to the left of the offset grid
                    // Box below. Uses the same row -> y technique as the tap-target
                    // overlay, so labels line up with actual grid rows exactly.
                    Box(
                        modifier = Modifier
                            .width(DAY_LABEL_WIDTH)
                            .height(heightDp)
                    ) {
                        for (row in DAY_LABEL_ROWS) {
                            val yDp = with(density) { (row * (cellPx + gapPx)).toDp() }
                            Text(
                                text = DayOfWeek.of(row + 1).getDisplayName(TextStyle.NARROW, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.offset(y = yDp)
                            )
                        }
                    }
                }

                // Every cell in the grid is drawn from day one, even outside the habit's
                // active window — an empty grid otherwise reads as broken. Only completed
                // cells inside the valid window get the accent color; everything else
                // (future days, days before startDate/after endDate, and simply
                // not-yet-completed days) shares the same muted "empty" tone.
                //
                // Shifted right by labelOffsetDp (0 unless showDayLabels) so the day
                // labels above have room on the left; the Canvas and tap-target loops
                // below keep using column/row-relative offsets unchanged since the
                // offset is applied once, on this wrapping Box.
                Box(modifier = Modifier.offset(x = labelOffsetDp)) {
                    Canvas(
                        modifier = (
                            if (showDayLabels) Modifier.width(gridWidthDp) else Modifier.fillMaxWidth()
                            ).height(heightDp)
                    ) {
                        for (column in 0 until weeks) {
                            for (row in 0 until HeatmapGrid.ROWS) {
                                val date = HeatmapGrid.dateAt(column, row, gridStart)
                                val isFuture = date.isAfter(today)
                                val isOutsideRange = date.isBefore(startDate) ||
                                    (endDate != null && date.isAfter(endDate))
                                val isCompleted = date in completions && !isFuture && !isOutsideRange

                                drawRoundRect(
                                    color = if (isCompleted) activeColor else emptyColor,
                                    topLeft = Offset(
                                        x = column * (cellPx + gapPx),
                                        y = row * (cellPx + gapPx)
                                    ),
                                    size = Size(cellPx, cellPx),
                                    cornerRadius = CornerRadius(cellPx * 0.25f)
                                )
                            }
                        }
                    }

                    // Accessible tap targets, one per visible cell, positioned at the same
                    // coordinates the Canvas above draws that cell's square at. Future or
                    // out-of-range dates get no element at all: they are not tappable and
                    // not present in the accessibility tree, matching the drawing loop's
                    // filter above.
                    for (column in 0 until weeks) {
                        for (row in 0 until HeatmapGrid.ROWS) {
                            val date = HeatmapGrid.dateAt(column, row, gridStart)
                            val isFuture = date.isAfter(today)
                            val isOutsideRange = date.isBefore(startDate) ||
                                (endDate != null && date.isAfter(endDate))
                            if (isFuture || isOutsideRange) continue

                            val isCompleted = date in completions
                            val formattedDate = dateFormatter.format(date)
                            val stateLabel = stringResource(
                                if (isCompleted) {
                                    R.string.routine_heatmap_cell_completed
                                } else {
                                    R.string.routine_heatmap_cell_not_completed
                                },
                                formattedDate
                            )
                            val xDp = with(density) { (column * (cellPx + gapPx)).toDp() }
                            val yDp = with(density) { (row * (cellPx + gapPx)).toDp() }

                            Box(
                                modifier = Modifier
                                    .offset(x = xDp, y = yDp)
                                    .size(cellDp)
                                    .clickable { onDayClick(date) }
                                    .semantics { contentDescription = stateLabel }
                                    .testTag("habit_heatmap_cell_$date")
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun previewCompletions(today: LocalDate): Set<LocalDate> =
    (0 until 90).filter { it % 3 != 0 }.map { today.minusDays(it.toLong()) }.toSet()

@Preview(name = "Compact card heatmap", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitHeatmapCompactPreview() {
    val today = LocalDate.now()
    QuoteAnimeTheme {
        HabitHeatmap(
            completions = previewCompletions(today),
            colorIndex = 0,
            today = today,
            startDate = today.minusMonths(3),
            endDate = null,
            onDayClick = {}
        )
    }
}

@Preview(name = "Detail heatmap, labeled", showBackground = true, backgroundColor = 0xFF0C0C1E)
@Composable
private fun HabitHeatmapDetailPreview() {
    val today = LocalDate.now()
    QuoteAnimeTheme {
        HabitHeatmap(
            completions = previewCompletions(today),
            colorIndex = 3,
            today = today,
            startDate = today.minusMonths(6),
            endDate = null,
            onDayClick = {},
            weeks = 26,
            showMonthLabels = true,
            showDayLabels = true
        )
    }
}

package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.R
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val GAP = 3.dp
private const val MIN_CELL_DP = 10f

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
    modifier: Modifier = Modifier
) {
    val weeks = GetActiveHabitsUseCase.VISIBLE_WEEKS
    val gridStart = remember(today) { HeatmapGrid.gridStart(today, weeks) }
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
        val cellPx = ((constraints.maxWidth - totalGaps) / weeks)
            .coerceAtLeast(with(density) { MIN_CELL_DP.dp.toPx() })
        val cellDp = with(density) { cellPx.toDp() }
        val heightDp = with(density) {
            (cellPx * HeatmapGrid.ROWS + gapPx * (HeatmapGrid.ROWS - 1)).toDp()
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
        ) {
            for (column in 0 until weeks) {
                for (row in 0 until HeatmapGrid.ROWS) {
                    val date = HeatmapGrid.dateAt(column, row, gridStart)
                    val isFuture = date.isAfter(today)
                    val isOutsideRange = date.isBefore(startDate) ||
                        (endDate != null && date.isAfter(endDate))
                    if (isFuture || isOutsideRange) continue

                    drawRoundRect(
                        color = if (date in completions) activeColor else emptyColor,
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

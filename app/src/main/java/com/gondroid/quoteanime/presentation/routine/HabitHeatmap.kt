package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import java.time.LocalDate

private val GAP = 3.dp
private const val MIN_CELL_DP = 10f

/**
 * Contribution-style grid: 17 columns (weeks) by 7 rows (weekdays). The cell
 * size is derived from the available width so the whole range fits a phone
 * screen without horizontal scrolling.
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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val gapPx = with(density) { GAP.toPx() }
        val totalGaps = gapPx * (weeks - 1)
        val cellPx = ((constraints.maxWidth - totalGaps) / weeks)
            .coerceAtLeast(with(density) { MIN_CELL_DP.dp.toPx() })
        val heightDp = with(density) {
            (cellPx * HeatmapGrid.ROWS + gapPx * (HeatmapGrid.ROWS - 1)).toDp()
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .testTag("habit_heatmap")
                .pointerInput(gridStart, today, startDate, endDate, cellPx, gapPx) {
                    detectTapGestures { offset ->
                        val cell = HeatmapGrid.cellAt(offset.x, offset.y, cellPx, gapPx, weeks)
                            ?: return@detectTapGestures
                        val date = HeatmapGrid.dateAt(cell.column, cell.row, gridStart)
                        val isInsideRange = !date.isBefore(startDate) &&
                            (endDate == null || !date.isAfter(endDate))
                        if (!date.isAfter(today) && isInsideRange) onDayClick(date)
                    }
                }
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
    }
}

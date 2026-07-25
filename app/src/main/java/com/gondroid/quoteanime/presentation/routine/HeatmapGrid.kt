package com.gondroid.quoteanime.presentation.routine

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Pure geometry of the contribution-style grid: columns are weeks, rows are
 * weekdays. Kept free of Compose so it can be unit tested.
 */
object HeatmapGrid {

    const val ROWS = 7

    data class Cell(val column: Int, val row: Int)

    /** Monday of the first visible week. */
    fun gridStart(today: LocalDate, weeks: Int): LocalDate =
        today.minusWeeks((weeks - 1).toLong())
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun columnsFor(weeks: Int): Int = weeks

    fun dateAt(column: Int, row: Int, gridStart: LocalDate): LocalDate =
        gridStart.plusDays((column * ROWS + row).toLong())

    /**
     * Resolves a tap position to a cell, or null when it lands on a gap or
     * outside the grid.
     */
    fun cellAt(x: Float, y: Float, cellSizePx: Float, gapPx: Float, columns: Int): Cell? {
        if (x < 0f || y < 0f) return null
        val stride = cellSizePx + gapPx
        val column = (x / stride).toInt()
        val row = (y / stride).toInt()
        if (row >= ROWS) return null
        if (column >= columns) return null
        val offsetInColumn = x - column * stride
        val offsetInRow = y - row * stride
        if (offsetInColumn > cellSizePx || offsetInRow > cellSizePx) return null
        return Cell(column = column, row = row)
    }
}

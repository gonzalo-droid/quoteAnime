package com.gondroid.quoteanime.presentation.routine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - The grid always starts on a Monday so rows line up with weekdays
 *  - The grid covers the requested number of weeks and always includes today
 *  - Cell coordinates map back to the expected date
 *  - Taps inside the gap between cells resolve to no cell
 *  - Taps outside the grid resolve to no cell
 */
class HeatmapGridTest {

    private val today = LocalDate.parse("2026-07-25") // Saturday
    private val weeks = 17

    @Test
    fun `given any today, when the grid starts, then it starts on a Monday`() {
        val start = HeatmapGrid.gridStart(today, weeks)

        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
    }

    @Test
    fun `given a grid of 17 weeks, when built, then it spans 17 columns and contains today`() {
        val start = HeatmapGrid.gridStart(today, weeks)
        val lastCellDate = HeatmapGrid.dateAt(weeks - 1, 6, start)

        assertEquals(weeks, HeatmapGrid.columnsFor(weeks))
        assertEquals(true, !today.isBefore(start) && !today.isAfter(lastCellDate))
    }

    @Test
    fun `given a column and row, when resolved, then the expected date is returned`() {
        val start = HeatmapGrid.gridStart(today, weeks)

        assertEquals(start, HeatmapGrid.dateAt(0, 0, start))
        assertEquals(start.plusDays(1), HeatmapGrid.dateAt(0, 1, start))
        assertEquals(start.plusDays(7), HeatmapGrid.dateAt(1, 0, start))
    }

    @Test
    fun `given a tap in the middle of a cell, when resolved, then the cell is returned`() {
        // cell 14px + 3px gap: the cell at column 2 row 1 spans x 34..48, y 17..31
        val cell = HeatmapGrid.cellAt(x = 40f, y = 20f, cellSizePx = 14f, gapPx = 3f)

        assertEquals(HeatmapGrid.Cell(column = 2, row = 1), cell)
    }

    @Test
    fun `given a tap inside the gap, when resolved, then no cell is returned`() {
        // x = 15f falls in the 14..17 gap after the first column
        assertNull(HeatmapGrid.cellAt(x = 15f, y = 5f, cellSizePx = 14f, gapPx = 3f))
    }

    @Test
    fun `given a negative coordinate, when resolved, then no cell is returned`() {
        assertNull(HeatmapGrid.cellAt(x = -1f, y = 5f, cellSizePx = 14f, gapPx = 3f))
    }
}

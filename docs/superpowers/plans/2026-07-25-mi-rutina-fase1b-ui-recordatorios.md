# «Mi rutina» — Fase 1B: interfaz, navegación y recordatorios — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Poner la sección «Mi rutina» en manos del usuario: pantalla con heatmap interactivo, editor de hábitos, barra inferior de navegación, recordatorios con acción rápida, medición y textos en español e inglés.

**Architecture:** MVVM sobre los use cases de la fase 1A. Cada pantalla se parte en un composable con estado (`RoutineScreen`, inyecta el ViewModel) y uno sin estado (`RoutineContent`, recibe estado y lambdas), que es el que se prueba. El heatmap se dibuja en `Canvas` y su geometría vive en un objeto puro (`HeatmapGrid`) probado con tests unitarios. Los recordatorios usan WorkManager con reprogramación en cadena, un trabajo único por hábito.

**Tech Stack:** Jetpack Compose + Material3, Hilt, WorkManager, Firebase Analytics, JUnit4 + MockK + Turbine, Compose UI Test.

**Spec:** `docs/superpowers/specs/2026-07-25-mi-rutina-habit-tracker-design.md`
**Depende de:** `docs/superpowers/plans/2026-07-25-mi-rutina-fase1a-nucleo.md` completada.

## Global Constraints

- Todo texto visible vive en `strings.xml`; ninguno se escribe directamente en el código.
- Los composables sin estado no conocen el ViewModel ni Hilt: reciben `RoutineUiState` y lambdas. Son los que se prueban.
- Los ViewModels exponen `StateFlow` y siguen el patrón de `SettingsViewModel`: escribir y reprogramar en la misma acción, sin esperar al flow reactivo.
- El límite de hábitos se lee siempre de `PremiumGate`, nunca como literal.
- Colores solo desde `HabitPalette`; iconos solo desde `HabitIcons`.
- Áreas táctiles de al menos 48 dp.
- Los recordatorios usan `WorkManager`. **Prohibido** usar `AlarmManager` ni pedir `SCHEDULE_EXACT_ALARM`.
- Comentarios en inglés, siguiendo el estilo del resto del proyecto.

## File Structure

**Crear:**

| Archivo | Responsabilidad |
|---|---|
| `presentation/routine/HabitPalette.kt` | Los 8 colores seleccionables |
| `presentation/routine/HabitIcons.kt` | Mapa de `iconKey` a icono, y resolución de títulos de plantilla |
| `presentation/routine/HeatmapGrid.kt` | Geometría pura del heatmap (fechas ↔ celdas) |
| `presentation/routine/HabitHeatmap.kt` | Composable del gráfico |
| `presentation/routine/HabitCard.kt` | Tarjeta de un hábito |
| `presentation/routine/RoutineUiState.kt` | Estado de la pantalla |
| `presentation/routine/RoutineViewModel.kt` | Lógica de presentación |
| `presentation/routine/RoutineScreen.kt` | Pantalla con estado + `RoutineContent` sin estado |
| `presentation/routine/HabitEditorSheet.kt` | Alta y edición |
| `presentation/navigation/BottomNavBar.kt` | Barra inferior |
| `domain/usecase/GetGlobalStreakUseCase.kt` | Racha global |
| `notification/HabitReminderScheduler.kt` | Programación por hábito |
| `notification/NextReminderCalculator.kt` | Cálculo puro de la próxima ocurrencia |
| `notification/HabitReminderReceiver.kt` | Acción «Hecho» de la notificación |
| `worker/HabitReminderWorker.kt` | Muestra el recordatorio y se reprograma |
| `analytics/RoutineAnalytics.kt` | Eventos de la feature |

**Modificar:**

| Archivo | Cambio |
|---|---|
| `presentation/navigation/AppNavGraph.kt` | Rutas nuevas y `Scaffold` con barra inferior |
| `notification/NotificationHelper.kt` | Canal `habit_reminders` y notificación de hábito |
| `presentation/onboarding/OnboardingScreen.kt` | Paso opcional de primer hábito |
| `data/local/datastore/UserPreferencesDataStore.kt` | Clave `routine_intro_seen` |
| `res/values/strings.xml` | Pasa a inglés (predeterminado) |
| `res/values-es/strings.xml` | Español (archivo nuevo con el contenido actual) |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Firebase Analytics |
| `AndroidManifest.xml` | Registro de `HabitReminderReceiver` |

---

### Task 1: Paleta, iconos y geometría del heatmap

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitPalette.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitIcons.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HeatmapGrid.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/presentation/routine/HeatmapGridTest.kt`

**Interfaces:**
- Consumes: `GetActiveHabitsUseCase.VISIBLE_WEEKS` de la fase 1A.
- Produces: `HabitPalette.colorAt(index: Int): Color`, `HabitPalette.COLORS: List<Color>`, `HabitIcons.iconFor(key: String): ImageVector`, y `HeatmapGrid` con `gridStart(today, weeks): LocalDate`, `dateAt(column, row, gridStart): LocalDate`, `cellAt(x, y, cellSizePx, gapPx): Cell?`, `columnsFor(weeks): Int`.

- [ ] **Step 1: Escribir el test de la geometría**

Crear `app/src/test/java/com/gondroid/quoteanime/presentation/routine/HeatmapGridTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.HeatmapGridTest"
```

Esperado: FALLA con `Unresolved reference: HeatmapGrid`.

- [ ] **Step 3: Implementar la geometría**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HeatmapGrid.kt`:

```kotlin
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
    fun cellAt(x: Float, y: Float, cellSizePx: Float, gapPx: Float): Cell? {
        if (x < 0f || y < 0f) return null
        val stride = cellSizePx + gapPx
        val column = (x / stride).toInt()
        val row = (y / stride).toInt()
        if (row >= ROWS) return null
        val offsetInColumn = x - column * stride
        val offsetInRow = y - row * stride
        if (offsetInColumn > cellSizePx || offsetInRow > cellSizePx) return null
        return Cell(column = column, row = row)
    }
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.HeatmapGridTest"
```

Esperado: los 6 tests en verde.

- [ ] **Step 5: Crear la paleta**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitPalette.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.ui.graphics.Color

/**
 * Fixed palette so every habit stays readable over the app's dark background.
 * Habits persist the index, never a hex value: changing a color here updates
 * existing habits without a data migration.
 */
object HabitPalette {

    val COLORS: List<Color> = listOf(
        Color(0xFFA78BFA), // brand purple
        Color(0xFFFF6B8A), // rose
        Color(0xFF4ADE80), // green
        Color(0xFF38BDF8), // sky
        Color(0xFFFBBF24), // amber
        Color(0xFFFB7185), // coral
        Color(0xFF2DD4BF), // teal
        Color(0xFFE879F9)  // fuchsia
    )

    fun colorAt(index: Int): Color = COLORS[index.mod(COLORS.size)]
}
```

- [ ] **Step 6: Crear el mapa de iconos**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitIcons.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons are stored as stable keys so the domain never depends on Compose. */
object HabitIcons {

    private val BY_KEY: Map<String, ImageVector> = mapOf(
        "dumbbell" to Icons.Filled.FitnessCenter,
        "book" to Icons.Filled.MenuBook,
        "self_improvement" to Icons.Filled.SelfImprovement,
        "water_drop" to Icons.Filled.WaterDrop,
        "bedtime" to Icons.Filled.Bedtime,
        "school" to Icons.Filled.School,
        "edit_note" to Icons.Filled.EditNote,
        "directions_walk" to Icons.Filled.DirectionsWalk
    )

    val ALL_KEYS: List<String> = BY_KEY.keys.toList()

    fun iconFor(key: String): ImageVector = BY_KEY[key] ?: Icons.Filled.CheckCircle
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/test/java/com/gondroid/quoteanime/presentation/routine
git commit -m "feat(routine): add habit palette, icons and heatmap geometry"
```

---

### Task 2: Composable del heatmap

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmap.kt`
- Test: `app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmapUiTest.kt`

**Interfaces:**
- Consumes: `HeatmapGrid`, `HabitPalette`, `GetActiveHabitsUseCase.VISIBLE_WEEKS`.
- Produces: `@Composable HabitHeatmap(completions: Set<LocalDate>, colorIndex: Int, today: LocalDate, startDate: LocalDate, endDate: LocalDate?, onDayClick: (LocalDate) -> Unit, modifier: Modifier)`.

- [ ] **Step 1: Implementar el composable**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmap.kt`:

```kotlin
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
                .pointerInput(gridStart, today, startDate, endDate) {
                    detectTapGestures { offset ->
                        val cell = HeatmapGrid.cellAt(offset.x, offset.y, cellPx, gapPx)
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
```

- [ ] **Step 2: Escribir el test de interacción**

Crear `app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmapUiTest.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - The grid renders
 *  - A tap on a past day inside the range reports a date
 *  - A tap on a day before startDate reports nothing
 */
@RunWith(AndroidJUnit4::class)
class HabitHeatmapUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-07-25")

    private fun setContent(startDate: LocalDate, onDayClick: (LocalDate) -> Unit) {
        composeRule.setContent {
            QuoteAnimeTheme {
                HabitHeatmap(
                    completions = emptySet(),
                    colorIndex = 0,
                    today = today,
                    startDate = startDate,
                    endDate = null,
                    onDayClick = onDayClick,
                    modifier = Modifier.width(340.dp)
                )
            }
        }
    }

    @Test
    fun heatmapIsDisplayed() {
        setContent(startDate = today.minusMonths(2)) { }

        composeRule.onNodeWithTag("habit_heatmap").assertIsDisplayed()
    }

    @Test
    fun tappingAPastDayInsideTheRangeReportsADate() {
        var clicked: LocalDate? = null
        setContent(startDate = today.minusMonths(3)) { clicked = it }

        composeRule.onNodeWithTag("habit_heatmap").performTouchInput { click(center) }

        assertTrue("expected a date from the center of the grid", clicked != null)
    }

    @Test
    fun tappingBeforeTheStartDateReportsNothing() {
        var clicked: LocalDate? = null
        // Habit starts today: every cell except today's is outside the range
        setContent(startDate = today) { clicked = it }

        composeRule.onNodeWithTag("habit_heatmap").performTouchInput { click(topLeft) }

        assertNull(clicked)
    }
}
```

- [ ] **Step 3: Ejecutar el test instrumentado**

```bash
./gradlew connectedAndroidTest --tests "com.gondroid.quoteanime.presentation.routine.HabitHeatmapUiTest"
```

Esperado: los 3 tests en verde.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmap.kt app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/HabitHeatmapUiTest.kt
git commit -m "feat(routine): add interactive contribution-style heatmap"
```

---

### Task 3: Estado y ViewModel de la rutina

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetGlobalStreakUseCase.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineUiState.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineViewModel.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/presentation/routine/RoutineViewModelTest.kt`

**Interfaces:**
- Consumes: `GetActiveHabitsUseCase`, `ToggleHabitCompletionUseCase`, `ArchiveHabitUseCase`, `PremiumGate`, `HabitRepository`, `CalculateStreakUseCase`.
- Produces: `RoutineUiState(habits, globalStreak, completedToday, totalHabits, canAddHabit, maxHabits, isLoading, message)`, `RoutineViewModel.uiState: StateFlow<RoutineUiState>`, y las acciones `onToggleDay(habitId, date)`, `onArchiveHabit(habitId)`, `onMessageShown()`.

- [ ] **Step 1: Escribir el test del ViewModel**

Crear `app/src/test/java/com/gondroid/quoteanime/presentation/routine/RoutineViewModelTest.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import app.cash.turbine.test
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - Habits and global streak reach the state
 *  - completedToday counts only habits completed today
 *  - canAddHabit turns false once the limit is reached
 *  - Toggling a day delegates to the use case
 *  - A rejected toggle surfaces a message
 *  - Archiving delegates to the use case
 */
class RoutineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.parse("2026-07-25")

    private lateinit var getActiveHabits: GetActiveHabitsUseCase
    private lateinit var getGlobalStreak: GetGlobalStreakUseCase
    private lateinit var toggleCompletion: ToggleHabitCompletionUseCase
    private lateinit var archiveHabit: ArchiveHabitUseCase
    private val premiumGate = PremiumGate()

    private fun habit(id: String) = Habit(
        id = id,
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(10)
    )

    private fun progress(id: String, completedToday: Boolean) = HabitWithProgress(
        habit = habit(id),
        completions = if (completedToday) setOf(today) else emptySet(),
        streak = StreakState(current = 1, best = 1, lastCompletedDate = today, completedToday = completedToday),
        completionRate = 0.5f
    )

    /** Pinned clock: the tests must not depend on the machine's current date. */
    private val fixedClock: Clock = Clock.fixed(
        LocalDate.parse("2026-07-25").atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private fun buildViewModel() = RoutineViewModel(
        getActiveHabits = getActiveHabits,
        getGlobalStreak = getGlobalStreak,
        toggleHabitCompletion = toggleCompletion,
        archiveHabit = archiveHabit,
        premiumGate = premiumGate,
        clock = fixedClock
    )

    @Before
    fun setup() {
        getActiveHabits = mockk()
        getGlobalStreak = mockk()
        toggleCompletion = mockk()
        archiveHabit = mockk()
        every { getGlobalStreak(any()) } returns flowOf(StreakState(current = 4, best = 9))
    }

    @Test
    fun `given habits, when state is collected, then they reach the state with the global streak`() = runTest {
        every { getActiveHabits(today) } returns flowOf(listOf(progress("h1", completedToday = true)))

        buildViewModel().uiState.test {
            skipItems(1) // initial loading state
            val state = awaitItem()
            assertEquals(1, state.habits.size)
            assertEquals(4, state.globalStreak.current)
            assertEquals(1, state.completedToday)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `given fewer habits than the limit, when state is collected, then adding is allowed`() = runTest {
        every { getActiveHabits(today) } returns flowOf(listOf(progress("h1", false)))

        buildViewModel().uiState.test {
            skipItems(1)
            assertTrue(awaitItem().canAddHabit)
        }
    }

    @Test
    fun `given the limit is reached, when state is collected, then adding is blocked`() = runTest {
        every { getActiveHabits(today) } returns flowOf(
            listOf(progress("h1", false), progress("h2", false), progress("h3", false))
        )

        buildViewModel().uiState.test {
            skipItems(1)
            assertFalse(awaitItem().canAddHabit)
        }
    }

    @Test
    fun `given a day, when toggled, then the use case is invoked with today as reference`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { toggleCompletion("h1", today, today) } returns ToggleCompletionResult.Success(true)

        val viewModel = buildViewModel()
        viewModel.onToggleDay("h1", today)
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleCompletion("h1", today, today) }
    }

    @Test
    fun `given a future day, when toggled, then a message is exposed`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        val future = today.plusDays(1)
        coEvery { toggleCompletion("h1", future, today) } returns ToggleCompletionResult.FutureDate

        val viewModel = buildViewModel()
        viewModel.onToggleDay("h1", future)
        advanceUntilIdle()

        assertEquals(RoutineMessage.FutureDayNotAllowed, viewModel.uiState.value.message)
    }

    @Test
    fun `given a habit id, when archived, then the use case is invoked`() = runTest {
        every { getActiveHabits(today) } returns flowOf(emptyList())
        coEvery { archiveHabit("h1") } returns Unit

        val viewModel = buildViewModel()
        viewModel.onArchiveHabit("h1")
        advanceUntilIdle()

        coVerify(exactly = 1) { archiveHabit("h1") }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.RoutineViewModelTest"
```

Esperado: FALLA con `Unresolved reference: GetGlobalStreakUseCase`.

- [ ] **Step 3: Crear el use case de racha global**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetGlobalStreakUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** A day counts for the global streak when at least one habit was completed. */
class GetGlobalStreakUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase
) {
    operator fun invoke(today: LocalDate): Flow<StreakState> =
        repository.getAllCompletionDates().map { dates -> calculateStreak(dates, today) }
}
```

- [ ] **Step 4: Crear el estado**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineUiState.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState

enum class RoutineMessage {
    FutureDayNotAllowed,
    OutsideHabitRange,
    HabitLimitReached
}

data class RoutineUiState(
    val habits: List<HabitWithProgress> = emptyList(),
    val globalStreak: StreakState = StreakState(),
    val isLoading: Boolean = true,
    val maxHabits: Int = 0,
    val message: RoutineMessage? = null
) {
    val completedToday: Int get() = habits.count { it.streak.completedToday }
    val totalHabits: Int get() = habits.size
    val canAddHabit: Boolean get() = habits.size < maxHabits
    val isEmpty: Boolean get() = !isLoading && habits.isEmpty()
}
```

- [ ] **Step 5: Crear el ViewModel**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineViewModel.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.usecase.ArchiveHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCase
import com.gondroid.quoteanime.domain.usecase.GetGlobalStreakUseCase
import com.gondroid.quoteanime.domain.usecase.ToggleCompletionResult
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val getActiveHabits: GetActiveHabitsUseCase,
    private val getGlobalStreak: GetGlobalStreakUseCase,
    private val toggleHabitCompletion: ToggleHabitCompletionUseCase,
    private val archiveHabit: ArchiveHabitUseCase,
    private val premiumGate: PremiumGate,
    /** Injected so tests can pin "today" instead of depending on the device clock. */
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineUiState(maxHabits = premiumGate.maxActiveHabits))
    val uiState: StateFlow<RoutineUiState> = _uiState.asStateFlow()

    private fun today(): LocalDate = LocalDate.now(clock)

    init {
        observeRoutine()
    }

    private fun observeRoutine() {
        val today = today()
        viewModelScope.launch {
            combine(
                getActiveHabits(today),
                getGlobalStreak(today)
            ) { habits, streak -> habits to streak }
                .collect { (habits, streak) ->
                    _uiState.update {
                        it.copy(
                            habits = habits,
                            globalStreak = streak,
                            isLoading = false,
                            maxHabits = premiumGate.maxActiveHabits
                        )
                    }
                }
        }
    }

    fun onToggleDay(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            when (toggleHabitCompletion(habitId, date, today())) {
                is ToggleCompletionResult.Success -> Unit
                ToggleCompletionResult.FutureDate ->
                    _uiState.update { it.copy(message = RoutineMessage.FutureDayNotAllowed) }
                ToggleCompletionResult.OutsideHabitRange ->
                    _uiState.update { it.copy(message = RoutineMessage.OutsideHabitRange) }
                ToggleCompletionResult.HabitNotFound -> Unit
            }
        }
    }

    fun onArchiveHabit(habitId: String) {
        viewModelScope.launch { archiveHabit(habitId) }
    }

    /** Consumed by the UI after showing the snackbar. */
    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}
```

- [ ] **Step 6: Proveer el reloj en Hilt**

`Clock` no tiene binding por defecto y Dagger ignora los valores por defecto de los constructores: sin este `@Provides`, la compilación de Hilt falla. En `di/AppModule.kt`, junto a los `@Provides` existentes:

```kotlin
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
```

Con el import `java.time.Clock`.

- [ ] **Step 7: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.RoutineViewModelTest"
```

Esperado: los 6 tests en verde.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetGlobalStreakUseCase.kt app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/main/java/com/gondroid/quoteanime/di/AppModule.kt app/src/test/java/com/gondroid/quoteanime/presentation/routine/RoutineViewModelTest.kt
git commit -m "feat(routine): add routine state and view model"
```

---

### Task 4: Pantalla de la rutina

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitCard.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/RoutineContentUiTest.kt`

**Interfaces:**
- Consumes: `RoutineUiState`, `RoutineViewModel`, `HabitHeatmap`, `HabitIcons`, `HabitPalette`.
- Produces: `@Composable RoutineScreen(viewModel, onAddHabit: () -> Unit, onEditHabit: (String) -> Unit)` y el composable sin estado `RoutineContent(state, today, onToggleToday, onToggleDay, onArchiveHabit, onAddHabit, onEditHabit, onMessageShown)`.

- [ ] **Step 1: Añadir los textos**

En `app/src/main/res/values/strings.xml`, añadir:

```xml
    <string name="routine_title">Mi rutina</string>
    <string name="routine_streak_days">%1$d días seguidos</string>
    <string name="routine_progress_today">%1$d de %2$d hoy</string>
    <string name="routine_empty_title">Empieza tu rutina</string>
    <string name="routine_empty_body">Elige hasta 3 hábitos y píntalos cada día que los cumplas.</string>
    <string name="routine_add_habit">Añadir hábito</string>
    <string name="routine_limit_reached">Has alcanzado el máximo de %1$d hábitos activos.</string>
    <string name="routine_current_streak">Racha: %1$d</string>
    <string name="routine_best_streak">Récord: %1$d</string>
    <string name="routine_completion_rate">Cumplimiento: %1$d%%</string>
    <string name="routine_mark_today">Marcar hoy</string>
    <string name="routine_unmark_today">Desmarcar hoy</string>
    <string name="routine_archive">Archivar</string>
    <string name="routine_edit">Editar</string>
    <string name="routine_message_future_day">No puedes marcar un día futuro.</string>
    <string name="routine_message_outside_range">Ese día está fuera del periodo del hábito.</string>
    <string name="template_train">Entrenar</string>
    <string name="template_read">Leer</string>
    <string name="template_meditate">Meditar</string>
    <string name="template_water">Beber agua</string>
    <string name="template_sleep_early">Dormir temprano</string>
    <string name="template_study">Estudiar</string>
    <string name="template_write">Escribir</string>
    <string name="template_walk">Caminar</string>
```

- [ ] **Step 2: Crear la tarjeta de hábito**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitCard.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
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
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
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
```

- [ ] **Step 3: Crear la pantalla**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineScreen.kt`:

```kotlin
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
        today = LocalDate.now(),
        onToggleToday = { habitId -> viewModel.onToggleDay(habitId, LocalDate.now()) },
        onToggleDay = viewModel::onToggleDay,
        onArchiveHabit = viewModel::onArchiveHabit,
        onAddHabit = onAddHabit,
        onEditHabit = onEditHabit,
        onMessageShown = viewModel::onMessageShown
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
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val futureMessage = stringResource(R.string.routine_message_future_day)
    val outsideMessage = stringResource(R.string.routine_message_outside_range)
    val limitMessage = stringResource(R.string.routine_limit_reached, state.maxHabits)

    LaunchedEffect(state.message) {
        val message = when (state.message) {
            RoutineMessage.FutureDayNotAllowed -> futureMessage
            RoutineMessage.OutsideHabitRange -> outsideMessage
            RoutineMessage.HabitLimitReached -> limitMessage
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
            onMessageShown = {}
        )
    }
}
```

- [ ] **Step 4: Escribir el test de la pantalla**

Crear `app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/RoutineContentUiTest.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.model.StreakState
import com.gondroid.quoteanime.ui.theme.QuoteAnimeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Empty state offers the create action
 *  - Habit cards render with the header
 *  - Marking today reports the habit id
 *  - The add button disappears once the limit is reached
 */
@RunWith(AndroidJUnit4::class)
class RoutineContentUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.parse("2026-07-25")

    private fun progress(id: String) = HabitWithProgress(
        habit = Habit(
            id = id,
            title = "Entrenar $id",
            iconKey = "dumbbell",
            colorIndex = 0,
            startDate = today.minusMonths(1)
        ),
        completions = setOf(today),
        streak = StreakState(1, 3, today, true),
        completionRate = 0.4f
    )

    private fun setContent(
        state: RoutineUiState,
        onToggleToday: (String) -> Unit = {},
        onAddHabit: () -> Unit = {}
    ) {
        composeRule.setContent {
            QuoteAnimeTheme {
                RoutineContent(
                    state = state,
                    today = today,
                    onToggleToday = onToggleToday,
                    onToggleDay = { _, _ -> },
                    onArchiveHabit = {},
                    onAddHabit = onAddHabit,
                    onEditHabit = {},
                    onMessageShown = {}
                )
            }
        }
    }

    @Test
    fun emptyStateOffersTheCreateAction() {
        var clicked = false
        setContent(RoutineUiState(isLoading = false, maxHabits = 3), onAddHabit = { clicked = true })

        composeRule.onNodeWithTag("empty_add_habit").assertIsDisplayed().performClick()

        assertTrue(clicked)
    }

    @Test
    fun habitCardsAndHeaderAreDisplayed() {
        setContent(
            RoutineUiState(
                habits = listOf(progress("h1")),
                globalStreak = StreakState(5, 9, today, true),
                isLoading = false,
                maxHabits = 3
            )
        )

        composeRule.onNodeWithTag("routine_header").assertIsDisplayed()
        composeRule.onNodeWithTag("habit_card_h1").assertIsDisplayed()
    }

    @Test
    fun markingTodayReportsTheHabitId() {
        var toggled: String? = null
        setContent(
            RoutineUiState(habits = listOf(progress("h1")), isLoading = false, maxHabits = 3),
            onToggleToday = { toggled = it }
        )

        composeRule.onNodeWithTag("toggle_today_h1").performClick()

        assertEquals("h1", toggled)
    }

    @Test
    fun addButtonDisappearsWhenTheLimitIsReached() {
        setContent(
            RoutineUiState(
                habits = listOf(progress("h1"), progress("h2"), progress("h3")),
                isLoading = false,
                maxHabits = 3
            )
        )

        // The FAB is not composed at all when the limit is reached, so the node must not exist
        composeRule.onNodeWithTag("add_habit_fab").assertDoesNotExist()
    }
}
```

- [ ] **Step 5: Ejecutar los tests**

```bash
./gradlew connectedAndroidTest --tests "com.gondroid.quoteanime.presentation.routine.RoutineContentUiTest"
```

Esperado: los 4 tests en verde.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/main/res/values/strings.xml app/src/androidTest/java/com/gondroid/quoteanime/presentation/routine/RoutineContentUiTest.kt
git commit -m "feat(routine): add routine screen with habit cards"
```

---

### Task 5: Editor de hábitos

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorSheet.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModel.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorUiState.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModelTest.kt`

**Interfaces:**
- Consumes: `GetHabitTemplatesUseCase`, `CreateHabitUseCase`, `UpdateHabitUseCase`, `HabitRepository` (para cargar el hábito en edición), `HabitPalette`, `HabitIcons`.
- Produces: `HabitEditorUiState`, `HabitEditorViewModel` con `onTitleChanged`, `onTemplateSelected`, `onColorSelected`, `onStartDateChanged`, `onEndDateChanged`, `onReminderToggled`, `onReminderTimeChanged`, `onReminderDayToggled`, `onSave`; y `@Composable HabitEditorSheet(habitId: String?, onDismiss: () -> Unit)`.

- [ ] **Step 1: Añadir los textos**

En `app/src/main/res/values/strings.xml`:

```xml
    <string name="habit_editor_new_title">Nuevo hábito</string>
    <string name="habit_editor_edit_title">Editar hábito</string>
    <string name="habit_editor_name">Nombre del hábito</string>
    <string name="habit_editor_templates">Sugerencias</string>
    <string name="habit_editor_color">Color</string>
    <string name="habit_editor_start_date">Fecha de inicio</string>
    <string name="habit_editor_end_date">Fecha de fin</string>
    <string name="habit_editor_has_end_date">Ponerle fecha de fin</string>
    <string name="habit_editor_reminder">Recordatorio</string>
    <string name="habit_editor_reminder_time">Hora</string>
    <string name="habit_editor_reminder_days">Días</string>
    <string name="habit_editor_save">Guardar</string>
    <string name="habit_editor_cancel">Cancelar</string>
    <string name="habit_editor_error_blank">Ponle un nombre al hábito.</string>
    <string name="habit_editor_error_dates">La fecha de fin debe ser posterior a la de inicio.</string>
    <string name="habit_editor_error_limit">Ya tienes %1$d hábitos activos. Archiva uno para crear otro.</string>
```

- [ ] **Step 2: Escribir el test del ViewModel del editor**

Crear `app/src/test/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModelTest.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateHabitResult
import com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCase
import com.gondroid.quoteanime.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Scenarios covered:
 *  - Templates are loaded into the state
 *  - Selecting a template fills title and icon
 *  - Saving a new habit calls CreateHabitUseCase
 *  - Reaching the limit surfaces an error and does not close the sheet
 *  - Editing an existing habit loads it and calls UpdateHabitUseCase
 *  - Turning the reminder off clears time and days
 */
class HabitEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getTemplates: GetHabitTemplatesUseCase
    private lateinit var createHabit: CreateHabitUseCase
    private lateinit var updateHabit: UpdateHabitUseCase
    private lateinit var repository: HabitRepository

    private val today = LocalDate.parse("2026-07-25")

    private val fixedClock: Clock = Clock.fixed(
        LocalDate.parse("2026-07-25").atStartOfDay(ZoneOffset.UTC).toInstant(),
        ZoneOffset.UTC
    )

    private fun buildViewModel(habitId: String? = null) = HabitEditorViewModel(
        savedStateHandle = SavedStateHandle(mapOf("habitId" to habitId)),
        getHabitTemplates = getTemplates,
        createHabit = createHabit,
        updateHabit = updateHabit,
        repository = repository,
        clock = fixedClock
    )

    @Before
    fun setup() {
        getTemplates = mockk()
        createHabit = mockk()
        updateHabit = mockk()
        repository = mockk()
        every { getTemplates() } returns flowOf(DefaultHabitTemplates.ALL)
    }

    @Test
    fun `given the editor opens, when templates load, then they reach the state`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(DefaultHabitTemplates.ALL, viewModel.uiState.value.templates)
    }

    @Test
    fun `given a template, when selected, then title and icon are filled`() = runTest {
        val viewModel = buildViewModel()
        advanceUntilIdle()
        val template = DefaultHabitTemplates.ALL.first()

        viewModel.onTemplateSelected(template)

        assertEquals(template.title, viewModel.uiState.value.title)
        assertEquals(template.iconKey, viewModel.uiState.value.iconKey)
        assertEquals(template.id, viewModel.uiState.value.templateId)
    }

    @Test
    fun `given a new habit, when saved, then it is created`() = runTest {
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.Success(
                Habit("h1", "Leer", "book", 0, today)
            )
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")

        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 1) { createHabit(any(), any(), any(), any(), any(), any(), any(), any()) }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `given the limit is reached, when saving, then an error is exposed and it is not saved`() = runTest {
        coEvery { createHabit(any(), any(), any(), any(), any(), any(), any(), any()) } returns
            CreateHabitResult.LimitReached(3)
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(HabitEditorError.LimitReached(3), viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isSaved)
    }

    @Test
    fun `given an existing habit id, when the editor opens, then it is loaded and updated on save`() = runTest {
        val existing = Habit(
            id = "h1",
            title = "Entrenar",
            iconKey = "dumbbell",
            colorIndex = 2,
            startDate = today.minusDays(5),
            reminderTime = LocalTime.of(7, 0),
            reminderDays = setOf(DayOfWeek.MONDAY)
        )
        coEvery { repository.getHabit("h1") } returns existing
        coEvery { updateHabit(any()) } returns UpdateHabitResult.Success(existing)

        val viewModel = buildViewModel(habitId = "h1")
        advanceUntilIdle()
        assertEquals("Entrenar", viewModel.uiState.value.title)

        viewModel.onSave()
        advanceUntilIdle()

        coVerify(exactly = 1) { updateHabit(any()) }
    }

    @Test
    fun `given the reminder is turned off, when saving, then time and days are cleared`() = runTest {
        var captured: Set<DayOfWeek>? = null
        coEvery {
            createHabit(any(), any(), any(), any(), any(), null, any(), any())
        } answers {
            captured = arg(6)
            CreateHabitResult.Success(Habit("h1", "Leer", "book", 0, today))
        }
        val viewModel = buildViewModel()
        advanceUntilIdle()
        viewModel.onTitleChanged("Leer")
        viewModel.onReminderToggled(true)
        viewModel.onReminderDayToggled(DayOfWeek.MONDAY)
        viewModel.onReminderToggled(false)

        viewModel.onSave()
        advanceUntilIdle()

        assertNotNull(captured)
        assertEquals(emptySet<DayOfWeek>(), captured)
    }
}
```

- [ ] **Step 3: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.HabitEditorViewModelTest"
```

Esperado: FALLA con `Unresolved reference: HabitEditorViewModel`.

- [ ] **Step 4: Crear el estado del editor**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorUiState.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import com.gondroid.quoteanime.domain.model.HabitTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface HabitEditorError {
    data object BlankTitle : HabitEditorError
    data object InvalidDateRange : HabitEditorError
    data class LimitReached(val max: Int) : HabitEditorError
}

data class HabitEditorUiState(
    val habitId: String? = null,
    val templates: List<HabitTemplate> = emptyList(),
    val title: String = "",
    val iconKey: String = "dumbbell",
    val templateId: String? = null,
    val colorIndex: Int = 0,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val reminderDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val error: HabitEditorError? = null,
    val isSaved: Boolean = false
) {
    val isEditing: Boolean get() = habitId != null
}
```

- [ ] **Step 5: Crear el ViewModel del editor**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModel.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.CreateHabitResult
import com.gondroid.quoteanime.domain.usecase.CreateHabitUseCase
import com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCase
import com.gondroid.quoteanime.domain.usecase.UpdateHabitResult
import com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class HabitEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHabitTemplates: GetHabitTemplatesUseCase,
    private val createHabit: CreateHabitUseCase,
    private val updateHabit: UpdateHabitUseCase,
    private val repository: HabitRepository,
    private val clock: Clock
) : ViewModel() {

    private val editedHabitId: String? = savedStateHandle["habitId"]

    private val _uiState = MutableStateFlow(
        HabitEditorUiState(habitId = editedHabitId, startDate = LocalDate.now(clock))
    )
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        editedHabitId?.let(::loadHabit)
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            getHabitTemplates().collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    private fun loadHabit(habitId: String) {
        viewModelScope.launch {
            val habit = repository.getHabit(habitId) ?: return@launch
            _uiState.update {
                it.copy(
                    title = habit.title,
                    iconKey = habit.iconKey,
                    templateId = habit.templateId,
                    colorIndex = habit.colorIndex,
                    startDate = habit.startDate,
                    endDate = habit.endDate,
                    reminderEnabled = habit.reminderTime != null,
                    reminderTime = habit.reminderTime ?: it.reminderTime,
                    reminderDays = habit.reminderDays.ifEmpty { it.reminderDays }
                )
            }
        }
    }

    fun onTitleChanged(title: String) =
        _uiState.update { it.copy(title = title, error = null) }

    fun onTemplateSelected(template: HabitTemplate) = _uiState.update {
        it.copy(
            title = template.title,
            iconKey = template.iconKey,
            templateId = template.id,
            error = null
        )
    }

    fun onColorSelected(colorIndex: Int) = _uiState.update { it.copy(colorIndex = colorIndex) }

    fun onStartDateChanged(date: LocalDate) =
        _uiState.update { it.copy(startDate = date, error = null) }

    fun onEndDateChanged(date: LocalDate?) =
        _uiState.update { it.copy(endDate = date, error = null) }

    fun onReminderToggled(enabled: Boolean) =
        _uiState.update { it.copy(reminderEnabled = enabled) }

    fun onReminderTimeChanged(time: LocalTime) =
        _uiState.update { it.copy(reminderTime = time) }

    fun onReminderDayToggled(day: DayOfWeek) = _uiState.update { state ->
        val days = if (day in state.reminderDays) state.reminderDays - day else state.reminderDays + day
        state.copy(reminderDays = days)
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    fun onSave() {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.isEditing) saveExisting(state) else saveNew(state)
        }
    }

    private suspend fun saveNew(state: HabitEditorUiState) {
        val result = createHabit(
            title = state.title,
            iconKey = state.iconKey,
            colorIndex = state.colorIndex,
            startDate = state.startDate,
            endDate = state.endDate,
            reminderTime = if (state.reminderEnabled) state.reminderTime else null,
            reminderDays = if (state.reminderEnabled) state.reminderDays else emptySet(),
            templateId = state.templateId
        )
        when (result) {
            is CreateHabitResult.Success -> _uiState.update { it.copy(isSaved = true) }
            is CreateHabitResult.LimitReached ->
                _uiState.update { it.copy(error = HabitEditorError.LimitReached(result.max)) }
            CreateHabitResult.BlankTitle ->
                _uiState.update { it.copy(error = HabitEditorError.BlankTitle) }
            CreateHabitResult.InvalidDateRange ->
                _uiState.update { it.copy(error = HabitEditorError.InvalidDateRange) }
        }
    }

    private suspend fun saveExisting(state: HabitEditorUiState) {
        val existing = repository.getHabit(state.habitId!!) ?: return
        val edited = existing.copy(
            title = state.title,
            iconKey = state.iconKey,
            colorIndex = state.colorIndex,
            startDate = state.startDate,
            endDate = state.endDate,
            reminderTime = if (state.reminderEnabled) state.reminderTime else null,
            reminderDays = if (state.reminderEnabled) state.reminderDays else emptySet(),
            templateId = state.templateId
        )
        when (updateHabit(edited)) {
            is UpdateHabitResult.Success -> _uiState.update { it.copy(isSaved = true) }
            UpdateHabitResult.BlankTitle ->
                _uiState.update { it.copy(error = HabitEditorError.BlankTitle) }
            UpdateHabitResult.InvalidDateRange ->
                _uiState.update { it.copy(error = HabitEditorError.InvalidDateRange) }
            UpdateHabitResult.HabitNotFound -> Unit
        }
    }
}
```

- [ ] **Step 6: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.presentation.routine.HabitEditorViewModelTest"
```

Esperado: los 6 tests en verde.

- [ ] **Step 7: Crear la hoja del editor**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorSheet.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorSheet(
    onDismiss: () -> Unit,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .testTag("habit_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.habit_editor_edit_title
                    else R.string.habit_editor_new_title
                ),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(stringResource(R.string.habit_editor_templates))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.templates.forEach { template ->
                    FilterChip(
                        selected = state.templateId == template.id,
                        onClick = { viewModel.onTemplateSelected(template) },
                        label = { Text(template.title) }
                    )
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.habit_editor_name)) },
                singleLine = true,
                isError = state.error is HabitEditorError.BlankTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_field")
            )

            Text(stringResource(R.string.habit_editor_color))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitPalette.COLORS.forEachIndexed { index, color ->
                    Column(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { viewModel.onColorSelected(index) }
                            .testTag("color_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .size(28.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (state.colorIndex == index) 2.dp else 0.dp,
                                    color = if (state.colorIndex == index) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {}
                    }
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_start_date)) },
                supportingContent = { Text(state.startDate.toString()) },
                modifier = Modifier.clickable { showStartPicker = true }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_has_end_date)) },
                trailingContent = {
                    Switch(
                        checked = state.endDate != null,
                        onCheckedChange = { checked ->
                            if (checked) showEndPicker = true else viewModel.onEndDateChanged(null)
                        }
                    )
                }
            )
            if (state.endDate != null) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_end_date)) },
                    supportingContent = { Text(state.endDate.toString()) },
                    modifier = Modifier.clickable { showEndPicker = true }
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.habit_editor_reminder)) },
                trailingContent = {
                    Switch(
                        checked = state.reminderEnabled,
                        onCheckedChange = viewModel::onReminderToggled,
                        modifier = Modifier.testTag("reminder_switch")
                    )
                }
            )

            if (state.reminderEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.habit_editor_reminder_time)) },
                    supportingContent = { Text(state.reminderTime.toString()) },
                    modifier = Modifier.clickable { showTimePicker = true }
                )
                Text(stringResource(R.string.habit_editor_reminder_days))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in state.reminderDays,
                            onClick = { viewModel.onReminderDayToggled(day) },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            }
                        )
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text = when (error) {
                        HabitEditorError.BlankTitle ->
                            stringResource(R.string.habit_editor_error_blank)
                        HabitEditorError.InvalidDateRange ->
                            stringResource(R.string.habit_editor_error_dates)
                        is HabitEditorError.LimitReached ->
                            stringResource(R.string.habit_editor_error_limit, error.max)
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = viewModel::onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_save_button")
            ) {
                Text(stringResource(R.string.habit_editor_save))
            }
        }
    }

    if (showStartPicker) {
        DatePickerModal(
            initialDate = state.startDate,
            onDateSelected = { viewModel.onStartDateChanged(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        DatePickerModal(
            initialDate = state.endDate ?: state.startDate,
            onDateSelected = { viewModel.onEndDateChanged(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }
    if (showTimePicker) {
        TimePickerModal(
            initialTime = state.reminderTime,
            onTimeSelected = { viewModel.onReminderTimeChanged(it); showTimePicker = false },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onDateSelected(
                        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    )
                }
            }) { Text(stringResource(R.string.habit_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.habit_editor_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerModal(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
            }) { Text(stringResource(R.string.habit_editor_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.habit_editor_cancel))
            }
        }
    ) {
        TimePicker(state = pickerState)
    }
}
```

- [ ] **Step 8: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/main/res/values/strings.xml app/src/test/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModelTest.kt
git commit -m "feat(routine): add habit editor sheet"
```

---

### Task 6: Navegación con barra inferior

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/presentation/navigation/BottomNavBar.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/presentation/navigation/AppNavGraph.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `RoutineScreen`, `HabitEditorSheet`.
- Produces: `Screen.Routine` (`routine`), `Screen.HabitEditor` con `routeWithArg = "habit_editor?habitId={habitId}"` y `createRoute(habitId: String?)`, y el composable `BottomNavBar(currentRoute, onNavigate)`.

- [ ] **Step 1: Añadir los textos**

En `app/src/main/res/values/strings.xml`:

```xml
    <string name="nav_quotes">Frases</string>
    <string name="nav_routine">Mi rutina</string>
    <string name="nav_catalog">Catálogo</string>
```

- [ ] **Step 2: Crear la barra inferior**

Crear `app/src/main/java/com/gondroid/quoteanime/presentation/navigation/BottomNavBar.kt`:

```kotlin
package com.gondroid.quoteanime.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.gondroid.quoteanime.R

enum class BottomTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    QUOTES(Screen.Home.route, R.string.nav_quotes, Icons.Filled.FormatQuote),
    ROUTINE(Screen.Routine.route, R.string.nav_routine, Icons.Filled.LocalFireDepartment),
    CATALOG(Screen.Catalog.route, R.string.nav_catalog, Icons.AutoMirrored.Filled.MenuBook)
}

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute?.startsWith(tab.route) == true,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) }
            )
        }
    }
}
```

- [ ] **Step 3: Añadir las rutas nuevas**

En `AppNavGraph.kt`, dentro de `sealed class Screen`, añadir:

```kotlin
    data object Routine : Screen("routine")
    data object HabitEditor : Screen("habit_editor") {
        const val ARG = "habitId"
        val routeWithArg = "habit_editor?$ARG={$ARG}"
        fun createRoute(habitId: String?) =
            if (habitId != null) "habit_editor?$ARG=$habitId" else "habit_editor"
    }
```

- [ ] **Step 4: Envolver el grafo en un Scaffold con barra inferior**

Sustituir la función `AppNavGraph` por:

```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startQuoteId: String? = null
) {
    // If app was opened via widget tap, skip splash/onboarding and go directly to Home
    val start = if (startQuoteId != null) Screen.Home.createRoute(startQuoteId)
                else Screen.Splash.route

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomTab.entries.any { currentRoute?.startsWith(it.route) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {
            // …existing composable(…) blocks stay unchanged…

            composable(Screen.Routine.route) {
                RoutineScreen(
                    onAddHabit = {
                        navController.navigate(Screen.HabitEditor.createRoute(null))
                    },
                    onEditHabit = { habitId ->
                        navController.navigate(Screen.HabitEditor.createRoute(habitId))
                    }
                )
            }

            composable(
                route = Screen.HabitEditor.routeWithArg,
                arguments = listOf(
                    navArgument(Screen.HabitEditor.ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                HabitEditorSheet(onDismiss = { navController.popBackStack() })
            }
        }
    }
}
```

Imports nuevos que hay que añadir al archivo:

```kotlin
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.gondroid.quoteanime.presentation.routine.HabitEditorSheet
import com.gondroid.quoteanime.presentation.routine.RoutineScreen
```

- [ ] **Step 5: Verificar en el emulador**

```bash
./gradlew installDebug
```

Comprobar a mano: la barra inferior aparece en Frases, Mi rutina y Catálogo; no aparece en splash, onboarding, ajustes ni en el tutorial del widget; el deep link del widget sigue abriendo la frase correcta.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/presentation/navigation app/src/main/res/values/strings.xml
git commit -m "feat(navigation): add bottom navigation with routine tab"
```

---

### Task 7: Recordatorios por hábito

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/notification/NextReminderCalculator.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/notification/HabitReminderScheduler.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/notification/HabitReminderReceiver.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/worker/HabitReminderWorker.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/notification/NotificationHelper.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/HabitEditorViewModel.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/gondroid/quoteanime/notification/NextReminderCalculatorTest.kt`

**Interfaces:**
- Consumes: `Habit`, `GetRandomQuoteUseCase`, `ToggleHabitCompletionUseCase`, `HabitRepository`.
- Produces: `NextReminderCalculator.nextOccurrence(from: LocalDateTime, time: LocalTime, days: Set<DayOfWeek>): LocalDateTime?`, `HabitReminderScheduler.schedule(habit: Habit)` y `.cancel(habitId: String)`.

- [ ] **Step 1: Escribir el test del cálculo**

Crear `app/src/test/java/com/gondroid/quoteanime/notification/NextReminderCalculatorTest.kt`:

```kotlin
package com.gondroid.quoteanime.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Today is a reminder day and the time has not passed yet
 *  - Today is a reminder day but the time already passed
 *  - The next reminder day is later this week
 *  - The next reminder day wraps into next week
 *  - Every day selected
 *  - No days selected
 */
class NextReminderCalculatorTest {

    // Saturday 2026-07-25 at 10:00
    private val saturdayMorning = LocalDateTime.parse("2026-07-25T10:00")

    @Test
    fun `given today is selected and the time has not passed, when calculated, then it is today`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(18, 0),
            days = setOf(DayOfWeek.SATURDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-25T18:00"), next)
    }

    @Test
    fun `given today is selected but the time passed, when calculated, then it is next week`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.SATURDAY)
        )

        assertEquals(LocalDateTime.parse("2026-08-01T07:00"), next)
    }

    @Test
    fun `given a later day this week, when calculated, then that day is returned`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.SUNDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-26T07:00"), next)
    }

    @Test
    fun `given only earlier weekdays, when calculated, then it wraps into next week`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        )

        assertEquals(LocalDateTime.parse("2026-07-27T07:00"), next)
    }

    @Test
    fun `given every day selected and the time passed, when calculated, then it is tomorrow`() {
        val next = NextReminderCalculator.nextOccurrence(
            from = saturdayMorning,
            time = LocalTime.of(7, 0),
            days = DayOfWeek.entries.toSet()
        )

        assertEquals(LocalDateTime.parse("2026-07-26T07:00"), next)
    }

    @Test
    fun `given no days selected, when calculated, then there is no next occurrence`() {
        assertNull(
            NextReminderCalculator.nextOccurrence(
                from = saturdayMorning,
                time = LocalTime.of(7, 0),
                days = emptySet()
            )
        )
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.notification.NextReminderCalculatorTest"
```

Esperado: FALLA con `Unresolved reference: NextReminderCalculator`.

- [ ] **Step 3: Implementar el cálculo**

Crear `app/src/main/java/com/gondroid/quoteanime/notification/NextReminderCalculator.kt`:

```kotlin
package com.gondroid.quoteanime.notification

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure calculation of the next reminder instant. Kept separate from WorkManager
 * so the weekday logic can be unit tested.
 */
object NextReminderCalculator {

    fun nextOccurrence(
        from: LocalDateTime,
        time: LocalTime,
        days: Set<DayOfWeek>
    ): LocalDateTime? {
        if (days.isEmpty()) return null

        for (offset in 0..7) {
            val candidate = from.toLocalDate().plusDays(offset.toLong()).atTime(time)
            if (candidate.dayOfWeek in days && candidate.isAfter(from)) return candidate
        }
        return null
    }
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.notification.NextReminderCalculatorTest"
```

Esperado: los 6 tests en verde.

- [ ] **Step 5: Añadir el canal y la notificación de hábito**

En `app/src/main/res/values/strings.xml`:

```xml
    <string name="habit_channel_name">Recordatorios de hábitos</string>
    <string name="habit_channel_description">Avisos para cumplir los hábitos de tu rutina</string>
    <string name="habit_notification_action_done">Hecho</string>
</resources>
```

En `NotificationHelper.kt`, añadir el canal nuevo y el método de notificación. Mantener intactos el canal y el método existentes de frases:

```kotlin
    companion object {
        const val HABIT_CHANNEL_ID = "habit_reminders"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    /** Separate channel so users can mute habit reminders without losing quotes. */
    private fun createHabitChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            HABIT_CHANNEL_ID,
            context.getString(R.string.habit_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.habit_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showHabitReminder(habitId: String, habitTitle: String, quoteText: String) {
        createHabitChannel()
        val notificationId = habitId.hashCode()

        val doneIntent = Intent(context, HabitReminderReceiver::class.java).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, HABIT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habitTitle)
            .setContentText(quoteText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quoteText))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.habit_notification_action_done),
                donePendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }
```

Comprobar el nombre real del icono y del `notificationManager` en el archivo existente y reutilizar los mismos; no introducir un icono nuevo.

- [ ] **Step 6: Crear el receiver de la acción «Hecho»**

Crear `app/src/main/java/com/gondroid/quoteanime/notification/HabitReminderReceiver.kt`:

```kotlin
package com.gondroid.quoteanime.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Handles the "Done" action so the habit can be marked without opening the app.
 */
@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var toggleHabitCompletion: ToggleHabitCompletionUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_ID) ?: return
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = LocalDate.now()
                toggleHabitCompletion(habitId, today, today)
                context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 7: Crear el worker que notifica y se reprograma**

Crear `app/src/main/java/com/gondroid/quoteanime/worker/HabitReminderWorker.kt`:

```kotlin
package com.gondroid.quoteanime.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gondroid.quoteanime.domain.repository.HabitRepository
import com.gondroid.quoteanime.domain.usecase.GetRandomQuoteUseCase
import com.gondroid.quoteanime.notification.HabitReminderScheduler
import com.gondroid.quoteanime.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Shows one habit reminder and immediately schedules the next occurrence:
 * WorkManager has no weekday-aware periodic work, so the chain is rebuilt each run.
 */
@HiltWorker
class HabitReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HabitRepository,
    private val getRandomQuote: GetRandomQuoteUseCase,
    private val notificationHelper: NotificationHelper,
    private val scheduler: HabitReminderScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val habitId = inputData.getString(KEY_HABIT_ID) ?: return Result.failure()
        val habit = repository.getHabit(habitId) ?: return Result.success()
        if (habit.isArchived || habit.reminderTime == null) return Result.success()

        val today = LocalDate.now()
        val shouldNotify = habit.isActiveOn(today) && !repository.isCompleted(habitId, today)

        if (shouldNotify) {
            val quote = runCatching { getRandomQuote(emptySet()) }.getOrNull()
            notificationHelper.showHabitReminder(
                habitId = habit.id,
                habitTitle = habit.title,
                quoteText = quote?.quote.orEmpty()
            )
        }

        scheduler.schedule(habit)
        return Result.success()
    }

    companion object {
        const val KEY_HABIT_ID = "habit_id"
    }
}
```

- [ ] **Step 8: Crear el programador**

Crear `app/src/main/java/com/gondroid/quoteanime/notification/HabitReminderScheduler.kt`:

```kotlin
package com.gondroid.quoteanime.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.worker.HabitReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One uniquely named chained work item per habit. Exact alarms are deliberately
 * avoided: the same tolerance already accepted for quote notifications applies.
 */
@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(habit: Habit) {
        val reminderTime = habit.reminderTime
        if (habit.isArchived || reminderTime == null || habit.reminderDays.isEmpty()) {
            cancel(habit.id)
            return
        }

        val now = LocalDateTime.now()
        val next = NextReminderCalculator.nextOccurrence(now, reminderTime, habit.reminderDays)
            ?: return
        if (habit.endDate != null && next.toLocalDate().isAfter(habit.endDate)) {
            cancel(habit.id)
            return
        }

        val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(HabitReminderWorker.KEY_HABIT_ID, habit.id).build())
            .build()

        workManager.enqueueUniqueWork(workName(habit.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(habitId: String) {
        workManager.cancelUniqueWork(workName(habitId))
    }

    private fun workName(habitId: String) = "$WORK_PREFIX$habitId"

    companion object {
        private const val WORK_PREFIX = "habit_reminder_"
    }
}
```

- [ ] **Step 9: Programar desde el editor y al archivar**

En `HabitEditorViewModel`, inyectar `private val reminderScheduler: HabitReminderScheduler` y programar tras guardar, siguiendo el patrón de `SettingsViewModel` (escribir y reprogramar en la misma acción):

```kotlin
            is CreateHabitResult.Success -> {
                reminderScheduler.schedule(result.habit)
                _uiState.update { it.copy(isSaved = true) }
            }
```

```kotlin
            is UpdateHabitResult.Success -> {
                reminderScheduler.schedule(edited)
                _uiState.update { it.copy(isSaved = true) }
            }
```

En `RoutineViewModel`, inyectar el mismo scheduler y cancelar al archivar:

```kotlin
    fun onArchiveHabit(habitId: String) {
        viewModelScope.launch {
            archiveHabit(habitId)
            reminderScheduler.cancel(habitId)
        }
    }
```

Actualizar las construcciones del ViewModel en `RoutineViewModelTest` y `HabitEditorViewModelTest` para pasar un `mockk<HabitReminderScheduler>(relaxed = true)`.

- [ ] **Step 10: Registrar el receiver en el manifiesto**

En `AndroidManifest.xml`, dentro de `<application>`:

```xml
        <receiver
            android:name=".notification.HabitReminderReceiver"
            android:exported="false" />
```

- [ ] **Step 11: Ejecutar los tests y verificar a mano**

```bash
./gradlew test assembleDebug
```

Esperado: `BUILD SUCCESSFUL` con todos los tests en verde.

Verificación manual: crear un hábito con recordatorio dentro de los próximos 2 minutos, esperar la notificación, pulsar «Hecho», y comprobar que el día aparece marcado en el heatmap y que la notificación desaparece.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/notification app/src/main/java/com/gondroid/quoteanime/worker/HabitReminderWorker.kt app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/test/java/com/gondroid/quoteanime/notification
git commit -m "feat(routine): add per-habit reminders with a done action"
```

---

### Task 8: Medición

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/analytics/RoutineAnalytics.kt`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Modify: `RoutineViewModel.kt`, `HabitEditorViewModel.kt`

**Interfaces:**
- Consumes: `FirebaseAnalytics`.
- Produces: `RoutineAnalytics` con `trackTabOpened()`, `trackHabitCreated(templateId, isCustom, hasReminder, hasEndDate)`, `trackHabitCompleted(habitId, isRetroactive, source)`, `trackHabitArchived(daysActive)`, `trackStreakMilestone(days)`, `trackStreakBroken(previousStreak)`.

- [ ] **Step 1: Añadir la dependencia**

En `gradle/libs.versions.toml`, dentro de `[libraries]`:

```toml
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
```

En `app/build.gradle.kts`, junto al resto de Firebase:

```kotlin
    implementation(libs.firebase.analytics)
```

- [ ] **Step 2: Crear el envoltorio de analítica**

Crear `app/src/main/java/com/gondroid/quoteanime/analytics/RoutineAnalytics.kt`:

```kotlin
package com.gondroid.quoteanime.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where routine events are named, so the dashboards stay stable
 * even if call sites move.
 */
@Singleton
class RoutineAnalytics @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun trackTabOpened() = analytics.logEvent("routine_tab_opened", Bundle())

    fun trackHabitCreated(
        templateId: String?,
        isCustom: Boolean,
        hasReminder: Boolean,
        hasEndDate: Boolean
    ) = analytics.logEvent(
        "habit_created",
        Bundle().apply {
            putString("template_id", templateId ?: "custom")
            putBoolean("is_custom", isCustom)
            putBoolean("has_reminder", hasReminder)
            putBoolean("has_end_date", hasEndDate)
        }
    )

    fun trackHabitCompleted(habitId: String, isRetroactive: Boolean, source: String) =
        analytics.logEvent(
            "habit_completed",
            Bundle().apply {
                putString("habit_id", habitId)
                putBoolean("is_retroactive", isRetroactive)
                putString("source", source)
            }
        )

    fun trackHabitArchived(daysActive: Long) = analytics.logEvent(
        "habit_archived",
        Bundle().apply { putLong("days_active", daysActive) }
    )

    fun trackStreakMilestone(days: Int) = analytics.logEvent(
        "streak_milestone",
        Bundle().apply { putInt("days", days) }
    )

    fun trackStreakBroken(previousStreak: Int) = analytics.logEvent(
        "streak_broken",
        Bundle().apply { putInt("previous_streak", previousStreak) }
    )

    companion object {
        const val SOURCE_APP = "app"
        const val SOURCE_NOTIFICATION = "notification"
    }
}
```

- [ ] **Step 3: Proveer FirebaseAnalytics en Hilt**

En `di/AppModule.kt`, junto al `@Provides` existente de `FirebaseDatabase`:

```kotlin
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
```

- [ ] **Step 4: Registrar los eventos**

En `RoutineViewModel`: inyectar `private val analytics: RoutineAnalytics`, llamar a `analytics.trackTabOpened()` en el bloque `init`, y en `onToggleDay`, dentro de la rama `Success`, registrar:

```kotlin
                is ToggleCompletionResult.Success -> {
                    if (result.completed) {
                        analytics.trackHabitCompleted(
                            habitId = habitId,
                            isRetroactive = date != today(),
                            source = RoutineAnalytics.SOURCE_APP
                        )
                    }
                }
```

Para acceder a `result` hay que asignar el `when` a una variable: `val result = toggleHabitCompletion(habitId, date, today())`.

En `HabitEditorViewModel`, dentro de la rama `CreateHabitResult.Success`:

```kotlin
                analytics.trackHabitCreated(
                    templateId = state.templateId,
                    isCustom = state.templateId == null,
                    hasReminder = state.reminderEnabled,
                    hasEndDate = state.endDate != null
                )
```

Añadir `mockk<RoutineAnalytics>(relaxed = true)` a las construcciones de ambos ViewModels en sus tests.

- [ ] **Step 5: Ejecutar los tests**

```bash
./gradlew test
```

Esperado: toda la suite en verde.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/gondroid/quoteanime/analytics app/src/main/java/com/gondroid/quoteanime/di app/src/main/java/com/gondroid/quoteanime/presentation/routine app/src/test/java/com/gondroid/quoteanime/presentation/routine
git commit -m "feat(routine): track routine events with Firebase Analytics"
```

---

### Task 9: Descubrimiento de la feature

**Files:**
- Modify: `app/src/main/java/com/gondroid/quoteanime/data/local/datastore/UserPreferencesDataStore.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/RoutineIntroUseCases.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/presentation/routine/RoutineScreen.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/presentation/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `UserPreferencesDataStore`.
- Produces: `IsRoutineIntroSeenUseCase(): Flow<Boolean>`, `SetRoutineIntroSeenUseCase()`.

- [ ] **Step 1: Añadir la clave en DataStore**

En `UserPreferencesDataStore.kt`, dentro de `Keys`:

```kotlin
        val ROUTINE_INTRO_SEEN = booleanPreferencesKey("routine_intro_seen")
```

Y al final de la clase:

```kotlin
    val isRoutineIntroSeen: Flow<Boolean> =
        dataStore.data.map { it[Keys.ROUTINE_INTRO_SEEN] ?: false }

    suspend fun setRoutineIntroSeen() {
        dataStore.edit { it[Keys.ROUTINE_INTRO_SEEN] = true }
    }
```

- [ ] **Step 2: Crear los use cases**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/RoutineIntroUseCases.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsRoutineIntroSeenUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    operator fun invoke(): Flow<Boolean> = dataStore.isRoutineIntroSeen
}

class SetRoutineIntroSeenUseCase @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {
    suspend operator fun invoke() = dataStore.setRoutineIntroSeen()
}
```

- [ ] **Step 3: Añadir los textos**

En `app/src/main/res/values/strings.xml`:

```xml
    <string name="routine_intro_title">Nuevo: Mi rutina</string>
    <string name="routine_intro_body">Elige hasta 3 hábitos y píntalos cada día que los cumplas. Tu racha te espera.</string>
    <string name="routine_intro_action">Empezar</string>
    <string name="routine_intro_dismiss">Ahora no</string>
    <string name="onboarding_habit_title">¿Quieres empezar un hábito?</string>
    <string name="onboarding_habit_skip">Saltar</string>
```

- [ ] **Step 4: Mostrar el aviso una sola vez**

En `RoutineViewModel`, añadir al estado `val showIntro: Boolean = false`, inyectar los dos use cases nuevos, observar `IsRoutineIntroSeenUseCase` en `init` y exponer:

```kotlin
    fun onIntroDismissed() {
        viewModelScope.launch {
            setRoutineIntroSeen()
            _uiState.update { it.copy(showIntro = false) }
        }
    }
```

Observación en `init`, junto a `observeRoutine()`:

```kotlin
    private fun observeIntro() {
        viewModelScope.launch {
            isRoutineIntroSeen().collect { seen ->
                _uiState.update { it.copy(showIntro = !seen) }
            }
        }
    }
```

En `RoutineContent`, añadir el parámetro `onIntroDismissed: () -> Unit` y, al final del cuerpo del `Scaffold`, el diálogo:

```kotlin
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
```

Imports nuevos: `androidx.compose.material3.AlertDialog`, `androidx.compose.material3.TextButton`. En `RoutineScreen`, pasar `onIntroDismissed = viewModel::onIntroDismissed`; en `RoutineContentPreview` y en `RoutineContentUiTest`, pasar `onIntroDismissed = {}`.

- [ ] **Step 5: Añadir el paso opcional al onboarding**

Crear el composable de la página final en `OnboardingScreen.kt`. No bloquea: sin selección el botón principal queda deshabilitado y siempre se puede salir con «Saltar».

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitOnboardingPage(
    templates: List<HabitTemplate>,
    selectedTemplateId: String?,
    onTemplateSelected: (HabitTemplate) -> Unit,
    onCreate: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_habit_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            templates.forEach { template ->
                FilterChip(
                    selected = selectedTemplateId == template.id,
                    onClick = { onTemplateSelected(template) },
                    label = { Text(resolveTemplateTitle(template.title)) }
                )
            }
        }
        Button(
            onClick = onCreate,
            enabled = selectedTemplateId != null,
            modifier = Modifier
                .padding(top = 32.dp)
                .testTag("onboarding_create_habit")
        ) {
            Text(stringResource(R.string.routine_add_habit))
        }
        TextButton(onClick = onSkip, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.onboarding_habit_skip))
        }
    }
}
```

`OnboardingViewModel` recibe `GetHabitTemplatesUseCase` y `CreateHabitUseCase`, expone `templates` y `selectedTemplateId` en su estado, y en `onCreateHabit()` llama a `createHabit(...)` con la plantilla elegida, color `0`, `startDate = LocalDate.now(clock)`, sin fecha de fin y sin recordatorio. Tanto `onCreate` como `onSkip` terminan invocando el `onFinished()` que ya existe en la pantalla.

- [ ] **Step 6: Verificar a mano**

```bash
./gradlew installDebug
```

Comprobar: en una instalación nueva aparece el paso de hábito en el onboarding y se puede saltar; al abrir «Mi rutina» por primera vez sale el aviso, y al volver a entrar ya no aparece.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/data/local/datastore app/src/main/java/com/gondroid/quoteanime/domain/usecase/RoutineIntroUseCases.kt app/src/main/java/com/gondroid/quoteanime/presentation app/src/main/res/values/strings.xml
git commit -m "feat(routine): introduce the routine tab to new and existing users"
```

---

### Task 10: Español e inglés

**Files:**
- Create: `app/src/main/res/values-es/strings.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: todos los textos añadidos en las tareas anteriores.
- Produces: inglés como idioma predeterminado, español para dispositivos en español.

- [ ] **Step 1: Copiar el español a su carpeta**

```bash
mkdir -p app/src/main/res/values-es
cp app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
```

- [ ] **Step 2: Traducir el archivo predeterminado al inglés**

Traducir **todos** los `<string>` de `app/src/main/res/values/strings.xml` al inglés, manteniendo idénticos los `name` y los marcadores de formato (`%1$d`, `%1$s`, `%%`). Textos de la rutina:

```xml
    <string name="routine_title">My routine</string>
    <string name="routine_streak_days">%1$d day streak</string>
    <string name="routine_progress_today">%1$d of %2$d today</string>
    <string name="routine_empty_title">Start your routine</string>
    <string name="routine_empty_body">Pick up to 3 habits and fill in every day you keep them.</string>
    <string name="routine_add_habit">Add habit</string>
    <string name="routine_limit_reached">You have reached the maximum of %1$d active habits.</string>
    <string name="routine_current_streak">Streak: %1$d</string>
    <string name="routine_best_streak">Best: %1$d</string>
    <string name="routine_completion_rate">Completion: %1$d%%</string>
    <string name="routine_mark_today">Mark today</string>
    <string name="routine_unmark_today">Unmark today</string>
    <string name="routine_archive">Archive</string>
    <string name="routine_edit">Edit</string>
    <string name="routine_message_future_day">You cannot mark a future day.</string>
    <string name="routine_message_outside_range">That day is outside the habit period.</string>
    <string name="nav_quotes">Quotes</string>
    <string name="nav_routine">My routine</string>
    <string name="nav_catalog">Catalog</string>
    <string name="template_train">Work out</string>
    <string name="template_read">Read</string>
    <string name="template_meditate">Meditate</string>
    <string name="template_water">Drink water</string>
    <string name="template_sleep_early">Sleep early</string>
    <string name="template_study">Study</string>
    <string name="template_write">Write</string>
    <string name="template_walk">Walk</string>
    <string name="habit_editor_new_title">New habit</string>
    <string name="habit_editor_edit_title">Edit habit</string>
    <string name="habit_editor_name">Habit name</string>
    <string name="habit_editor_templates">Suggestions</string>
    <string name="habit_editor_color">Color</string>
    <string name="habit_editor_start_date">Start date</string>
    <string name="habit_editor_end_date">End date</string>
    <string name="habit_editor_has_end_date">Set an end date</string>
    <string name="habit_editor_reminder">Reminder</string>
    <string name="habit_editor_reminder_time">Time</string>
    <string name="habit_editor_reminder_days">Days</string>
    <string name="habit_editor_save">Save</string>
    <string name="habit_editor_cancel">Cancel</string>
    <string name="habit_editor_error_blank">Give the habit a name.</string>
    <string name="habit_editor_error_dates">The end date must be after the start date.</string>
    <string name="habit_editor_error_limit">You already have %1$d active habits. Archive one to create another.</string>
    <string name="habit_channel_name">Habit reminders</string>
    <string name="habit_channel_description">Nudges to keep the habits in your routine</string>
    <string name="habit_notification_action_done">Done</string>
    <string name="routine_intro_title">New: My routine</string>
    <string name="routine_intro_body">Pick up to 3 habits and fill in every day you keep them. Your streak is waiting.</string>
    <string name="routine_intro_action">Get started</string>
    <string name="routine_intro_dismiss">Not now</string>
    <string name="onboarding_habit_title">Want to start a habit?</string>
    <string name="onboarding_habit_skip">Skip</string>
```

- [ ] **Step 3: Resolver las claves de plantilla en la interfaz**

Las plantillas de respaldo guardan claves (`template_train`), no texto. Añadir a `HabitIcons.kt`:

```kotlin
/** Bundled templates carry a string key; remote ones carry literal text. */
@Composable
fun resolveTemplateTitle(title: String): String {
    val context = LocalContext.current
    val resId = remember(title) {
        context.resources.getIdentifier(title, "string", context.packageName)
    }
    return if (resId != 0) stringResource(resId) else title
}
```

Imports necesarios: `androidx.compose.runtime.Composable`, `androidx.compose.runtime.remember`, `androidx.compose.ui.platform.LocalContext`, `androidx.compose.ui.res.stringResource`.

Usarla en `HabitEditorSheet` para el texto de los chips: `Text(resolveTemplateTitle(template.title))`. Y al seleccionar una plantilla, guardar el título ya resuelto para que el hábito se persista con texto legible.

- [ ] **Step 4: Verificar en el emulador**

```bash
./gradlew installDebug
```

Cambiar el idioma del dispositivo a inglés y recorrer las seis pantallas (splash, onboarding, frases, rutina, catálogo, ajustes) comprobando que no queda texto en español. Repetir en español.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values app/src/main/res/values-es app/src/main/java/com/gondroid/quoteanime/presentation/routine
git commit -m "feat(i18n): make English the default language and add Spanish resources"
```

---

## Estado al terminar la fase 1

La sección «Mi rutina» está completa y en manos del usuario: hábitos con plantillas o libres, heatmap interactivo con marcado retroactivo, rachas, recordatorios con acción rápida, navegación por pestañas, medición y dos idiomas.

**Verificación final antes de publicar:**

```bash
./gradlew test connectedAndroidTest assembleRelease
```

Pendiente para las fases siguientes:
- **Fase 2**: galería de logros, recompensas desbloqueables, autenticación anónima y espejo en Realtime Database con sus reglas de seguridad.
- **Fase 3**: widget Glance de racha con marcado rápido.
- **Fase 4**: homologación en iOS.

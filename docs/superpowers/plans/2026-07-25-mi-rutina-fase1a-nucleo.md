# «Mi rutina» — Fase 1A: núcleo de datos y lógica — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el modelo de dominio, la persistencia y toda la lógica de negocio del seguimiento de hábitos, con cobertura de tests, sin tocar todavía la interfaz.

**Architecture:** Clean Architecture igual que el resto del proyecto. Room es la fuente de verdad local con una migración real de la versión 4 a la 5 (nunca destructiva). La racha no se persiste: se deriva siempre de las filas de completions mediante una función pura. Las plantillas de hábitos vienen de Realtime Database con respaldo compilado en la app.

**Tech Stack:** Kotlin, Room 2.7.1, Hilt, Coroutines/Flow, Firebase Realtime Database, JUnit4 + MockK + Turbine, core library desugaring para `java.time`.

**Spec:** `docs/superpowers/specs/2026-07-25-mi-rutina-habit-tracker-design.md`

## Global Constraints

- minSdk 24, targetSdk 36, JVM 17, `com.gondroid.quoteanime`.
- `java.time` (`LocalDate`, `LocalTime`, `DayOfWeek`) solo es utilizable tras activar core library desugaring. Ningún archivo debe usarlo antes de completar la tarea 1.
- Las fechas se persisten como texto ISO `yyyy-MM-dd` (`LocalDate.toString()`), nunca como epoch.
- **Prohibido** persistir contadores de racha. La racha se calcula siempre desde `habit_completions`.
- **Prohibido** dejar `fallbackToDestructiveMigration()` en `DatabaseModule`: borraría los favoritos de los usuarios ya instalados.
- Máximo de hábitos activos: 3, siempre leído desde `PremiumGate`, nunca escrito como literal en use cases o UI.
- Los tests siguen la convención existente: nombres con backticks en estilo `given X, when Y, then Z`, MockK para dobles, `runTest` para corrutinas, `MainDispatcherRule` para ViewModels.
- Comentarios en inglés, textos de usuario en `strings.xml`. El código de esta fase no introduce textos de usuario.

## File Structure

**Crear:**

| Archivo | Responsabilidad |
|---|---|
| `domain/model/Habit.kt` | Modelo de hábito |
| `domain/model/HabitTemplate.kt` | Plantilla curada |
| `domain/model/StreakState.kt` | Resultado del cálculo de racha |
| `domain/model/HabitWithProgress.kt` | Hábito + completions visibles + racha + cumplimiento |
| `domain/repository/HabitRepository.kt` | Contrato de persistencia de hábitos |
| `domain/usecase/CalculateStreakUseCase.kt` | Función pura de racha |
| `domain/usecase/CreateHabitUseCase.kt` | Alta con validaciones y límite |
| `domain/usecase/UpdateHabitUseCase.kt` | Edición |
| `domain/usecase/ArchiveHabitUseCase.kt` | Archivado |
| `domain/usecase/ToggleHabitCompletionUseCase.kt` | Marcar/desmarcar un día |
| `domain/usecase/GetActiveHabitsUseCase.kt` | Composición para la pantalla |
| `domain/usecase/GetHabitTemplatesUseCase.kt` | Plantillas con respaldo |
| `data/local/db/entity/HabitEntity.kt` | Fila de `habits` |
| `data/local/db/entity/HabitCompletionEntity.kt` | Fila de `habit_completions` |
| `data/local/db/dao/HabitDao.kt` | Acceso a `habits` |
| `data/local/db/dao/HabitCompletionDao.kt` | Acceso a `habit_completions` |
| `data/local/db/Migrations.kt` | `MIGRATION_4_5` |
| `data/repository/HabitMappers.kt` | Entidad ↔ dominio |
| `data/repository/HabitRepositoryImpl.kt` | Implementación del contrato |
| `data/remote/HabitTemplateRemoteDataSource.kt` | Lectura de `/habitTemplates` |
| `data/remote/dto/HabitTemplateDto.kt` | DTO + mapper |
| `domain/model/DefaultHabitTemplates.kt` | Respaldo compilado |
| `di/PremiumGate.kt` | Límites según plan del usuario |

**Modificar:**

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml` | `desugar_jdk_libs`, `room-testing` |
| `app/build.gradle.kts` | Desugaring, `exportSchema`, dependencias de test |
| `data/local/db/AppDatabase.kt` | Versión 5, entidades y DAOs nuevos |
| `di/DatabaseModule.kt` | Migración real, `provides` de los DAOs nuevos |
| `di/RepositoryModule.kt` | Binding de `HabitRepository` |

---

### Task 1: Infraestructura de fechas y de migraciones

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/JavaTimeAvailabilityTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces: `java.time` disponible en producción; `androidx.room:room-testing` disponible en `androidTest`; esquemas de Room exportados a `app/schemas`.

- [ ] **Step 1: Añadir las versiones y librerías al catálogo**

En `gradle/libs.versions.toml`, dentro de `[versions]`:

```toml
desugarJdkLibs = "2.1.5"
```

Dentro de `[libraries]`:

```toml
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

- [ ] **Step 2: Activar desugaring y exportación de esquemas**

En `app/build.gradle.kts`, sustituir el bloque `compileOptions` por:

```kotlin
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
```

Añadir un bloque `ksp` **al nivel superior del archivo**, fuera de `android { ... }` y antes de `dependencies { ... }`. Es donde KSP lee sus argumentos; dentro de `defaultConfig` no tiene efecto:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

En el bloque `dependencies`, junto al resto de dependencias de Room:

```kotlin
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.coroutines.test)
```

`coroutines-test` en `androidTest` es necesario para los `runTest` de los tests de DAO de la tarea 3.

- [ ] **Step 3: Escribir el test que verifica el uso de java.time**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/JavaTimeAvailabilityTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain

import org.junit.Assert.assertEquals
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Test

/**
 * Guards the core library desugaring setup: the codebase relies on java.time
 * with minSdk 24, which only works with desugaring enabled.
 */
class JavaTimeAvailabilityTest {

    @Test
    fun `given an ISO string, when parsed, then it round-trips to the same text`() {
        val date = LocalDate.parse("2026-07-25")

        assertEquals("2026-07-25", date.toString())
        assertEquals(DayOfWeek.SATURDAY, date.dayOfWeek)
    }

    @Test
    fun `given a date, when subtracting one day, then the previous day is returned`() {
        val date = LocalDate.parse("2026-01-01")

        assertEquals(LocalDate.parse("2025-12-31"), date.minusDays(1))
    }
}
```

- [ ] **Step 4: Ejecutar el test y la compilación**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.JavaTimeAvailabilityTest" assembleDebug
```

Esperado: `BUILD SUCCESSFUL`. Si `assembleDebug` falla con un error de desugaring, revisar que `isCoreLibraryDesugaringEnabled` esté antes de las líneas de compatibilidad.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/test/java/com/gondroid/quoteanime/domain/JavaTimeAvailabilityTest.kt
git commit -m "build: enable core library desugaring and Room schema export"
```

---

### Task 2: Modelo de dominio y cálculo de racha

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/model/Habit.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/model/StreakState.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/CalculateStreakUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/CalculateStreakUseCaseTest.kt`

**Interfaces:**
- Consumes: `java.time` de la tarea 1.
- Produces: `Habit`, `StreakState(current, best, lastCompletedDate, completedToday)`, y `CalculateStreakUseCase.invoke(dates: List<LocalDate>, today: LocalDate): StreakState`.

- [ ] **Step 1: Escribir el test de racha**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/CalculateStreakUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - No completions at all
 *  - Only today
 *  - Consecutive run ending today
 *  - Run ending yesterday (streak still alive, not completed today)
 *  - Run ending two days ago (streak broken, best preserved)
 *  - Retroactive marking that joins two separate runs
 *  - Duplicated dates are ignored
 *  - Runs crossing month and year boundaries
 */
class CalculateStreakUseCaseTest {

    private val today = LocalDate.parse("2026-07-25")
    private lateinit var useCase: CalculateStreakUseCase

    @Before
    fun setup() {
        useCase = CalculateStreakUseCase()
    }

    private fun daysAgo(vararg offsets: Int): List<LocalDate> =
        offsets.map { today.minusDays(it.toLong()) }

    @Test
    fun `given no completions, when calculated, then everything is zero`() {
        val result = useCase(emptyList(), today)

        assertEquals(0, result.current)
        assertEquals(0, result.best)
        assertNull(result.lastCompletedDate)
        assertFalse(result.completedToday)
    }

    @Test
    fun `given only today, when calculated, then current streak is one and completed today is true`() {
        val result = useCase(daysAgo(0), today)

        assertEquals(1, result.current)
        assertEquals(1, result.best)
        assertEquals(today, result.lastCompletedDate)
        assertTrue(result.completedToday)
    }

    @Test
    fun `given three consecutive days ending today, when calculated, then current streak is three`() {
        val result = useCase(daysAgo(0, 1, 2), today)

        assertEquals(3, result.current)
        assertEquals(3, result.best)
        assertTrue(result.completedToday)
    }

    @Test
    fun `given a run ending yesterday, when calculated, then the streak is alive but not completed today`() {
        val result = useCase(daysAgo(1, 2, 3), today)

        assertEquals(3, result.current)
        assertFalse(result.completedToday)
    }

    @Test
    fun `given the last completion was two days ago, when calculated, then current is zero and best is preserved`() {
        val result = useCase(daysAgo(2, 3, 4, 5), today)

        assertEquals(0, result.current)
        assertEquals(4, result.best)
        assertEquals(today.minusDays(2), result.lastCompletedDate)
    }

    @Test
    fun `given a retroactive mark joining two runs, when calculated, then the runs count as one`() {
        // Days 0,1 and 3,4 are done; marking day 2 joins them into a run of five
        val result = useCase(daysAgo(0, 1, 2, 3, 4), today)

        assertEquals(5, result.current)
        assertEquals(5, result.best)
    }

    @Test
    fun `given duplicated dates, when calculated, then they count once`() {
        val result = useCase(daysAgo(0, 0, 1, 1), today)

        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun `given a run crossing the end of the year, when calculated, then it counts as consecutive`() {
        val newYear = LocalDate.parse("2026-01-01")
        val dates = listOf(
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2025-12-31"),
            LocalDate.parse("2025-12-30")
        )

        val result = useCase(dates, newYear)

        assertEquals(3, result.current)
    }

    @Test
    fun `given an old long run and a short current run, when calculated, then best keeps the long one`() {
        val dates = daysAgo(0, 1) + daysAgo(10, 11, 12, 13, 14)

        val result = useCase(dates, today)

        assertEquals(2, result.current)
        assertEquals(5, result.best)
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCaseTest"
```

Esperado: FALLA con error de compilación, `Unresolved reference: CalculateStreakUseCase`.

- [ ] **Step 3: Crear el modelo StreakState**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/model/StreakState.kt`:

```kotlin
package com.gondroid.quoteanime.domain.model

import java.time.LocalDate

/**
 * Always derived from the completion dates — never persisted, so it cannot drift
 * when the device time zone changes or when past days are marked retroactively.
 */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val lastCompletedDate: LocalDate? = null,
    val completedToday: Boolean = false
)
```

- [ ] **Step 4: Crear el modelo Habit**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/model/Habit.kt`:

```kotlin
package com.gondroid.quoteanime.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: String,
    val title: String,
    val iconKey: String,
    val colorIndex: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val reminderTime: LocalTime? = null,
    val reminderDays: Set<DayOfWeek> = emptySet(),
    val templateId: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L
) {
    /** True when [date] falls inside the habit's active window. */
    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))
}
```

- [ ] **Step 5: Implementar el cálculo de racha**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/CalculateStreakUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.StreakState
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pure function: given the dates a habit was completed, returns the streak state.
 * A streak stays alive while the most recent completion is today or yesterday.
 */
class CalculateStreakUseCase @Inject constructor() {

    operator fun invoke(dates: List<LocalDate>, today: LocalDate): StreakState {
        val sorted = dates.distinct().sortedDescending()
        if (sorted.isEmpty()) return StreakState()

        val last = sorted.first()
        val isAlive = last == today || last == today.minusDays(1)
        val current = if (isAlive) runLengthFrom(sorted, 0) else 0
        val best = longestRun(sorted)

        return StreakState(
            current = current,
            best = best,
            lastCompletedDate = last,
            completedToday = last == today
        )
    }

    /** Length of the consecutive run starting at [startIndex] in a descending list. */
    private fun runLengthFrom(sorted: List<LocalDate>, startIndex: Int): Int {
        var length = 1
        var index = startIndex
        while (index + 1 < sorted.size && sorted[index + 1] == sorted[index].minusDays(1)) {
            length++
            index++
        }
        return length
    }

    private fun longestRun(sorted: List<LocalDate>): Int {
        var best = 1
        var currentRun = 1
        for (index in 1 until sorted.size) {
            currentRun = if (sorted[index] == sorted[index - 1].minusDays(1)) currentRun + 1 else 1
            if (currentRun > best) best = currentRun
        }
        return best
    }
}
```

- [ ] **Step 6: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.CalculateStreakUseCaseTest"
```

Esperado: los 9 tests en verde.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain app/src/test/java/com/gondroid/quoteanime/domain/usecase/CalculateStreakUseCaseTest.kt
git commit -m "feat(routine): add habit domain model and streak calculation"
```

---

### Task 3: Persistencia en Room con migración segura

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/data/local/db/entity/HabitEntity.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/local/db/entity/HabitCompletionEntity.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/local/db/dao/HabitDao.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/local/db/dao/HabitCompletionDao.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/local/db/Migrations.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/data/local/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/di/DatabaseModule.kt`
- Test: `app/src/androidTest/java/com/gondroid/quoteanime/data/local/db/MigrationFrom4To5Test.kt`
- Test: `app/src/androidTest/java/com/gondroid/quoteanime/data/local/db/HabitDaoTest.kt`

**Interfaces:**
- Consumes: nada del dominio; las entidades son independientes.
- Produces: `HabitDao` (`getActive(): Flow<List<HabitEntity>>`, `countActive(): Int`, `getById(String): HabitEntity?`, `upsert(HabitEntity)`, `archive(String)`), `HabitCompletionDao` (`getByHabit(String): Flow<List<HabitCompletionEntity>>`, `getAllDates(): Flow<List<String>>`, `insert(HabitCompletionEntity)`, `delete(String, String)`, `exists(String, String): Boolean`), y `MIGRATION_4_5`.

- [ ] **Step 1: Crear las entidades**

Crear `app/src/main/java/com/gondroid/quoteanime/data/local/db/entity/HabitEntity.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val iconKey: String,
    val colorIndex: Int,
    val startDate: String,        // ISO yyyy-MM-dd
    val endDate: String?,         // ISO yyyy-MM-dd, null when open-ended
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val reminderDays: String,     // "MONDAY,WEDNESDAY"; empty when no reminder
    val templateId: String?,
    val isArchived: Boolean,
    val createdAt: Long
)
```

Crear `app/src/main/java/com/gondroid/quoteanime/data/local/db/entity/HabitCompletionEntity.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class HabitCompletionEntity(
    val habitId: String,
    val date: String,             // ISO yyyy-MM-dd
    val completedAt: Long
)
```

- [ ] **Step 2: Crear los DAOs**

Crear `app/src/main/java/com/gondroid/quoteanime/data/local/db/dao/HabitDao.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActive(): Flow<List<HabitEntity>>

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getById(habitId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId")
    suspend fun archive(habitId: String)
}
```

Crear `app/src/main/java/com/gondroid/quoteanime/data/local/db/dao/HabitCompletionDao.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getByHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    /** Distinct dates where at least one habit was completed — feeds the global streak. */
    @Query("SELECT DISTINCT date FROM habit_completions ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: String, date: String)

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun exists(habitId: String, date: String): Boolean
}
```

- [ ] **Step 3: Escribir la migración**

Crear `app/src/main/java/com/gondroid/quoteanime/data/local/db/Migrations.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the habit tracking tables. Favorites are left untouched: a destructive
 * fallback here would wipe every installed user's saved quotes.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habits` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `colorIndex` INTEGER NOT NULL,
                `startDate` TEXT NOT NULL,
                `endDate` TEXT,
                `reminderHour` INTEGER,
                `reminderMinute` INTEGER,
                `reminderDays` TEXT NOT NULL,
                `templateId` TEXT,
                `isArchived` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_completions` (
                `habitId` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`habitId`, `date`),
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habit_completions_date` " +
                "ON `habit_completions` (`date`)"
        )
    }
}
```

- [ ] **Step 4: Actualizar AppDatabase a la versión 5**

Sustituir el contenido de `app/src/main/java/com/gondroid/quoteanime/data/local/db/AppDatabase.kt` por:

```kotlin
package com.gondroid.quoteanime.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.FavoriteQuoteEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity

@Database(
    entities = [
        FavoriteQuoteEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteQuoteDao(): FavoriteQuoteDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
}
```

- [ ] **Step 5: Registrar la migración y los DAOs en Hilt**

Sustituir el contenido de `app/src/main/java/com/gondroid/quoteanime/di/DatabaseModule.kt` por:

```kotlin
package com.gondroid.quoteanime.di

import android.content.Context
import androidx.room.Room
import com.gondroid.quoteanime.data.local.db.AppDatabase
import com.gondroid.quoteanime.data.local.db.MIGRATION_4_5
import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "quote_anime_db"
        )
            .addMigrations(MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideFavoriteQuoteDao(db: AppDatabase): FavoriteQuoteDao = db.favoriteQuoteDao()

    @Provides
    fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()

    @Provides
    fun provideHabitCompletionDao(db: AppDatabase): HabitCompletionDao = db.habitCompletionDao()
}
```

Nota: se ha eliminado `fallbackToDestructiveMigration()`. A partir de ahora, cada subida de versión necesita su `Migration`.

- [ ] **Step 6: Escribir el test de migración**

Crear `app/src/androidTest/java/com/gondroid/quoteanime/data/local/db/MigrationFrom4To5Test.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards against data loss: users upgrading from version 4 must keep their favorites.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom4To5Test {

    private val testDb = "migration_test_db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate4To5_keepsFavoritesAndCreatesHabitTables() {
        helper.createDatabase(testDb, 4).apply {
            execSQL(
                "INSERT INTO favorite_quotes (id, quote, author, anime, animeSlug, categories, savedAt) " +
                    "VALUES ('q1', 'Never give up', 'Naruto', 'Naruto', 'naruto', '', 1000)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 5, true, MIGRATION_4_5)

        db.query("SELECT id FROM favorite_quotes").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("q1", cursor.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='habits'").use {
            assertTrue(it.count == 1)
        }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='habit_completions'"
        ).use {
            assertTrue(it.count == 1)
        }
    }
}
```

Si la inserción falla por no coincidir las columnas de `favorite_quotes`, ejecutar primero `./gradlew :app:kspDebugKotlin` y consultar el esquema generado en `app/schemas/…/4.json` para copiar los nombres exactos de columna.

- [ ] **Step 7: Escribir el test de los DAOs**

Crear `app/src/androidTest/java/com/gondroid/quoteanime/data/local/db/HabitDaoTest.kt`:

```kotlin
package com.gondroid.quoteanime.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var completionDao: HabitCompletionDao

    private fun habit(id: String, archived: Boolean = false) = HabitEntity(
        id = id,
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = "2026-07-01",
        endDate = null,
        reminderHour = null,
        reminderMinute = null,
        reminderDays = "",
        templateId = null,
        isArchived = archived,
        createdAt = 1000
    )

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        habitDao = db.habitDao()
        completionDao = db.habitCompletionDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun archivedHabitsAreExcludedFromActive() = runTest {
        habitDao.upsert(habit("h1"))
        habitDao.upsert(habit("h2", archived = true))

        assertEquals(listOf("h1"), habitDao.getActive().first().map { it.id })
        assertEquals(1, habitDao.countActive())
    }

    @Test
    fun insertingSameDayTwiceKeepsOneRow() = runTest {
        habitDao.upsert(habit("h1"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 2))

        assertEquals(1, completionDao.getByHabit("h1").first().size)
        assertTrue(completionDao.exists("h1", "2026-07-25"))
    }

    @Test
    fun deletingCompletionRemovesTheDay() = runTest {
        habitDao.upsert(habit("h1"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))

        completionDao.delete("h1", "2026-07-25")

        assertFalse(completionDao.exists("h1", "2026-07-25"))
    }

    @Test
    fun distinctDatesAreReturnedOnceAcrossHabits() = runTest {
        habitDao.upsert(habit("h1"))
        habitDao.upsert(habit("h2"))
        completionDao.insert(HabitCompletionEntity("h1", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h2", "2026-07-25", 1))
        completionDao.insert(HabitCompletionEntity("h2", "2026-07-24", 1))

        assertEquals(listOf("2026-07-25", "2026-07-24"), completionDao.getAllDates().first())
    }
}
```

- [ ] **Step 8: Ejecutar los tests instrumentados**

Con un emulador o dispositivo conectado:

```bash
./gradlew connectedAndroidTest --tests "com.gondroid.quoteanime.data.local.db.*"
```

Esperado: los dos tests de migración y de DAOs en verde. Si `runMigrationsAndValidate` falla por diferencias de esquema, comparar el SQL de `Migrations.kt` con `app/schemas/com.gondroid.quoteanime.data.local.db.AppDatabase/5.json` y ajustar exactamente tipos, orden y restricciones.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/data/local/db app/src/main/java/com/gondroid/quoteanime/di/DatabaseModule.kt app/src/androidTest/java/com/gondroid/quoteanime/data/local/db app/schemas
git commit -m "feat(routine): add habit tables with a non-destructive Room migration"
```

---

### Task 4: Repositorio de hábitos

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/repository/HabitRepository.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/repository/HabitMappers.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/repository/HabitRepositoryImpl.kt`
- Modify: `app/src/main/java/com/gondroid/quoteanime/di/RepositoryModule.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/data/repository/HabitMappersTest.kt`

**Interfaces:**
- Consumes: `Habit` (tarea 2), `HabitDao` y `HabitCompletionDao` (tarea 3).
- Produces: `HabitRepository` con `getActiveHabits(): Flow<List<Habit>>`, `getCompletions(habitId: String): Flow<List<LocalDate>>`, `getAllCompletionDates(): Flow<List<LocalDate>>`, `countActiveHabits(): Int`, `getHabit(id: String): Habit?`, `saveHabit(habit: Habit)`, `archiveHabit(id: String)`, `setCompletion(habitId: String, date: LocalDate, completed: Boolean)`, `isCompleted(habitId: String, date: LocalDate): Boolean`.

- [ ] **Step 1: Escribir el test de los mappers**

Crear `app/src/test/java/com/gondroid/quoteanime/data/repository/HabitMappersTest.kt`:

```kotlin
package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Full habit with reminder and end date round-trips unchanged
 *  - Habit without reminder stores an empty day list and null time
 *  - Unknown day names in stored data are ignored instead of crashing
 */
class HabitMappersTest {

    private val fullHabit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 3,
        startDate = LocalDate.parse("2026-07-01"),
        endDate = LocalDate.parse("2026-08-01"),
        reminderTime = LocalTime.of(7, 30),
        reminderDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        templateId = "train",
        isArchived = false,
        createdAt = 1234L
    )

    @Test
    fun `given a full habit, when mapped both ways, then it stays equal`() {
        val result = fullHabit.toEntity().toDomain()

        assertEquals(fullHabit, result)
    }

    @Test
    fun `given a habit without reminder, when mapped to entity, then time is null and days are empty`() {
        val entity = fullHabit.copy(reminderTime = null, reminderDays = emptySet()).toEntity()

        assertNull(entity.reminderHour)
        assertNull(entity.reminderMinute)
        assertEquals("", entity.reminderDays)
    }

    @Test
    fun `given an unknown day name stored, when mapped to domain, then it is ignored`() {
        val entity = fullHabit.toEntity().copy(reminderDays = "MONDAY,LUNES")

        assertEquals(setOf(DayOfWeek.MONDAY), entity.toDomain().reminderDays)
    }

    @Test
    fun `given a habit without end date, when mapped both ways, then end date stays null`() {
        val openEnded = fullHabit.copy(endDate = null)

        assertNull(openEnded.toEntity().toDomain().endDate)
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.data.repository.HabitMappersTest"
```

Esperado: FALLA con `Unresolved reference: toEntity`.

- [ ] **Step 3: Implementar los mappers**

Crear `app/src/main/java/com/gondroid/quoteanime/data/repository/HabitMappers.kt`:

```kotlin
package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.entity.HabitEntity
import com.gondroid.quoteanime.domain.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    iconKey = iconKey,
    colorIndex = colorIndex,
    startDate = startDate.toString(),
    endDate = endDate?.toString(),
    reminderHour = reminderTime?.hour,
    reminderMinute = reminderTime?.minute,
    reminderDays = reminderDays.joinToString(",") { it.name },
    templateId = templateId,
    isArchived = isArchived,
    createdAt = createdAt
)

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    title = title,
    iconKey = iconKey,
    colorIndex = colorIndex,
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let(LocalDate::parse),
    reminderTime = if (reminderHour != null && reminderMinute != null) {
        LocalTime.of(reminderHour, reminderMinute)
    } else null,
    reminderDays = reminderDays.split(",")
        .mapNotNull { name -> runCatching { DayOfWeek.valueOf(name.trim()) }.getOrNull() }
        .toSet(),
    templateId = templateId,
    isArchived = isArchived,
    createdAt = createdAt
)
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.data.repository.HabitMappersTest"
```

Esperado: los 4 tests en verde.

- [ ] **Step 5: Crear el contrato del repositorio**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/repository/HabitRepository.kt`:

```kotlin
package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.Habit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getActiveHabits(): Flow<List<Habit>>
    fun getCompletions(habitId: String): Flow<List<LocalDate>>
    /** Dates where at least one habit was completed — feeds the global streak. */
    fun getAllCompletionDates(): Flow<List<LocalDate>>
    suspend fun countActiveHabits(): Int
    suspend fun getHabit(id: String): Habit?
    suspend fun saveHabit(habit: Habit)
    suspend fun archiveHabit(id: String)
    suspend fun setCompletion(habitId: String, date: LocalDate, completed: Boolean)
    suspend fun isCompleted(habitId: String, date: LocalDate): Boolean
}
```

- [ ] **Step 6: Implementar el repositorio**

Crear `app/src/main/java/com/gondroid/quoteanime/data/repository/HabitRepositoryImpl.kt`:

```kotlin
package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.dao.HabitCompletionDao
import com.gondroid.quoteanime.data.local.db.dao.HabitDao
import com.gondroid.quoteanime.data.local.db.entity.HabitCompletionEntity
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) : HabitRepository {

    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.getActive().map { entities -> entities.map { it.toDomain() } }

    override fun getCompletions(habitId: String): Flow<List<LocalDate>> =
        completionDao.getByHabit(habitId).map { rows -> rows.map { LocalDate.parse(it.date) } }

    override fun getAllCompletionDates(): Flow<List<LocalDate>> =
        completionDao.getAllDates().map { dates -> dates.map(LocalDate::parse) }

    override suspend fun countActiveHabits(): Int = habitDao.countActive()

    override suspend fun getHabit(id: String): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun saveHabit(habit: Habit) = habitDao.upsert(habit.toEntity())

    override suspend fun archiveHabit(id: String) = habitDao.archive(id)

    override suspend fun setCompletion(habitId: String, date: LocalDate, completed: Boolean) {
        if (completed) {
            completionDao.insert(
                HabitCompletionEntity(
                    habitId = habitId,
                    date = date.toString(),
                    completedAt = System.currentTimeMillis()
                )
            )
        } else {
            completionDao.delete(habitId, date.toString())
        }
    }

    override suspend fun isCompleted(habitId: String, date: LocalDate): Boolean =
        completionDao.exists(habitId, date.toString())
}
```

- [ ] **Step 7: Registrar el binding en Hilt**

En `app/src/main/java/com/gondroid/quoteanime/di/RepositoryModule.kt`, añadir el import y el método:

```kotlin
import com.gondroid.quoteanime.data.repository.HabitRepositoryImpl
import com.gondroid.quoteanime.domain.repository.HabitRepository
```

```kotlin
    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository
```

- [ ] **Step 8: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`. Un fallo de Hilt aquí indica que falta algún `@Provides` de DAO en `DatabaseModule`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/repository/HabitRepository.kt app/src/main/java/com/gondroid/quoteanime/data/repository app/src/main/java/com/gondroid/quoteanime/di/RepositoryModule.kt app/src/test/java/com/gondroid/quoteanime/data/repository/HabitMappersTest.kt
git commit -m "feat(routine): add habit repository with entity mapping"
```

---

### Task 5: Límite de hábitos y alta

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/di/PremiumGate.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCaseTest.kt`

**Interfaces:**
- Consumes: `HabitRepository` (tarea 4), `Habit` (tarea 2).
- Produces: `PremiumGate.maxActiveHabits: Int`, `PremiumGate.isPremium: Boolean`, `CreateHabitUseCase.invoke(...): CreateHabitResult` con las variantes `Success(habit)`, `LimitReached(max)`, `BlankTitle`, `InvalidDateRange`.

- [ ] **Step 1: Escribir el test de alta**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Scenarios covered:
 *  - Creating below the limit succeeds and persists
 *  - Creating at the limit is rejected without touching the repository
 *  - Blank titles are rejected
 *  - End date before start date is rejected
 *  - End date equal to start date is accepted (a one-day challenge)
 */
class CreateHabitUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: CreateHabitUseCase
    private val premiumGate = PremiumGate()
    private val today = LocalDate.parse("2026-07-25")

    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateHabitUseCase(repository, premiumGate)
    }

    private suspend fun create(
        title: String = "Entrenar",
        startDate: LocalDate = today,
        endDate: LocalDate? = null
    ) = useCase(
        title = title,
        iconKey = "dumbbell",
        colorIndex = 2,
        startDate = startDate,
        endDate = endDate,
        reminderTime = LocalTime.of(7, 0),
        reminderDays = setOf(DayOfWeek.MONDAY),
        templateId = null
    )

    @Test
    fun `given fewer habits than the limit, when creating, then it succeeds and is saved`() = runTest {
        coEvery { repository.countActiveHabits() } returns 2
        coJustRun { repository.saveHabit(any()) }

        val result = create()

        assertTrue(result is CreateHabitResult.Success)
        assertEquals("Entrenar", (result as CreateHabitResult.Success).habit.title)
        coVerify(exactly = 1) { repository.saveHabit(any()) }
    }

    @Test
    fun `given the limit is reached, when creating, then it is rejected and nothing is saved`() = runTest {
        coEvery { repository.countActiveHabits() } returns 3

        val result = create()

        assertEquals(CreateHabitResult.LimitReached(3), result)
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given a blank title, when creating, then it is rejected`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0

        assertEquals(CreateHabitResult.BlankTitle, create(title = "   "))
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given an end date before the start date, when creating, then it is rejected`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0

        val result = create(startDate = today, endDate = today.minusDays(1))

        assertEquals(CreateHabitResult.InvalidDateRange, result)
    }

    @Test
    fun `given an end date equal to the start date, when creating, then it succeeds`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0
        coJustRun { repository.saveHabit(any()) }

        assertTrue(create(startDate = today, endDate = today) is CreateHabitResult.Success)
    }

    @Test
    fun `given a title with surrounding spaces, when creating, then it is trimmed`() = runTest {
        coEvery { repository.countActiveHabits() } returns 0
        coJustRun { repository.saveHabit(any()) }

        val result = create(title = "  Leer  ") as CreateHabitResult.Success

        assertEquals("Leer", result.habit.title)
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.CreateHabitUseCaseTest"
```

Esperado: FALLA con `Unresolved reference: PremiumGate`.

- [ ] **Step 3: Crear PremiumGate**

Crear `app/src/main/java/com/gondroid/quoteanime/di/PremiumGate.kt`:

```kotlin
package com.gondroid.quoteanime.di

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single place where plan-based limits live. Billing is not implemented yet:
 * flipping [isPremium] later must not require touching use cases or UI.
 */
@Singleton
class PremiumGate @Inject constructor() {

    val isPremium: Boolean = false

    val maxActiveHabits: Int
        get() = if (isPremium) UNLIMITED_HABITS else FREE_HABIT_LIMIT

    companion object {
        const val FREE_HABIT_LIMIT = 3
        const val UNLIMITED_HABITS = Int.MAX_VALUE
    }
}
```

- [ ] **Step 4: Implementar el alta**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.di.PremiumGate
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

sealed interface CreateHabitResult {
    data class Success(val habit: Habit) : CreateHabitResult
    data class LimitReached(val max: Int) : CreateHabitResult
    data object BlankTitle : CreateHabitResult
    data object InvalidDateRange : CreateHabitResult
}

class CreateHabitUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val premiumGate: PremiumGate
) {
    suspend operator fun invoke(
        title: String,
        iconKey: String,
        colorIndex: Int,
        startDate: LocalDate,
        endDate: LocalDate?,
        reminderTime: LocalTime?,
        reminderDays: Set<DayOfWeek>,
        templateId: String?
    ): CreateHabitResult {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return CreateHabitResult.BlankTitle
        if (endDate != null && endDate.isBefore(startDate)) return CreateHabitResult.InvalidDateRange

        val max = premiumGate.maxActiveHabits
        if (repository.countActiveHabits() >= max) return CreateHabitResult.LimitReached(max)

        val habit = Habit(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            iconKey = iconKey,
            colorIndex = colorIndex,
            startDate = startDate,
            endDate = endDate,
            reminderTime = reminderTime,
            reminderDays = if (reminderTime == null) emptySet() else reminderDays,
            templateId = templateId,
            isArchived = false,
            createdAt = System.currentTimeMillis()
        )
        repository.saveHabit(habit)
        return CreateHabitResult.Success(habit)
    }
}
```

- [ ] **Step 5: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.CreateHabitUseCaseTest"
```

Esperado: los 6 tests en verde.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/di/PremiumGate.kt app/src/main/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCase.kt app/src/test/java/com/gondroid/quoteanime/domain/usecase/CreateHabitUseCaseTest.kt
git commit -m "feat(routine): add habit creation with plan-based limit"
```

---

### Task 6: Marcar y desmarcar días

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCaseTest.kt`

**Interfaces:**
- Consumes: `HabitRepository` (tarea 4), `Habit.isActiveOn(date)` (tarea 2).
- Produces: `ToggleHabitCompletionUseCase.invoke(habitId: String, date: LocalDate, today: LocalDate): ToggleCompletionResult` con las variantes `Success(completed: Boolean)`, `HabitNotFound`, `FutureDate`, `OutsideHabitRange`.

- [ ] **Step 1: Escribir el test**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Marking an unmarked day inserts it
 *  - Marking an already marked day removes it (toggle)
 *  - Retroactive marking of a past day is allowed
 *  - Future days are rejected
 *  - Days before startDate or after endDate are rejected
 *  - Unknown habit id is reported
 */
class ToggleHabitCompletionUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: ToggleHabitCompletionUseCase

    private val today = LocalDate.parse("2026-07-25")
    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = LocalDate.parse("2026-07-01"),
        endDate = LocalDate.parse("2026-07-31")
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = ToggleHabitCompletionUseCase(repository)
    }

    @Test
    fun `given an unmarked day, when toggled, then it is marked as completed`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns false
        coJustRun { repository.setCompletion("h1", today, true) }

        val result = useCase("h1", today, today)

        assertEquals(ToggleCompletionResult.Success(completed = true), result)
        coVerify(exactly = 1) { repository.setCompletion("h1", today, true) }
    }

    @Test
    fun `given a marked day, when toggled, then it is unmarked`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", today) } returns true
        coJustRun { repository.setCompletion("h1", today, false) }

        val result = useCase("h1", today, today)

        assertEquals(ToggleCompletionResult.Success(completed = false), result)
    }

    @Test
    fun `given a past day inside the range, when toggled, then it is allowed`() = runTest {
        val pastDay = today.minusDays(5)
        coEvery { repository.getHabit("h1") } returns habit
        coEvery { repository.isCompleted("h1", pastDay) } returns false
        coJustRun { repository.setCompletion("h1", pastDay, true) }

        assertEquals(ToggleCompletionResult.Success(true), useCase("h1", pastDay, today))
    }

    @Test
    fun `given a future day, when toggled, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        val result = useCase("h1", today.plusDays(1), today)

        assertEquals(ToggleCompletionResult.FutureDate, result)
        coVerify(exactly = 0) { repository.setCompletion(any(), any(), any()) }
    }

    @Test
    fun `given a day before the habit started, when toggled, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        val result = useCase("h1", LocalDate.parse("2026-06-30"), today)

        assertEquals(ToggleCompletionResult.OutsideHabitRange, result)
    }

    @Test
    fun `given a day after the habit ended, when toggled, then it is rejected`() = runTest {
        val endedHabit = habit.copy(endDate = LocalDate.parse("2026-07-10"))
        coEvery { repository.getHabit("h1") } returns endedHabit

        val result = useCase("h1", LocalDate.parse("2026-07-20"), today)

        assertEquals(ToggleCompletionResult.OutsideHabitRange, result)
    }

    @Test
    fun `given an unknown habit, when toggled, then it is reported as not found`() = runTest {
        coEvery { repository.getHabit("missing") } returns null

        assertEquals(ToggleCompletionResult.HabitNotFound, useCase("missing", today, today))
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCaseTest"
```

Esperado: FALLA con `Unresolved reference: ToggleHabitCompletionUseCase`.

- [ ] **Step 3: Implementar el use case**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import java.time.LocalDate
import javax.inject.Inject

sealed interface ToggleCompletionResult {
    data class Success(val completed: Boolean) : ToggleCompletionResult
    data object HabitNotFound : ToggleCompletionResult
    data object FutureDate : ToggleCompletionResult
    data object OutsideHabitRange : ToggleCompletionResult
}

/**
 * Marks or unmarks a single day. Past days can be corrected; future days cannot
 * be marked, and days outside the habit's own start/end window are rejected so
 * the completion rate stays meaningful.
 */
class ToggleHabitCompletionUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(
        habitId: String,
        date: LocalDate,
        today: LocalDate
    ): ToggleCompletionResult {
        val habit = repository.getHabit(habitId) ?: return ToggleCompletionResult.HabitNotFound
        if (date.isAfter(today)) return ToggleCompletionResult.FutureDate
        if (!habit.isActiveOn(date)) return ToggleCompletionResult.OutsideHabitRange

        val newValue = !repository.isCompleted(habitId, date)
        repository.setCompletion(habitId, date, newValue)
        return ToggleCompletionResult.Success(completed = newValue)
    }
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.ToggleHabitCompletionUseCaseTest"
```

Esperado: los 7 tests en verde.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCase.kt app/src/test/java/com/gondroid/quoteanime/domain/usecase/ToggleHabitCompletionUseCaseTest.kt
git commit -m "feat(routine): add habit completion toggle with date validation"
```

---

### Task 7: Edición y archivado

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCase.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/ArchiveHabitUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCaseTest.kt`

**Interfaces:**
- Consumes: `HabitRepository` (tarea 4), `CreateHabitResult` para reutilizar las variantes de validación.
- Produces: `UpdateHabitUseCase.invoke(habit: Habit): UpdateHabitResult` con `Success(habit)`, `BlankTitle`, `InvalidDateRange`, `HabitNotFound`; y `ArchiveHabitUseCase.invoke(habitId: String)`.

- [ ] **Step 1: Escribir el test**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Updating an existing habit persists the new values
 *  - Blank title and invalid date range are rejected
 *  - Updating a habit that no longer exists is reported
 *  - Archiving delegates to the repository
 */
class UpdateHabitUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var updateHabit: UpdateHabitUseCase
    private lateinit var archiveHabit: ArchiveHabitUseCase

    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = LocalDate.parse("2026-07-01")
    )

    @Before
    fun setup() {
        repository = mockk()
        updateHabit = UpdateHabitUseCase(repository)
        archiveHabit = ArchiveHabitUseCase(repository)
    }

    @Test
    fun `given an existing habit, when updated, then the new values are saved`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        coJustRun { repository.saveHabit(any()) }
        val edited = habit.copy(title = "Entrenar duro", colorIndex = 5)

        val result = updateHabit(edited)

        assertTrue(result is UpdateHabitResult.Success)
        coVerify(exactly = 1) { repository.saveHabit(edited) }
    }

    @Test
    fun `given a blank title, when updated, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit

        assertEquals(UpdateHabitResult.BlankTitle, updateHabit(habit.copy(title = "  ")))
        coVerify(exactly = 0) { repository.saveHabit(any()) }
    }

    @Test
    fun `given an end date before the start date, when updated, then it is rejected`() = runTest {
        coEvery { repository.getHabit("h1") } returns habit
        val invalid = habit.copy(endDate = habit.startDate.minusDays(1))

        assertEquals(UpdateHabitResult.InvalidDateRange, updateHabit(invalid))
    }

    @Test
    fun `given a habit that no longer exists, when updated, then it is reported as not found`() = runTest {
        coEvery { repository.getHabit("h1") } returns null

        assertEquals(UpdateHabitResult.HabitNotFound, updateHabit(habit))
    }

    @Test
    fun `given a habit id, when archived, then the repository archives it`() = runTest {
        coJustRun { repository.archiveHabit("h1") }

        archiveHabit("h1")

        coVerify(exactly = 1) { repository.archiveHabit("h1") }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCaseTest"
```

Esperado: FALLA con `Unresolved reference: UpdateHabitUseCase`.

- [ ] **Step 3: Implementar la edición**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

sealed interface UpdateHabitResult {
    data class Success(val habit: Habit) : UpdateHabitResult
    data object BlankTitle : UpdateHabitResult
    data object InvalidDateRange : UpdateHabitResult
    data object HabitNotFound : UpdateHabitResult
}

class UpdateHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habit: Habit): UpdateHabitResult {
        repository.getHabit(habit.id) ?: return UpdateHabitResult.HabitNotFound
        if (habit.title.isBlank()) return UpdateHabitResult.BlankTitle
        if (habit.endDate != null && habit.endDate.isBefore(habit.startDate)) {
            return UpdateHabitResult.InvalidDateRange
        }

        repository.saveHabit(habit)
        return UpdateHabitResult.Success(habit)
    }
}
```

- [ ] **Step 4: Implementar el archivado**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/ArchiveHabitUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.repository.HabitRepository
import javax.inject.Inject

/** Archiving keeps the history: completions are never deleted from the database. */
class ArchiveHabitUseCase @Inject constructor(
    private val repository: HabitRepository
) {
    suspend operator fun invoke(habitId: String) = repository.archiveHabit(habitId)
}
```

- [ ] **Step 5: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.UpdateHabitUseCaseTest"
```

Esperado: los 5 tests en verde.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCase.kt app/src/main/java/com/gondroid/quoteanime/domain/usecase/ArchiveHabitUseCase.kt app/src/test/java/com/gondroid/quoteanime/domain/usecase/UpdateHabitUseCaseTest.kt
git commit -m "feat(routine): add habit editing and archiving"
```

---

### Task 8: Plantillas de hábitos con respaldo local

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/model/HabitTemplate.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/model/DefaultHabitTemplates.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/remote/dto/HabitTemplateDto.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/data/remote/HabitTemplateRemoteDataSource.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCaseTest.kt`

**Interfaces:**
- Consumes: `FirebaseDatabase` (ya inyectado en `AppModule` para `QuoteRemoteDataSource`).
- Produces: `HabitTemplate(id, title, iconKey, order)`, `DefaultHabitTemplates.ALL: List<HabitTemplate>`, `GetHabitTemplatesUseCase.invoke(): Flow<List<HabitTemplate>>`.

- [ ] **Step 1: Escribir el test**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Scenarios covered:
 *  - Remote templates are returned sorted by order
 *  - An empty remote node falls back to the bundled defaults
 *  - A remote failure falls back to the bundled defaults
 */
class GetHabitTemplatesUseCaseTest {

    private lateinit var remote: HabitTemplateRemoteDataSource
    private lateinit var useCase: GetHabitTemplatesUseCase

    @Before
    fun setup() {
        remote = mockk()
        useCase = GetHabitTemplatesUseCase(remote)
    }

    @Test
    fun `given remote templates, when collected, then they are sorted by order`() = runTest {
        every { remote.getTemplates() } returns flowOf(
            listOf(
                HabitTemplate("b", "Leer", "book", order = 2),
                HabitTemplate("a", "Entrenar", "dumbbell", order = 1)
            )
        )

        useCase().test {
            assertEquals(listOf("a", "b"), awaitItem().map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun `given an empty remote node, when collected, then the bundled defaults are returned`() = runTest {
        every { remote.getTemplates() } returns flowOf(emptyList())

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a remote failure, when collected, then the bundled defaults are returned`() = runTest {
        every { remote.getTemplates() } returns flow { throw IllegalStateException("no network") }

        useCase().test {
            assertEquals(DefaultHabitTemplates.ALL, awaitItem())
            awaitComplete()
        }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCaseTest"
```

Esperado: FALLA con `Unresolved reference: HabitTemplateRemoteDataSource`.

- [ ] **Step 3: Crear el modelo y el respaldo**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/model/HabitTemplate.kt`:

```kotlin
package com.gondroid.quoteanime.domain.model

data class HabitTemplate(
    val id: String,
    val title: String,
    val iconKey: String,
    val order: Int
)
```

Crear `app/src/main/java/com/gondroid/quoteanime/domain/model/DefaultHabitTemplates.kt`:

```kotlin
package com.gondroid.quoteanime.domain.model

/**
 * Bundled fallback so the habit editor is never empty on first launch or offline.
 * The remote /habitTemplates node overrides this list when available.
 * Titles are placeholders resolved to strings.xml keys by the UI layer.
 */
object DefaultHabitTemplates {
    val ALL: List<HabitTemplate> = listOf(
        HabitTemplate("train", "template_train", "dumbbell", 1),
        HabitTemplate("read", "template_read", "book", 2),
        HabitTemplate("meditate", "template_meditate", "self_improvement", 3),
        HabitTemplate("water", "template_water", "water_drop", 4),
        HabitTemplate("sleep_early", "template_sleep_early", "bedtime", 5),
        HabitTemplate("study", "template_study", "school", 6),
        HabitTemplate("write", "template_write", "edit_note", 7),
        HabitTemplate("walk", "template_walk", "directions_walk", 8)
    )
}
```

El campo `title` de los valores por defecto es una **clave de string**, no texto visible. La interfaz (fase 1B) resuelve la clave contra `strings.xml`; si no encuentra la clave, muestra el valor tal cual, que es el caso de las plantillas remotas.

- [ ] **Step 4: Crear el DTO y el data source**

Crear `app/src/main/java/com/gondroid/quoteanime/data/remote/dto/HabitTemplateDto.kt`:

```kotlin
package com.gondroid.quoteanime.data.remote.dto

import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.google.firebase.database.DataSnapshot

data class HabitTemplateDto(
    val id: String = "",
    val title: String = "",
    val iconKey: String = "",
    val order: Int = 0
)

fun DataSnapshot.toHabitTemplateDto(): HabitTemplateDto? {
    val id = key ?: return null
    val title = child("title").getValue(String::class.java) ?: return null
    val iconKey = child("iconKey").getValue(String::class.java) ?: return null
    val order = child("order").getValue(Int::class.java) ?: 0
    return HabitTemplateDto(id = id, title = title, iconKey = iconKey, order = order)
}

fun HabitTemplateDto.toDomain(): HabitTemplate =
    HabitTemplate(id = id, title = title, iconKey = iconKey, order = order)
```

Crear `app/src/main/java/com/gondroid/quoteanime/data/remote/HabitTemplateRemoteDataSource.kt`:

```kotlin
package com.gondroid.quoteanime.data.remote

import com.gondroid.quoteanime.data.remote.dto.toDomain
import com.gondroid.quoteanime.data.remote.dto.toHabitTemplateDto
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class HabitTemplateRemoteDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val templatesRef = database.getReference("habitTemplates")

    fun getTemplates(): Flow<List<HabitTemplate>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val templates = snapshot.children
                    .mapNotNull { it.toHabitTemplateDto()?.toDomain() }
                trySend(templates)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        templatesRef.addValueEventListener(listener)
        awaitClose { templatesRef.removeEventListener(listener) }
    }
}
```

- [ ] **Step 5: Implementar el use case con respaldo**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Remote templates let new habit suggestions ship without an app release.
 * The bundled list keeps the editor usable offline and on first launch.
 */
class GetHabitTemplatesUseCase @Inject constructor(
    private val remoteDataSource: HabitTemplateRemoteDataSource
) {
    operator fun invoke(): Flow<List<HabitTemplate>> =
        remoteDataSource.getTemplates()
            .map { templates ->
                if (templates.isEmpty()) DefaultHabitTemplates.ALL
                else templates.sortedBy { it.order }
            }
            .catch { emit(DefaultHabitTemplates.ALL) }
}
```

- [ ] **Step 6: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.GetHabitTemplatesUseCaseTest"
```

Esperado: los 3 tests en verde.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/model/HabitTemplate.kt app/src/main/java/com/gondroid/quoteanime/domain/model/DefaultHabitTemplates.kt app/src/main/java/com/gondroid/quoteanime/data/remote app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCase.kt app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetHabitTemplatesUseCaseTest.kt
git commit -m "feat(routine): add habit templates with bundled fallback"
```

---

### Task 9: Composición para la pantalla

**Files:**
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/model/HabitWithProgress.kt`
- Create: `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCase.kt`
- Test: `app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCaseTest.kt`

**Interfaces:**
- Consumes: `HabitRepository`, `CalculateStreakUseCase`.
- Produces: `HabitWithProgress(habit, completions: Set<LocalDate>, streak: StreakState, completionRate: Float)` y `GetActiveHabitsUseCase.invoke(today: LocalDate): Flow<List<HabitWithProgress>>`. Esta es la firma que consume `RoutineViewModel` en la fase 1B. Constante `VISIBLE_WEEKS = 17`.

- [ ] **Step 1: Escribir el test**

Crear `app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCaseTest.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import app.cash.turbine.test
import com.gondroid.quoteanime.domain.model.Habit
import com.gondroid.quoteanime.domain.repository.HabitRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Scenarios covered:
 *  - Each habit is paired with its own completions and streak
 *  - Only completions inside the visible window reach the heatmap
 *  - The streak still counts runs older than the visible window
 *  - Completion rate is measured from the habit start date
 *  - No habits produces an empty list
 */
class GetActiveHabitsUseCaseTest {

    private lateinit var repository: HabitRepository
    private lateinit var useCase: GetActiveHabitsUseCase

    private val today = LocalDate.parse("2026-07-25")

    private val habit = Habit(
        id = "h1",
        title = "Entrenar",
        iconKey = "dumbbell",
        colorIndex = 0,
        startDate = today.minusDays(9)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetActiveHabitsUseCase(repository, CalculateStreakUseCase())
    }

    @Test
    fun `given no habits, when collected, then the list is empty`() = runTest {
        every { repository.getActiveHabits() } returns flowOf(emptyList())

        useCase(today).test {
            assertEquals(emptyList<Any>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given a habit with recent completions, when collected, then streak and completions are attached`() = runTest {
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(
            listOf(today, today.minusDays(1), today.minusDays(2))
        )

        useCase(today).test {
            val item = awaitItem().single()
            assertEquals(3, item.streak.current)
            assertEquals(3, item.completions.size)
            awaitComplete()
        }
    }

    @Test
    fun `given completions older than the visible window, when collected, then they are excluded from the heatmap`() = runTest {
        val old = today.minusWeeks(GetActiveHabitsUseCase.VISIBLE_WEEKS.toLong() + 1)
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(listOf(today, old))

        useCase(today).test {
            val item = awaitItem().single()
            assertEquals(setOf(today), item.completions)
            awaitComplete()
        }
    }

    @Test
    fun `given ten active days and five completions, when collected, then completion rate is one half`() = runTest {
        // startDate is 9 days ago, so the active window is 10 days including today
        every { repository.getActiveHabits() } returns flowOf(listOf(habit))
        every { repository.getCompletions("h1") } returns flowOf(
            listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(3), today.minusDays(4))
        )

        useCase(today).test {
            assertEquals(0.5f, awaitItem().single().completionRate, 0.001f)
            awaitComplete()
        }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCaseTest"
```

Esperado: FALLA con `Unresolved reference: GetActiveHabitsUseCase`.

- [ ] **Step 3: Crear el modelo de progreso**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/model/HabitWithProgress.kt`:

```kotlin
package com.gondroid.quoteanime.domain.model

import java.time.LocalDate

/**
 * What the routine screen needs to draw one habit card: the habit itself, the
 * completions inside the visible heatmap window, its streak, and how much of its
 * active window has been completed.
 */
data class HabitWithProgress(
    val habit: Habit,
    val completions: Set<LocalDate>,
    val streak: StreakState,
    val completionRate: Float
)
```

- [ ] **Step 4: Implementar la composición**

Crear `app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCase.kt`:

```kotlin
package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.HabitWithProgress
import com.gondroid.quoteanime.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Streaks are calculated over the full history, while the heatmap only receives
 * the visible window: a personal best older than four months must still show up.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetActiveHabitsUseCase @Inject constructor(
    private val repository: HabitRepository,
    private val calculateStreak: CalculateStreakUseCase
) {
    operator fun invoke(today: LocalDate): Flow<List<HabitWithProgress>> =
        repository.getActiveHabits().flatMapLatest { habits ->
            if (habits.isEmpty()) {
                flowOf(emptyList())
            } else {
                val windowStart = today.minusWeeks(VISIBLE_WEEKS.toLong())
                val progressFlows = habits.map { habit ->
                    repository.getCompletions(habit.id).map { dates ->
                        HabitWithProgress(
                            habit = habit,
                            completions = dates.filter { !it.isBefore(windowStart) }.toSet(),
                            streak = calculateStreak(dates, today),
                            completionRate = completionRate(habit.startDate, today, dates.size)
                        )
                    }
                }
                combine(progressFlows) { it.toList() }
            }
        }

    private fun completionRate(startDate: LocalDate, today: LocalDate, completed: Int): Float {
        val activeDays = ChronoUnit.DAYS.between(startDate, today) + 1
        if (activeDays <= 0) return 0f
        return (completed.toFloat() / activeDays.toFloat()).coerceIn(0f, 1f)
    }

    companion object {
        /** Weeks shown in the heatmap — fits a phone width without scrolling. */
        const val VISIBLE_WEEKS = 17
    }
}
```

- [ ] **Step 5: Ejecutar el test para verificar que pasa**

```bash
./gradlew test --tests "com.gondroid.quoteanime.domain.usecase.GetActiveHabitsUseCaseTest"
```

Esperado: los 4 tests en verde.

- [ ] **Step 6: Ejecutar toda la suite unitaria**

```bash
./gradlew test
```

Esperado: los tests existentes del proyecto y los nuevos, todos en verde. Ningún test previo debe romperse: esta fase no modifica código existente salvo `AppDatabase`, `DatabaseModule` y `RepositoryModule`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/gondroid/quoteanime/domain/model/HabitWithProgress.kt app/src/main/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCase.kt app/src/test/java/com/gondroid/quoteanime/domain/usecase/GetActiveHabitsUseCaseTest.kt
git commit -m "feat(routine): compose habits with streaks and visible completions"
```

---

## Estado al terminar esta fase

El núcleo queda completo y probado, sin ninguna pantalla todavía. La fase 1B (`2026-07-25-mi-rutina-fase1b-ui-recordatorios.md`) construye sobre estas firmas:

- `GetActiveHabitsUseCase(today): Flow<List<HabitWithProgress>>`
- `GetHabitTemplatesUseCase(): Flow<List<HabitTemplate>>`
- `CreateHabitUseCase(...): CreateHabitResult`
- `UpdateHabitUseCase(habit): UpdateHabitResult`
- `ArchiveHabitUseCase(habitId)`
- `ToggleHabitCompletionUseCase(habitId, date, today): ToggleCompletionResult`
- `CalculateStreakUseCase(dates, today): StreakState`
- `HabitRepository.getAllCompletionDates(): Flow<List<LocalDate>>` para la racha global
- `PremiumGate.maxActiveHabits`
- `GetActiveHabitsUseCase.VISIBLE_WEEKS = 17`

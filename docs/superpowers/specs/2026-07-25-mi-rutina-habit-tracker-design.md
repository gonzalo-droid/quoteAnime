# Spec — «Mi rutina»: seguimiento de hábitos con heatmap

Fecha: 2026-07-25
Plataforma: Android (la homologación en iOS es posterior, ver fase 4)
Contexto de producto: `2026-07-25-analisis-producto-quoteanime.md`

## Objetivo

Dar al usuario una razón para volver cada día: una sección donde define hasta tres hábitos, los marca a diario y ve su constancia como un gráfico de celdas al estilo del de contribuciones de GitHub, con frases de anime como refuerzo y wallpapers como recompensa.

Criterio de éxito propuesto, a ajustar con los datos reales de la app: al menos el 25 % de los usuarios activos crea un hábito en la primera semana tras actualizar, y de esos, la mitad sigue marcando a los 7 días.

## Decisiones tomadas

| Decisión | Elección |
|---|---|
| Tipo de hábitos | Plantillas curadas + hábito libre |
| Límite | 3 hábitos activos |
| Fechas | Fecha de inicio obligatoria, fecha de fin opcional |
| Gráfico | Un heatmap por hábito, con el color del hábito |
| Rango visible | Últimas 17 semanas, sin scroll |
| Celdas | Binarias: cumplido o no |
| Marcado retroactivo | Sí, tocando cualquier celda pasada |
| Racha global | Se mantiene marcando al menos un hábito al día |
| Fallo | Rompe la racha; se conserva el récord histórico |
| Recompensas | Galería de logros con frases y wallpapers |
| Recordatorios | Hora y días de la semana por hábito, con acción «Hecho» |
| Persistencia | Room como fuente de verdad + espejo en Realtime Database bajo UID anónimo |
| Navegación | Barra inferior de 3 pestañas |
| Monetización | Gratis, con bandera `isPremium` preparada |
| Idiomas | Español e inglés |
| Medición | Firebase Analytics |

## Modelo de dominio

```kotlin
// domain/model/Habit.kt
data class Habit(
    val id: String,                     // UUID generado en el dispositivo
    val title: String,
    val iconKey: String,                // clave de HabitIcons, no un ImageVector
    val colorIndex: Int,                // 0..7, índice en HabitPalette
    val startDate: LocalDate,
    val endDate: LocalDate?,            // null = indefinido
    val reminderTime: LocalTime?,       // null = sin recordatorio
    val reminderDays: Set<DayOfWeek>,   // vacío si no hay recordatorio
    val templateId: String?,            // null si es hábito libre
    val isArchived: Boolean = false,
    val createdAt: Long
)

// domain/model/HabitTemplate.kt
data class HabitTemplate(
    val id: String,
    val title: String,
    val iconKey: String,
    val order: Int
)

// domain/model/StreakState.kt
data class StreakState(
    val current: Int,
    val best: Int,
    val lastCompletedDate: LocalDate?,
    val completedToday: Boolean
)

// domain/model/Reward.kt
data class Reward(
    val id: String,
    val requiredStreak: Int,            // 3, 7, 21, 50, 100
    val type: RewardType,               // QUOTE | WALLPAPER
    val payloadRef: String,             // id de frase o slug de imagen
    val isUnlocked: Boolean
)
```

`colorIndex` en lugar de un hexadecimal: garantiza contraste sobre el fondo oscuro, evita colores ilegibles y permite retocar la paleta o añadir tema claro sin migrar datos.

`iconKey` en lugar de un `ImageVector`: el dominio no depende de Compose y el valor es persistible.

## Persistencia local (Room v5)

Tres entidades nuevas. La versión de la base sube de 4 a 5 con una migración que solo crea tablas; no toca `favorite_quotes`.

```kotlin
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val iconKey: String,
    val colorIndex: Int,
    val startDate: String,        // ISO yyyy-MM-dd
    val endDate: String?,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val reminderDays: String,     // "MONDAY,WEDNESDAY"; vacío si no hay recordatorio
    val templateId: String?,
    val isArchived: Boolean,
    val createdAt: Long
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("date")]
)
data class HabitCompletionEntity(
    val habitId: String,
    val date: String,             // ISO yyyy-MM-dd
    val completedAt: Long
)

@Entity(tableName = "unlocked_rewards")
data class UnlockedRewardEntity(
    @PrimaryKey val rewardId: String,
    val unlockedAt: Long
)
```

Principio: **no se persiste ningún contador de racha**. La racha siempre se deriva de las filas de `habit_completions`. Así no puede desincronizarse al cambiar de zona horaria, al retroceder el reloj del sistema o al marcar días pasados.

Marcar un día inserta una fila; desmarcarlo la borra.

## Lógica de racha

`CalculateStreakUseCase` recibe una lista de fechas y la fecha de hoy, y devuelve `StreakState`. Es una función pura sin dependencias de Android:

1. Ordenar las fechas distintas de forma descendente.
2. La racha está viva si la más reciente es hoy o ayer; en caso contrario `current = 0`.
3. Desde ese punto, contar hacia atrás mientras cada fecha sea exactamente el día anterior.
4. `best` es la secuencia consecutiva más larga del histórico completo.

**Racha global**: se calcula sobre el conjunto de fechas en que se completó *al menos un* hábito.
**Racha por hábito**: la misma función, filtrando por `habitId`.

El «día» es el día natural en la zona horaria del dispositivo, con corte a medianoche.

Los días anteriores a `startDate` y posteriores a `endDate` no cuentan como fallo: se excluyen del cálculo y del porcentaje de cumplimiento.

## Plantillas de hábitos

Nodo nuevo en Realtime Database:

```
/habitTemplates/{id} → { title: String, iconKey: String, order: Int }
```

`GetHabitTemplatesUseCase` emite lo remoto y cae a una lista compilada en la app (`DefaultHabitTemplates`) si no hay red o el nodo está vacío. Esto permite añadir plantillas nuevas sin publicar una versión.

Plantillas iniciales sugeridas: entrenar, leer, meditar, beber agua, dormir temprano, estudiar, escribir, salir a caminar.

## Use cases

| Use case | Responsabilidad |
|---|---|
| `GetActiveHabitsUseCase` | Flow de hábitos no archivados. El heatmap recibe solo las completions de las últimas 17 semanas; el `StreakState` se calcula sobre el histórico completo, porque la mejor racha puede ser anterior a esa ventana |
| `CreateHabitUseCase` | Valida el límite de 3 activos y las fechas; devuelve error tipado si se supera |
| `UpdateHabitUseCase` | Edición de título, color, fechas y recordatorio; reprograma el recordatorio |
| `ArchiveHabitUseCase` | Archiva y cancela el recordatorio; conserva el histórico |
| `ToggleHabitCompletionUseCase` | Marca o desmarca `(habitId, date)`; rechaza fechas futuras y fuera del rango del hábito |
| `CalculateStreakUseCase` | Función pura de racha |
| `GetHabitTemplatesUseCase` | Plantillas remotas con respaldo local |
| `GetRewardsUseCase` | Catálogo de recompensas con su estado de desbloqueo |
| `CheckRewardUnlockUseCase` | Tras cada marcado, comprueba si se alcanzó un hito y desbloquea |

## Interfaz

### Pantalla «Mi rutina»

**Cabecera**: racha global en grande, progreso de hoy (`2 de 3`), acceso a la galería de logros.

**Lista de hábitos**, una tarjeta cada uno:
- Título, icono y botón grande de marcar hoy (área táctil mínima de 48 dp).
- Heatmap.
- Racha actual, mejor racha y porcentaje de cumplimiento desde `startDate`.
- Menú de edición y archivado.

**Estado vacío**: explicación breve y botón para crear el primer hábito, con las plantillas visibles de inmediato.

**Botón de añadir**: oculto al alcanzar 3 hábitos activos, sustituido por un texto que explica el límite.

### Heatmap

- Rejilla de 17 columnas (semanas) × 7 filas (días), la última columna es la semana en curso.
- `Canvas` de Compose, celdas de unos 14 dp con esquinas redondeadas y 3 dp de separación. Cabe en el ancho de un móvil sin scroll.
- Celda cumplida: color del hábito. Celda no cumplida dentro del rango activo: color de superficie con baja opacidad. Celda fuera del rango del hábito (antes del inicio, después del fin) o futura: sin relleno ni borde.
- Etiquetas de mes discretas sobre la rejilla.
- Interacción: `detectTapGestures` traduce el offset a índice de celda y de ahí a `LocalDate`; alterna ese día. Los toques en celdas futuras o fuera de rango se ignoran.
- Accesibilidad: descripción de contenido por celda con la fecha y el estado.

### Crear y editar hábito

Hoja inferior modal con:
1. Plantillas en `FlowRow` de chips, más la opción «Crear el mío» con campo de texto.
2. Selector de color: 8 muestras circulares de `HabitPalette`.
3. Fecha de inicio (por defecto hoy) mediante el `DatePicker` de Material3.
4. Interruptor de fecha de fin, con selector cuando está activo. Validación: la fecha de fin debe ser posterior a la de inicio.
5. Interruptor de recordatorio, con `TimePicker` y chips de días de la semana. Al activarlo por primera vez se pide `POST_NOTIFICATIONS` reutilizando el flujo que ya existe en `SettingsScreen`.

### Galería de logros

Rejilla de recompensas. Las desbloqueadas se muestran a todo color con acceso a descargar o compartir el wallpaper reutilizando `ShareUtils`; las pendientes aparecen en gris con el hito necesario. Hitos: 3, 7, 21, 50 y 100 días de racha global.

Al desbloquear, animación de celebración sobre la pantalla de rutina.

## Recordatorios

**Canal nuevo** `habit_reminders`, separado de `quote_notifications`, para que se puedan silenciar por separado.

**Programación**: un trabajo único por hábito, con nombre `habit_reminder_<habitId>`. `HabitReminderScheduler` calcula el próximo instante que cumpla la hora y uno de los días seleccionados, encola un `OneTimeWorkRequest` con ese retraso y, al ejecutarse, `HabitReminderWorker` vuelve a encolarse para la siguiente ocurrencia. Se evita así el permiso de alarmas exactas de Android 12+, con la misma tolerancia de ventana que ya se acepta para las frases.

Al archivar o editar un hábito se cancela por nombre y se reprograma.

**Contenido**: título del hábito, una frase de anime obtenida con `GetRandomQuoteUseCase` y una acción **Hecho**.

**Acción Hecho**: `HabitReminderReceiver` recibe el `habitId`, marca la completion del día usando `goAsync()` y cancela la notificación. No abre la app. Tocar el cuerpo de la notificación navega a «Mi rutina».

No se muestra recordatorio si el hábito ya está marcado ese día, ni fuera del rango de fechas del hábito.

## Sincronización

`FirebaseAuth` anónimo (`firebase-auth-ktx`), sin ninguna pantalla de login. El UID se crea en el primer arranque.

- Room es la única fuente de verdad para la interfaz.
- Tras cada escritura local se sube el espejo a `/users/{uid}/habits` y `/users/{uid}/completions`, en segundo plano y tolerante a fallos.
- Solo se descarga al arrancar si Room está vacío, para no pisar datos locales.
- Sin resolución de conflictos en esta fase: no hay uso simultáneo en dos dispositivos mientras no exista cuenta vinculada.

**Requisito de infraestructura**: las reglas de Realtime Database deben permitir escribir en `/users/$uid` solo a `auth.uid === $uid`, manteniendo el resto de nodos de solo lectura. Sin esto, la sincronización no debe activarse.

## Navegación

Barra inferior con tres destinos: **Frases** (`home`), **Mi rutina** (`routine`), **Catálogo** (`catalog`). Ajustes se mantiene accesible desde el icono superior de Frases.

`AppNavGraph` pasa a tener un `Scaffold` con `NavigationBar` que se muestra solo en esos tres destinos; splash, onboarding, tutorial del widget y ajustes siguen sin barra. El deep link del widget sigue entrando directamente en Frases.

Rutas nuevas: `Screen.Routine` (`routine`), `Screen.Rewards` (`rewards`).

## Activación

- **Usuarios nuevos**: paso adicional y opcional al final del onboarding para elegir el primer hábito de las plantillas. Se puede omitir.
- **Usuarios existentes**: al abrir la versión con la feature, un aviso destacado una sola vez que lleva a «Mi rutina», controlado con la clave `routine_intro_seen` en DataStore.

## Medición

Firebase Analytics (`firebase-analytics-ktx`, dependencia nueva). Eventos:

| Evento | Parámetros |
|---|---|
| `routine_tab_opened` | — |
| `habit_created` | `template_id`, `is_custom`, `has_reminder`, `has_end_date` |
| `habit_completed` | `habit_id`, `is_retroactive`, `source` (app / notificación) |
| `habit_archived` | `days_active` |
| `streak_milestone` | `days` |
| `streak_broken` | `previous_streak` |
| `reward_unlocked` | `reward_id`, `type` |

## Monetización preparada

El límite de 3 hábitos y el catálogo de recompensas se consultan a través de `PremiumGate`, con `isPremium` cableado a `false`. Activar premium más adelante consistirá en implementar Play Billing detrás de esa interfaz, sin tocar los use cases.

## Idiomas

Los textos nuevos se añaden en español e inglés. Se traducen también los 29 strings existentes.

Se mueve el español de `values/` a `values-es/` y el inglés pasa a ser el predeterminado en `values/`, de modo que un usuario con el idioma del sistema en, por ejemplo, francés vea inglés y no español. El cambio es mecánico pero afecta a toda la app, y debe verificarse pantalla por pantalla.

## Cambios en el proyecto

**Gradle**:
- Activar `isCoreLibraryDesugaringEnabled = true` y añadir `coreLibraryDesugaring(libs.desugar.jdk.libs)`. Sin esto, `java.time` no está disponible con minSdk 24.
- Dependencias nuevas: `firebase-auth-ktx`, `firebase-analytics-ktx`.

**Documentación**: corregir `CLAUDE.md`, que describe Firestore cuando la app usa Realtime Database, y actualizar el esquema real (`categories: List<String>`, `animeSlug`, nodo `/imagenes/{slug}`).

**Estructura nueva**:

```
data/local/db/dao/       HabitDao, HabitCompletionDao, UnlockedRewardDao
data/local/db/entity/    HabitEntity, HabitCompletionEntity, UnlockedRewardEntity
data/remote/             HabitTemplateRemoteDataSource, HabitSyncDataSource
data/repository/         HabitRepositoryImpl, RewardRepositoryImpl
domain/model/            Habit, HabitTemplate, StreakState, Reward, RewardType
domain/repository/       HabitRepository, RewardRepository
domain/usecase/          los nueve use cases listados arriba
presentation/routine/    RoutineScreen, RoutineViewModel, RoutineUiState,
                         HabitCard, HabitHeatmap, HabitEditorSheet, HabitPalette, HabitIcons
presentation/rewards/    RewardsScreen, RewardsViewModel
notification/            HabitReminderScheduler, HabitReminderReceiver
worker/                  HabitReminderWorker
di/                      PremiumGate y bindings nuevos
```

## Fases

1. **Fase 1 — Núcleo**: desugaring, Room v5, modelo, use cases, `RoutineScreen` con heatmap interactivo, editor de hábitos, recordatorios con acción «Hecho», barra inferior, analítica, inglés.
2. **Fase 2 — Recompensas y respaldo**: galería de logros, desbloqueos, autenticación anónima y espejo en Realtime Database con sus reglas de seguridad.
3. **Fase 3 — Widget**: widget Glance con la racha y marcado rápido de hábitos.
4. **Fase 4 — iOS**: homologación en `/Volumes/Neko/apps_ios/quoteAnime` con el mismo modelo de datos y el mismo esquema remoto.

## Pruebas

**Unitarias** (sin Android):
- `CalculateStreakUseCase`: racha vacía, de un día, consecutiva, rota, marcada ayer pero no hoy, con marcado retroactivo que une dos tramos, con cambio de mes y de año.
- `CreateHabitUseCase`: rechaza el cuarto hábito activo; permite crear tras archivar uno.
- `ToggleHabitCompletionUseCase`: rechaza fechas futuras y fechas fuera del rango del hábito.
- `HabitReminderScheduler`: cálculo de la próxima ocurrencia con distintos conjuntos de días, incluido el salto de semana y la hora ya pasada hoy.
- `CheckRewardUnlockUseCase`: desbloqueo en cada hito y ausencia de desbloqueo duplicado.

**Room in-memory**: DAOs, borrado en cascada al eliminar un hábito, unicidad de `(habitId, date)`.

**Compose UI**, siguiendo el patrón de `SettingsShareAndSocialUiTest`: estado vacío, marcar y desmarcar hoy, toque en celda pasada, límite de 3 hábitos, validación de fechas en el editor.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El heatmap resulta ilegible en pantallas pequeñas | Celda calculada a partir del ancho disponible, con mínimo de 10 dp; probar en 320 dp |
| Los recordatorios se retrasan por optimizaciones del fabricante | Es una limitación conocida de WorkManager; se asume la misma ventana que las frases y se comunica en la interfaz |
| La sincronización pisa datos locales | Solo se descarga si Room está vacío; nunca se fusiona en esta fase |
| El cambio de `values/` a inglés deja textos sin traducir | Revisión pantalla por pantalla antes de publicar |
| La feature no retiene y el trabajo se desperdicia | La analítica de la fase 1 decide si se invierte en las fases 2 y 3 |

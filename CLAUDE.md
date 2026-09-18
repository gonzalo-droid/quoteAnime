# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build                  # Full build
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew test --tests "com.gondroid.quoteanime.data.repository.QuoteRepositoryImplTest"  # Single test class
```

## Project Overview

Motivational anime quotes Android app. Quotes come from **Firebase Realtime Database** (not Firestore). Users save quotes to **favorites**, stored locally in **Room**. It also ships **Mi Rutina**, a habit tracker (Room), and a **Premium** subscription sold through Google Play Billing. Notifications via WorkManager, three home-screen widgets via Glance, ads via AdMob.

- **Language**: Kotlin | **UI**: Jetpack Compose + Material3
- **minSdk**: 24, **targetSdk**: 36, **compileSdk**: 36
- **Package**: `com.gondroid.quoteanime`
- **DI**: Hilt | **DB**: Room v6 (favorites + habits) | **Remote**: Firebase RTDB | **Preferences**: DataStore | **Widgets**: Glance | **Billing**: `billing-ktx` 9.1.0 | **Ads**: AdMob

> **Firebase**: `google-services.json` lives in `app/src/` (not `app/`) and **is committed**. Realtime Database must be enabled in the Firebase project.

## Realtime Database Schema

```
/quotes/{index}        → id: Long, quote: String, author: String, anime: String
/habitTemplates/{id}   → title (string-resource key), iconKey, order, themeColorIndex?, themeKey?, isPremiumOnly
/imagenes              → fetched once by QuoteRemoteDataSource
```

Quote categories are **derived** from the distinct `anime` values — there is no `/categories` node. If `/habitTemplates` is empty or missing, the habit editor falls back to the local `DefaultHabitTemplates.ALL`.

## Architecture

Single-module, Clean Architecture with MVVM in the presentation layer.

```
com.gondroid.quoteanime/
├── data/
│   ├── local/db/                  # Room v6: dao/ (FavoriteQuote, Habit, HabitCompletion), entity/, migrations
│   ├── local/datastore/           # UserPreferencesDataStore — prefs, onboarding flags, IS_PREMIUM
│   ├── remote/                    # Quote + HabitTemplate RTDB sources (callbackFlow), BillingClientFactory, dto/
│   └── repository/                # Quote, UserPreferences, Habit, Billing implementations
├── domain/                        # model/, repository/ (interfaces), usecase/
├── presentation/
│   ├── navigation/                # AppNavGraph, Screen sealed class
│   ├── splash/ onboarding/ home/ catalog/ settings/
│   ├── routine/                   # Mi Rutina
│   ├── subscription/              # Paywall
│   ├── widget/                    # HabitWidgetConfigureActivity
│   └── ads/ components/ common/ web/
├── worker/                        # 6 workers — see Notification & Widget sections
├── widget/                        # 3 Glance widgets
├── notification/                  # NotificationHelper, schedulers, HabitReminderReceiver
├── analytics/                     # RoutineAnalytics (Firebase Analytics)
└── di/                            # AppModule, DatabaseModule, RepositoryModule, PremiumGate
```

`PremiumGate` lives in `di/`, not `domain/` — it holds the plan limits.

## Domain Use Cases

| Use Case | Description |
|---|---|
| `GetCategoriesUseCase` | Flow of categories, derived from the `anime` field in RTDB |
| `GetQuotesByCategoryUseCase` | Flow of quotes for a category, with `isFavorite` merged from Room |
| `GetRandomQuoteUseCase` | Suspend — used by WorkManager & Widget |
| `GetFavoriteQuotesUseCase` | Flow of locally saved favorites |
| `ToggleFavoriteUseCase` | Adds or removes favorite based on `quote.isFavorite` |
| `GetAllQuotesUseCase` | Flow of every quote, `isFavorite` merged from Room — feeds Home |
| `UpdateUserPreferencesUseCase` | Writes categories, time window, frequency, enabled flag, widget settings to DataStore |

32 use cases in total. Beyond quotes: habits (`Create/Update/Archive/Unarchive/Delete`, `ToggleHabitCompletion`, `CalculateStreak`, `GetGlobalStreak`, `GetHabitTemplates`), billing (`GetSubscriptionOffers`, `LaunchSubscriptionPurchase`, `ObservePurchaseEvents`, `RestorePurchases`, `AcknowledgePendingPurchases`, `GetManageSubscriptionUrl`), premium (`Observe/SetPremiumStatus`) and flags (`Get/SetOnboardingCompleted`, `IsRoutineIntroSeen`/`SetRoutineIntroSeen`).

## Key Wiring

- **`isFavorite` merging**: `QuoteRepositoryImpl.getQuotesByCategory` and `getAllQuotes` use `combine()` to merge the RTDB Flow with `FavoriteQuoteDao.getFavoriteIds()`, so the UI always has an up-to-date favorite state without extra calls.
- **Application class**: `QuoteAnimeApplication` — `@HiltAndroidApp`, implements `Configuration.Provider` to inject `HiltWorkerFactory` into WorkManager (manual init). The `WorkManagerInitializer` startup provider is removed in the manifest to avoid double-init.
- **Hilt + WorkManager**: Workers must use `@HiltWorker` + `@AssistedInject`.
- **Navigation**: `AppNavGraph` uses a `sealed class Screen(route)` pattern. Screens receive navigation lambdas, not the NavController directly.
- **Repository binding**: `RepositoryModule` uses `@Binds` (abstract module) — keep it abstract, not `object`.
- **Startup** (`QuoteAnimeApplication.onCreate`): `MobileAds.initialize()` (synchronous, main thread), widget update scheduling, routine widget daily refresh, then `syncPremiumEntitlement()` → `RestorePurchasesUseCase`. The Premium entitlement is re-synced from Play on **every** start.
- **Startup routing**: `SplashViewModel` reads `GetOnboardingCompletedUseCase` and routes to `Onboarding` or `Home`.
- **Premium gating**: `PremiumGate.maxActiveHabits(isPremium)` — `FREE_HABIT_LIMIT = 3`. The entitlement flag is `IS_PREMIUM` in DataStore; ads are hidden when it is set.

## HomeScreen & CatalogScreen

### HomeScreen
- Feed full-screen con `VerticalPager`, una frase por página, alimentado por `GetAllQuotesUseCase` (con `isFavorite` ya mergeado desde Room) y **filtrado por los animes elegidos en Ajustes** (`selectedCategoryIds`, vacío = todos; regla `Quote.isInCategories`, igual que iOS). El pager se reinicia al cambiar la selección
- El toggle de favorito usa `ToggleFavoriteUseCase`; Room emite y el flow lo propaga al UI sin setState manual
- `Screen.Home` acepta `home?quoteId={quoteId}`: el widget de frase abre el feed posicionado en esa frase
- Recibe `onNavigateToCatalog: (categoryId: String?) -> Unit` (no el NavController)

### CatalogScreen
- Filtros: `CatalogFilter` sealed class — `Favorites`, `All`, `ByEmotion(categoryId)`
- `CatalogViewModel` usa `flatMapLatest` sobre `_selectedFilter: CatalogFilter?`: `Favorites` → `getFavoriteQuotes()` (Room), `All` → `getAllQuotes()`, `ByEmotion` → `getQuotesByCategory()` (RTDB), y `null` → **lista vacía** (ya no significa "favoritos"). El cambio de filtro es instantáneo porque Kotlin Flows cancelan el anterior
- El filtro inicial viene de `SavedStateHandle` — permite navegar desde Home con categoría preseleccionada
- `key = { it.id }` en `LazyColumn` evita recomposiciones innecesarias al hacer toggle de favorito

### Componentes compartidos
- `QuoteCard` (`presentation/components/`) — card reutilizable con texto, autor e `IconButton` de favorito; `maxLines` configurable

### Navegación actualizada
- `Screen.Catalog` tiene `routeWithArg = "catalog?categoryId={categoryId}"` con argumento nullable
- `Screen.Home` tiene `routeWithArg = "home?quoteId={quoteId}"` (deep link desde el widget)
- Destinos: `Splash`, `Onboarding`, `Home`, `Catalog`, `Settings`, `WidgetTutorial`, `Routine`, `HabitEditor`, `HabitDetail`, `Paywall`, `WebView`
- `HomeScreen` recibe `onNavigateToCatalog: (categoryId: String?) -> Unit` (no el NavController directamente)

## Settings Screen (Personalización)

**Estado**: `SettingsUiState` — data class con todas las preferencias + `toUserPreferences()` helper + `allCategoriesSelected` computed property.

**ViewModel** (`SettingsViewModel`): combina `GetCategoriesUseCase` + `GetUserPreferencesUseCase` + `ObservePremiumStatusUseCase` con `combine()`. Cada acción escribe en DataStore **y** reprograma el scheduler con el estado nuevo (no espera al flow reactivo para evitar race conditions).

| Acción ViewModel | Comportamiento |
|---|---|
| `onCategoryToggled(id)` | Toggle en `selectedCategoryIds`, reschedula si notificaciones ON |
| `onSelectAllCategories()` | Vacía `selectedCategoryIds` (= todas) |
| `onNotificationsEnabled()` | Persiste + schedula — llamado desde la Screen tras confirmar el permiso |
| `onNotificationsDisabled()` | Persiste + cancela worker |
| `onTimeRangeChanged(startH, startM, endH, endM)` | Persiste la ventana horaria + reschedula |
| `onFrequencyChanged(timesPerDay)` | Persiste + reschedula con nueva frecuencia (1–10 por día) |
| `onWidgetSizeChanged(size)` | Persiste + `widgetScheduler.triggerImmediateUpdate()` |
| `onWidgetUpdateTimesChanged(times)` | Persiste + `widgetScheduler.schedule(times)` |
| `onTestNotification()` | Encola `QuoteNotificationWorker` una vez. Ninguna pantalla lo llama hoy |
| `onPermissionDeniedPermanently()` | Marca flag en UiState para deshabilitar el switch |

**Flujo de permiso `POST_NOTIFICATIONS` (API 33+)**: la Screen gestiona el `rememberLauncherForActivityResult` y solo llama a `viewModel.onNotificationsEnabled()` si el permiso es concedido. Si es denegado definitivamente, muestra un `Snackbar` con acción que abre los ajustes del sistema.

**UI**:
- Animes: `FilterChip`s en un `FlowRow` con "Todos los animes"; selección múltiple, vacía = todas (`allCategoriesSelected`). Afecta feed de Inicio, notificaciones y widget. La lista carga aparte (`categoriesLoading`): el resto de Ajustes no la espera (sin red el listener de RTDB no responde)
- Notificaciones: `Switch` en `ListItem`; cuando ON aparecen ventana horaria y frecuencia
- Ventana horaria: inicio y fin, cada uno con Material3 `TimePicker`
- Frecuencia: `Slider` de 1 a 10 (veces por día). Refrescos del widget: `Slider` de 1 a 8
- El enum `NotificationFrequency` (3 valores) es **código muerto**: nada lo usa, la preferencia es un `Int`

## Widget (Glance API)

| Clase | Responsabilidad |
|---|---|
| `QuoteWidget` | `GlanceAppWidget` — UI declarativa con Glance Composables; lee estado de `PreferencesGlanceStateDefinition`; 3 estados: loading / error / quote |
| `QuoteWidgetReceiver` | `GlanceAppWidgetReceiver` — recibe `APPWIDGET_UPDATE` del sistema; encola `UpdateQuoteWidgetWorker` |
| `RefreshQuoteAction` | `ActionCallback` — ejecutado al tocar el botón refresh; muestra loading y encola el worker |
| `UpdateQuoteWidgetWorker` | `CoroutineWorker + @HiltWorker` — obtiene frase con `GetRandomQuoteUseCase` (RTDB); actualiza estado de **todas** las instancias del widget; marca error si falla |
| `QuoteWidgetState` | Claves `Preferences`: `QUOTE_TEXT`, `QUOTE_AUTHOR`, `QUOTE_ID`, `QUOTE_ANIME`, `IS_LOADING`, `HAS_ERROR`, `BACKGROUND_IMAGE_URI` |

**Flujo de actualización**: `onUpdate / RefreshQuoteAction` → `UpdateQuoteWidgetWorker` → `updateAppWidgetState()` → `QuoteWidget().update()` → recomposición de Glance

**Múltiples instancias**: `UpdateQuoteWidgetWorker` itera sobre todos los `glanceIds` de `GlanceAppWidgetManager` para actualizar cada widget independientemente.

**Nota sobre lock screen**: Los widgets de pantalla de bloqueo fueron eliminados en Android 5.0 (API 21). Con minSdk 24, no son posibles. Las notificaciones cubren ese caso de uso. `widgetCategory="home_screen"` en el XML es correcto.

**Widgets de Mi Rutina**: `RoutineSummaryWidget` (todos los hábitos activos) y `HabitWidget` (uno por instancia). El hábito de cada `HabitWidget` se elige en `HabitWidgetConfigureActivity` al agregarlo — la única Activity de configuración de widget de la app. Se actualizan con `UpdateRoutineSummaryWidgetWorker` y `UpdateHabitWidgetWorker`; `RoutineWidgetScheduler` agenda el refresco diario.

## Notification & WorkManager System

| Clase | Responsabilidad |
|---|---|
| `NotificationHelper` | Crea los canales (`quote_notifications`, `habit_reminders`), construye y muestra la notificación con `BigTextStyle` |
| `NotificationScheduler` | Encola **un** `OneTimeWorkRequest` único (`quote_notification_next`, `REPLACE`) con delay hasta el próximo horario de `QuoteNotificationSlotCalculator`, con red requerida. Cada `schedule()` cancela además el periódico viejo (`quote_notification_work`) — así migran solas las instalaciones anteriores |
| `HabitReminderScheduler` | Un `OneTimeWorkRequest` único por hábito (`ExistingWorkPolicy.REPLACE`) → `HabitReminderWorker`; `NextReminderCalculator` calcula el próximo disparo |
| `HabitReminderReceiver` | Maneja la acción "Done" de la notificación de recordatorio |
| `QuoteNotificationWorker` | `CoroutineWorker` + `@HiltWorker`; muestra la notificación si está dentro de la ventana (con 30 min de gracia al final) y **encadena el próximo horario** llamando a `NotificationScheduler.schedule()` como último paso — también al fallar, nunca al reintentar. Reintenta hasta 3 veces |

**Flujo de scheduling**: `SettingsViewModel` → `NotificationScheduler.schedule(prefs)` → `enqueueUniqueWork(REPLACE)` → `QuoteNotificationWorker.doWork()` → `NotificationHelper.showQuoteNotification()` → `NotificationScheduler.schedule(prefs)` (próximo horario)

**Reparto de horarios**: `QuoteNotificationSlotCalculator` (puro, con tests) reparte N notificaciones de punta a punta en la ventana — 3 por día en 08:00–22:00 → 08:00, 15:00, 22:00. Soporta ventanas que cruzan la medianoche (22:00–02:00) e interpreta inicio == fin como 24 h.

**No usar `runCatching` en el worker**: atrapa también la cancelación, y un worker cancelado porque el usuario apagó las notificaciones seguiría y encadenaría el próximo horario. Por eso existe `attempt { }`, que relanza `CancellationException`.

**Por qué NO se usa `SCHEDULE_EXACT_ALARM`**: WorkManager (vía `JobScheduler`/`AlarmManager`) nunca adelanta, solo atrasa; para frases motivacionales ese margen es aceptable y evita el diálogo de permiso especial de Android 12+. El margen de gracia de 30 min del worker existe justamente por esos atrasos.

**WorkManager y reinicios**: WorkManager registra su propio `BroadcastReceiver` para `BOOT_COMPLETED` internamente. El permiso `RECEIVE_BOOT_COMPLETED` en el manifest es suficiente — no hace falta un `BootReceiver` propio.

## Permissions (AndroidManifest)

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Requerido en API 33+ para mostrar notificaciones |
| `RECEIVE_BOOT_COMPLETED` | WorkManager lo usa internamente para reprogramar tras reinicio |

## Mi Rutina (habit tracker)

- Room: `HabitEntity` + `HabitCompletionEntity`; las completions tienen `ForeignKey` a hábitos con `onDelete = CASCADE` — borrar un hábito borra su historial
- Hábitos archivables y restaurables; los activos están limitados por `PremiumGate` (3 en plan gratis)
- Plantillas temáticas desde `/habitTemplates` en RTDB, con fallback local `DefaultHabitTemplates.ALL`; algunas son `isPremiumOnly` y abren el Paywall
- Rachas: `CalculateStreakUseCase` (por hábito) y `GetGlobalStreakUseCase`

## Premium & Billing

- Producto de suscripción: `premium_subscription` (declarado en `BillingRepository`). Debe existir en Play Console con al menos un base plan, si no el paywall queda sin planes
- `BillingRepositoryImpl` envuelve `BillingClient` (creado por `BillingClientFactory`): conexión, `ProductDetails`, purchase flow, acknowledge
- `AcknowledgePurchasesWorker` reconoce compras pendientes — Play las reembolsa si no se reconocen en 3 días
- El entitlement se guarda en DataStore (`IS_PREMIUM`) y se re-sincroniza con Play en cada arranque

## Zonas peligrosas

- **Migraciones de Room**: la DB es v6 con `exportSchema = true` (schemas en `app/schemas/`). `DatabaseModule` registra `MIGRATION_4_5` y `MIGRATION_5_6`, y solo permite reset destructivo desde las versiones 1–3. **Subir la versión exige escribir `MIGRATION_6_7`**: sin ella, los usuarios en v6 no tienen camino y la app crashea al abrir la DB. La 4→5 existe para no borrar los favoritos de usuarios reales.
- **AdMob**: los ad unit IDs son `buildConfigField` por build type en `app/build.gradle.kts` (debug = IDs de prueba de Google, release = producción). El application ID es `meta-data` en el manifest.

## Convenciones

- Commits: Conventional Commits con ámbito cuando aplica (`feat(routine):`, `fix(billing):`, `docs:`)
- `CHANGELOG.md` es el historial de versiones; el README solo enlaza a él
- Versión en `app/build.gradle.kts` (`versionName` / `versionCode`)

<!-- project-memory: rev=caff5b2 date=2026-09-18 -->

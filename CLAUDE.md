# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build                  # Full build
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew test --tests "com.gondroid.tokensclaude.ExampleUnitTest"  # Single test class
```

## Project Overview

Motivational quotes Android app. Quotes and categories come from **Firebase Firestore** (remote). Users can save quotes to **favorites**, stored locally in **Room**. Notification scheduling via WorkManager. Home/lock-screen widget via Glance API.

- **Language**: Kotlin | **UI**: Jetpack Compose + Material3
- **minSdk**: 24, **targetSdk**: 36
- **Package**: `com.gondroid.quoteanime`
- **DI**: Hilt | **DB**: Room (favorites only) | **Remote**: Firestore | **Preferences**: DataStore | **Widget**: Glance API

> **Firebase setup required**: add `google-services.json` to `app/` after registering the app in the Firebase console. Enable Firestore in the project.

## Firestore Schema

```
/categories/{id}   → name: String, imageUrl: String
/quotes/{id}       → text: String, author: String, categoryId: String
```

## Architecture

Single-module, Clean Architecture with MVVM in the presentation layer.

```
com.gondroid.quoteanime/
├── data/
│   ├── local/
│   │   ├── db/                    # Room: AppDatabase, FavoriteQuoteDao, FavoriteQuoteEntity
│   │   └── datastore/             # UserPreferencesDataStore (DataStore<Preferences>)
│   ├── remote/
│   │   ├── QuoteRemoteDataSource  # Firestore access (callbackFlow + suspend)
│   │   └── dto/                   # QuoteDto, CategoryDto + DocumentSnapshot mappers
│   └── repository/                # QuoteRepositoryImpl, UserPreferencesRepositoryImpl
├── domain/
│   ├── model/                     # Quote, Category, UserPreferences, NotificationFrequency
│   ├── repository/                # QuoteRepository, UserPreferencesRepository (interfaces)
│   └── usecase/                   # One class per use case (see list below)
├── presentation/
│   ├── navigation/                # AppNavGraph, Screen sealed class
│   ├── home/                      # HomeScreen + HomeViewModel
│   ├── catalog/                   # CatalogScreen + CatalogViewModel
│   └── settings/                  # SettingsScreen + SettingsViewModel
├── worker/                        # QuoteNotificationWorker (HiltWorker)
├── widget/                        # QuoteWidget (Glance), QuoteWidgetReceiver
├── notification/                  # NotificationHelper
└── di/                            # AppModule, DatabaseModule, RepositoryModule
```

## Domain Use Cases

| Use Case | Description |
|---|---|
| `GetCategoriesUseCase` | Flow of categories from Firestore |
| `GetQuotesByCategoryUseCase` | Flow of quotes for a category, with `isFavorite` merged from Room |
| `GetRandomQuoteUseCase` | Suspend — used by WorkManager & Widget |
| `GetFavoriteQuotesUseCase` | Flow of locally saved favorites |
| `ToggleFavoriteUseCase` | Adds or removes favorite based on `quote.isFavorite` |
| `UpdateUserPreferencesUseCase` | Writes categories, time, frequency, enabled flag to DataStore |

## Key Wiring

- **`isFavorite` merging**: `QuoteRepositoryImpl.getQuotesByCategory` uses `combine()` to merge the Firestore Flow with `FavoriteQuoteDao.getFavoriteIds()`, so the UI always has an up-to-date favorite state without extra calls.
- **Application class**: `QuoteAnimeApplication` — `@HiltAndroidApp`, implements `Configuration.Provider` to inject `HiltWorkerFactory` into WorkManager (manual init). The `WorkManagerInitializer` startup provider is removed in the manifest to avoid double-init.
- **Hilt + WorkManager**: Workers must use `@HiltWorker` + `@AssistedInject`.
- **Navigation**: `AppNavGraph` uses a `sealed class Screen(route)` pattern. Screens receive navigation lambdas, not the NavController directly.
- **Repository binding**: `RepositoryModule` uses `@Binds` (abstract module) — keep it abstract, not `object`.
- **Firestore `whereIn` limit**: `getRandomQuote()` uses `whereIn("categoryId", ...)` — Firestore caps this at 30 values. If categories can exceed 30, split into multiple queries.

## HomeScreen & CatalogScreen

### HomeScreen
- Muestra una frase destacada aleatoria basada en categorías del usuario (`GetRandomQuoteUseCase`)
- Los chips de categoría navegan a `CatalogScreen` con `categoryId` preseleccionado; "Mis favoritos" navega con `categoryId = null`
- El estado `isFavorite` de la frase destacada se observa reactivamente desde Room via `ObserveFavoriteStatusUseCase` — se cancela y reabre el job cada vez que se carga una nueva frase
- "Refresh" carga una nueva frase aleatoria; el toggle de favorito actualiza Room y el flow lo propaga al UI sin setState manual

### CatalogScreen
- Filtros: `FilterChip` "Favoritos" (tab null) + un chip por categoría en scroll horizontal
- `CatalogViewModel` usa `flatMapLatest` sobre `_selectedCategoryId`: si es `null` → `getFavoriteQuotes()` (Room); si es `categoryId` → `getQuotesByCategory()` (Firestore). El cambio de tab es instantáneo porque Kotlin Flows cancelan el anterior automáticamente
- El `selectedCategoryId` inicial viene de `SavedStateHandle` — permite navegar desde Home con categoría preseleccionada
- `key = { it.id }` en `LazyColumn` evita recomposiciones innecesarias al hacer toggle de favorito

### Componentes compartidos
- `QuoteCard` (`presentation/components/`) — card reutilizable con texto, autor e `IconButton` de favorito; `maxLines` configurable

### Navegación actualizada
- `Screen.Catalog` tiene `routeWithArg = "catalog?categoryId={categoryId}"` con argumento nullable
- `HomeScreen` recibe `onNavigateToCatalog: (categoryId: String?) -> Unit` (no el NavController directamente)

## Settings Screen (Personalización)

**Estado**: `SettingsUiState` — data class con todas las preferencias + `toUserPreferences()` helper + `allCategoriesSelected` computed property.

**ViewModel** (`SettingsViewModel`): combina `GetCategoriesUseCase` + `GetUserPreferencesUseCase` con `combine()`. Cada acción escribe en DataStore **y** reprograma el scheduler con el estado nuevo (no espera al flow reactivo para evitar race conditions).

| Acción ViewModel | Comportamiento |
|---|---|
| `onCategoryToggled(id)` | Toggle en `selectedCategoryIds`, reschedula si notificaciones ON |
| `onSelectAllCategories()` | Vacía `selectedCategoryIds` (= todas) |
| `onNotificationsEnabled()` | Persiste + schedula — llamado desde la Screen tras confirmar el permiso |
| `onNotificationsDisabled()` | Persiste + cancela worker |
| `onTimeChanged(h, m)` | Persiste + reschedula con nueva hora |
| `onFrequencyChanged(f)` | Persiste + reschedula con nueva frecuencia |
| `onPermissionDeniedPermanently()` | Marca flag en UiState para deshabilitar el switch |

**Flujo de permiso `POST_NOTIFICATIONS` (API 33+)**: la Screen gestiona el `rememberLauncherForActivityResult` y solo llama a `viewModel.onNotificationsEnabled()` si el permiso es concedido. Si es denegado definitivamente, muestra un `Snackbar` con acción que abre los ajustes del sistema.

**UI**:
- Categorías: `FlowRow` + `FilterChip` (incluye chip "Todas" que vacía la selección)
- Notificaciones: `Switch` en `ListItem`; cuando ON aparecen hora y frecuencia
- Hora: `ListItem` clickable → `AlertDialog` con Material3 `TimePicker` (formato 24h)
- Frecuencia: `SingleChoiceSegmentedButtonRow` con los 3 valores de `NotificationFrequency`

## Widget (Glance API)

| Clase | Responsabilidad |
|---|---|
| `QuoteWidget` | `GlanceAppWidget` — UI declarativa con Glance Composables; lee estado de `PreferencesGlanceStateDefinition`; 3 estados: loading / error / quote |
| `QuoteWidgetReceiver` | `GlanceAppWidgetReceiver` — recibe `APPWIDGET_UPDATE` del sistema; encola `UpdateQuoteWidgetWorker` |
| `RefreshQuoteAction` | `ActionCallback` — ejecutado al tocar el botón refresh; muestra loading y encola el worker |
| `UpdateQuoteWidgetWorker` | `CoroutineWorker + @HiltWorker` — obtiene frase de Firestore; actualiza estado de **todas** las instancias del widget; marca error si falla |
| `QuoteWidgetState` | Claves `Preferences` para el estado del widget (`QUOTE_TEXT`, `QUOTE_AUTHOR`, `IS_LOADING`, `HAS_ERROR`) |

**Flujo de actualización**: `onUpdate / RefreshQuoteAction` → `UpdateQuoteWidgetWorker` → `updateAppWidgetState()` → `QuoteWidget().update()` → recomposición de Glance

**Múltiples instancias**: `UpdateQuoteWidgetWorker` itera sobre todos los `glanceIds` de `GlanceAppWidgetManager` para actualizar cada widget independientemente.

**Nota sobre lock screen**: Los widgets de pantalla de bloqueo fueron eliminados en Android 5.0 (API 21). Con minSdk 24, no son posibles. Las notificaciones del Paso 3 cubren ese caso de uso. `widgetCategory="home_screen"` en el XML es correcto.

## Notification & WorkManager System

| Clase | Responsabilidad |
|---|---|
| `NotificationHelper` | Crea el canal (`quote_notifications`), construye y muestra la notificación con `BigTextStyle` |
| `NotificationScheduler` | Envuelve WorkManager: `schedule(UserPreferences)` calcula el delay inicial hasta la hora elegida y encola `PeriodicWorkRequest`; `cancel()` cancela por nombre único |
| `QuoteNotificationWorker` | `CoroutineWorker` + `@HiltWorker`; lee prefs, llama `GetRandomQuoteUseCase`, muestra notificación; reintenta hasta 3 veces en error de red |

**Flujo de scheduling**: `SettingsViewModel` → `NotificationScheduler.schedule(prefs)` → `WorkManager.enqueueUniquePeriodicWork(UPDATE)` → `QuoteNotificationWorker.doWork()` → `NotificationHelper.showQuoteNotification()`

**Por qué NO se usa `SCHEDULE_EXACT_ALARM`**: `PeriodicWorkRequest` usa `JobScheduler`/`AlarmManager` internamente gestionado por WorkManager. Para frases motivacionales la ventana de ±30 min es aceptable y evita el diálogo de permiso especial de Android 12+.

**WorkManager y reinicios**: WorkManager registra su propio `BroadcastReceiver` para `BOOT_COMPLETED` internamente. El permiso `RECEIVE_BOOT_COMPLETED` en el manifest es suficiente — no hace falta un `BootReceiver` propio.

## Permissions (AndroidManifest)

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Requerido en API 33+ para mostrar notificaciones |
| `RECEIVE_BOOT_COMPLETED` | WorkManager lo usa internamente para reprogramar tras reinicio |

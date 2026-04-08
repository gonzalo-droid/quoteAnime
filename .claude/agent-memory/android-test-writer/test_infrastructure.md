---
name: Test Infrastructure
description: MainDispatcherRule location, test dependencies added, test directory layout for quoteAnime
type: project
---

## MainDispatcherRule
Location: `app/src/test/java/com/gondroid/quoteanime/util/MainDispatcherRule.kt`
Uses `StandardTestDispatcher` by default. All ViewModel tests use `@get:Rule val mainDispatcherRule = MainDispatcherRule()`.

## Test Dependencies Added (2026-04-07)
Added to `gradle/libs.versions.toml`:
- `mockk = "1.13.12"` → `io.mockk:mockk`
- `coroutinesTest = "1.10.2"` → `kotlinx-coroutines-test` (same version as `coroutines`)
- `turbine = "1.2.0"` → `app.cash.turbine:turbine`

Added to `app/build.gradle.kts` under `testImplementation`:
- `libs.mockk`
- `libs.coroutines.test`
- `libs.turbine`

## Test Directory Layout
```
app/src/test/java/com/gondroid/quoteanime/
├── util/
│   └── MainDispatcherRule.kt
├── domain/
│   └── usecase/
│       ├── ToggleFavoriteUseCaseTest.kt          (4 tests)
│       ├── GetFavoriteQuotesUseCaseTest.kt        (4 tests)
│       ├── GetQuotesByCategoryUseCaseTest.kt      (5 tests)
│       ├── GetRandomQuoteUseCaseTest.kt           (5 tests)
│       └── UpdateUserPreferencesUseCaseTest.kt   (12 tests)
├── data/
│   └── repository/
│       └── QuoteRepositoryImplTest.kt            (10 tests)
└── presentation/
    ├── home/
    │   ├── HomeViewModelTest.kt                  (13 tests)
    │   └── ShareUtilsTest.kt                     (10 tests)
    ├── catalog/
    │   └── CatalogViewModelTest.kt               (15 tests)
    └── settings/
        └── SettingsViewModelTest.kt              (21 tests)
```
Total: 99 unit tests, all passing.

## wrapText visibility
`ShareUtils.kt` had `wrapText` as `private`. Changed to `internal` with `@VisibleForTesting` annotation to enable unit testing.

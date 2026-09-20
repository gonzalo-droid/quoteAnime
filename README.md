# Quote Anime

![Version](https://img.shields.io/badge/version-1.2.2-blue)
![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen)
![License](https://img.shields.io/badge/license-Proprietary-lightgrey)

[Google Play](https://play.google.com/store/apps/details?id=com.gondroid.quoteanime)

Quote Anime shows motivational quotes from the anime you already love, one per screen,
read the way you read a feed: swipe up for the next one. Save the ones that stick, filter
them by series, share any of them as an image, and get one pushed to you on the schedule
you choose.

It also ships **Mi Rutina**, an anime-themed habit tracker — pick habits from themed
templates, mark them done, and watch a GitHub-style heatmap and a streak counter fill in.
A **Premium** subscription lifts the habit limit, removes the ads and unlocks the
exclusive themes.

---

## Screenshots

> Pending. Screens to capture, in flow order: Onboarding · Quote feed · Catalog ·
> Mi Rutina · Settings · Widgets

<!-- TODO: capturar y reemplazar por la tabla de imágenes (ver docs/screenshots/flow.yml) -->

---

## Features

| Feature | Detail |
|---|---|
| **Full-screen quote feed** | `VerticalPager`, one quote per page, a different gradient on each, drawn from the anime you selected |
| **Favorites** | Saved locally in Room, available offline and reachable from the Catalog |
| **Catalog** | Filter by anime series or by favorites, horizontal chip row |
| **Anime selection** | Settings → Anime; drives the Home feed, the notifications and the quote widget |
| **Share as image** | Any quote renders to a bitmap and goes out through the system share sheet |
| **Quote notifications** | Time window plus 1–10 per day, spread end to end across the window; each notification schedules the next one |
| **Quote widget** | Glance widget that adapts its layout to the size you drop it at; tapping it opens the feed on that quote |
| **Mi Rutina (habit tracker)** | Create and edit habits from themed templates (ninja, One Piece, saiyan, Pokémon, Black Clover), 17-week heatmap, current and record streak, month calendar, archive / restore / delete with confirmation, per-habit reminders |
| **Routine widgets** | A summary widget for all active habits, plus a per-habit widget whose habit is chosen in a configuration activity when you drop it on the home screen |
| **Premium subscription** | Unlimited habits, no ads, exclusive themes — real Google Play Billing, with plans and offers read from Play Console at runtime and the entitlement re-synced on every app start |
| **Onboarding** | Four pages on first launch — three quote pages plus picking a first habit |
| **Dark theme** | Always dark, no dynamic color |
| **Localization** | Spanish and English, following the device language |
| **Ads** | AdMob banner plus an interstitial after sharing, both hidden for Premium users |

---

## Tech Stack

- **Language**: Kotlin 2.3.20
- **UI**: Jetpack Compose + Material3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt 2.58
- **Local database**: Room 2.7.1 — schema version 6 (favorites, habits, habit completions,
  with `ForeignKey CASCADE`)
- **Preferences**: DataStore — holds the local Premium entitlement flag, kept in sync with
  Google Play Billing
- **Remote**: Firebase Realtime Database (`/quotes`, `/imagenes`, `/habitTemplates`)
- **Billing**: Google Play Billing `billing-ktx` 9.1.0 — `premium_subscription`, plans and
  offers read dynamically from Play Console
- **Widgets**: Glance 1.1.1 — three widgets (quote, routine summary, single habit)
- **Notifications**: WorkManager + NotificationCompat (quotes and per-habit reminders)
- **Ads**: Google AdMob (banner + interstitial), gated by Premium
- **Images**: Coil 2.7.0
- **Build**: AGP 8.10.0, KSP, Gradle version catalog
- **minSdk** 24 · **targetSdk** 36 · **compileSdk** 36

---

## Architecture

```
com.gondroid.quoteanime/
├── data/
│   ├── local/
│   │   ├── db/                  # Room v6 — dao/ (FavoriteQuote, Habit, HabitCompletion)
│   │   │                        #   + entity/
│   │   └── datastore/           # UserPreferencesDataStore (prefs + local Premium flag)
│   ├── remote/                  # QuoteRemoteDataSource, HabitTemplateRemoteDataSource
│   │                            #   (Firebase RTDB callbackFlow), BillingClientFactory, dto/
│   └── repository/              # Quote, UserPreferences, Habit and Billing implementations
├── domain/
│   ├── model/                   # Quote, Category, Habit, HabitWithProgress, StreakState,
│   │                            #   SubscriptionOffer, UserPreferences, WidgetSize…
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # 32 use case classes across 25 files
├── presentation/
│   ├── splash/ onboarding/ home/ catalog/ settings/   # Core screens
│   ├── routine/                 # Mi Rutina: list, editor, detail, heatmap, calendar, palette
│   ├── subscription/            # PaywallScreen + PaywallViewModel
│   ├── widget/                  # HabitWidgetConfigureActivity (per-habit widget setup)
│   ├── ads/ components/ common/ web/                  # Shared UI, AppLinks, in-app WebView
│   └── navigation/              # AppNavGraph, Screen sealed class
├── widget/                      # Glance: QuoteWidget, RoutineSummaryWidget, HabitWidget
│                                #   (+ their *Receiver and *State)
├── worker/                      # Quote notification, habit reminder, the three widget
│                                #   updaters, purchase acknowledgement
├── notification/                # NotificationHelper + schedulers + NextReminderCalculator
├── analytics/                   # RoutineAnalytics (Firebase Analytics)
├── ui/theme/                    # Colors, typography, theme
└── di/                          # AppModule, DatabaseModule, RepositoryModule, PremiumGate
```

---

## Firebase — Schema

```
/quotes/{index}
  ├── id:     Long
  ├── quote:  String
  ├── author: String
  └── anime:  String

/habitTemplates/{id}          # optional — overrides DefaultHabitTemplates.ALL when present
  ├── title:            String   # string-resource key, e.g. "template_theme_ninja"
  ├── iconKey:          String
  ├── order:            Int
  ├── themeColorIndex:  Int?     # index into HabitPalette.COLORS
  ├── themeKey:         String?  # resolves image + description through HabitThemeImages
  └── isPremiumOnly:    Boolean  # defaults to false when the node is missing
```

> Quote categories are derived dynamically from the distinct values of the `anime` field.
> If `/habitTemplates` is empty or missing, the habit editor falls back to the local
> `DefaultHabitTemplates.ALL` (five themes: ninja, One Piece, saiyan, Pokémon, Black
> Clover — the last two Premium-only).

---

## Navigation Flow

```
App launch
 └── Splash (2s, animated logo)
      ├── First run → Onboarding (3 quote pages + first habit) → Home
      └── Seen      → Home
           ├── Catalog (filter by anime or favorites)
           ├── Settings → Anime (which series the feed and notifications use)
           ├── Settings (notifications, widget, Premium row)
           │    └── Paywall (benefits + real Google Play Billing plans)
           └── Mi Rutina (habit list, Active / Archived tabs)
                ├── Habit editor (create/edit, full-screen bottom sheet)
                ├── Habit detail (large heatmap + calendar, full-screen bottom sheet)
                └── Paywall (tapping a locked themed suggestion)

Quote widget tap    → Home (scrolled to the widget's quote)
Routine widget tap  → Mi Rutina
Habit reminder tap  → Mi Rutina
```

---

## Requirements & Setup

| Tool | Version |
|---|---|
| Android Studio | Narwhal or newer (AGP 8.10.0) |
| JDK | 17 |
| Android SDK | compileSdk 36 |
| Gradle | wrapper included (`./gradlew`) |

### 1. Firebase

`app/src/google-services.json` is currently committed to this repository, so a clean
checkout builds as-is. To point the app at your own Firebase project instead:

1. Create a project in the [Firebase Console](https://console.firebase.google.com).
2. Add an Android app with the package `com.gondroid.quoteanime`.
3. Download `google-services.json` and replace the one in `app/src/`.
4. Enable **Realtime Database** and seed at least the `/quotes` node:

```json
{
  "quotes": [
    { "id": 1, "quote": "...", "author": "...", "anime": "..." }
  ]
}
```

### 2. AdMob

Ad unit IDs are `buildConfigField` entries in `app/build.gradle.kts`, read through
`BuildConfig.AD_UNIT_BANNER` / `BuildConfig.AD_UNIT_INTERSTITIAL`. The AdMob application
ID is a `meta-data` entry in `AndroidManifest.xml`.

The **debug** build type already uses Google's public test units. The **release** build
type carries this project's production IDs — replace them with your own before publishing.
Serving live ads to your own device is invalid traffic and gets AdMob accounts suspended.

### 3. Play Billing

The `premium_subscription` product must exist in Play Console with at least one base plan,
otherwise the paywall renders with an empty plan list.

### Build commands

```bash
./gradlew build                # Full build
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK
./gradlew test                 # Unit tests (336 tests)
./gradlew connectedAndroidTest # Instrumented tests (24, requires a device/emulator)
```

---

## Permissions

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Motivational notifications and habit reminders (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | WorkManager reschedules periodic work after a reboot |

`INTERNET` is not declared in the app manifest; it is merged in from the Firebase and
AdMob libraries.

---

## Privacy Policy

The published privacy policy and terms of service are declared in
`presentation/common/AppLinks.kt` and open inside the app's WebView:

- https://www.animequote.app/privacy-policy
- https://www.animequote.app/terms-and-conditions

---

## Versioning

Current release: **1.2.2** (versionCode 11)

This project follows [Semantic Versioning](https://semver.org/). The full release history
lives in [CHANGELOG.md](CHANGELOG.md).

---

## License

Copyright © 2026 Gondroid. All rights reserved.

This source code is proprietary. It may be viewed for reference, but it may not be
copied, modified, redistributed, or published to any app store without written
permission.

Quotes, characters, and artwork belong to their respective rights holders. The license
above covers this application's source code only, not third-party content.

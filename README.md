# Quote Anime

Aplicación Android de frases motivacionales de anime. Experiencia inmersiva, diseño oscuro minimalista y tipografía elegante.

## Screenshots

> _Agregar capturas de HomeScreen, Onboarding, CatalogScreen, Mi Rutina, Paywall y los 3 widgets_

---

## Descripción

Quote Anime muestra frases de tus series favoritas con una experiencia de lectura inmersiva. Desliza verticalmente entre frases, guarda tus favoritas, recibe notificaciones en tu horario y coloca un widget en tu pantalla de inicio. También incluye **Mi Rutina**, un habit tracker temático (heatmap tipo GitHub, rachas, recordatorios y sugerencias 100% basadas en anime) con un nivel **Premium** que desbloquea hábitos ilimitados, elimina anuncios y da acceso a temas exclusivos.

---

## Características

| Feature | Detalle |
|---|---|
| **Frases full-screen** | `VerticalPager` con gradientes únicos por página |
| **Favoritos** | Guardado local en Room, accesible desde Catalog |
| **Explorar** | Filtro por anime con scroll horizontal |
| **Notificaciones de frases** | Rango horario configurable, frecuencia 1–10×/día |
| **Widget de frases** | Responsive (Small/Medium/Large), se adapta al tamaño físico |
| **Compartir** | Comparte cualquier frase como imagen con un toque |
| **Mi Rutina (habit tracker)** | Crear/editar hábitos con sugerencias temáticas (ninja, One Piece, saiyan, Pokémon, Black Clover), heatmap de 17 semanas, racha actual/récord, calendario mensual, archivar/restaurar/borrar con confirmación, recordatorios por hábito |
| **Widgets de Mi Rutina** | Widget resumen (todos los hábitos activos) + widget individual por hábito (heatmap propio, se elige el hábito al agregarlo) |
| **Premium** | Hábitos ilimitados, sin anuncios, temas exclusivos — suscripción real vía Google Play Billing, planes/ofertas leídos dinámicamente desde Play Console |
| **Onboarding** | 4 páginas (3 de frases + selección de primer hábito), solo en el primer arranque |
| **Splash** | Logo animado + transición suave |
| **Dark theme** | Siempre oscuro, sin dynamic color |
| **AdMob** | Banner + intersticial al compartir (ocultos si sos Premium) |

---

## Stack tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Arquitectura**: Clean Architecture + MVVM
- **DI**: Hilt
- **Base de datos local**: Room v5 (favoritos + hábitos/completions, con `ForeignKey CASCADE`)
- **Preferencias**: DataStore (flag de entitlement Premium, sincronizado con Google Play Billing)
- **Remote**: Firebase Realtime Database (`/quotes`, `/imagenes`, `/habitTemplates`)
- **Pagos**: Google Play Billing (`billing-ktx` 9.1.0) — suscripción `premium_subscription`, planes/ofertas leídos dinámicamente desde Play Console
- **Widgets**: Glance API — 3 widgets (frase, resumen de rutina, hábito individual)
- **Notificaciones**: WorkManager + NotificationCompat (frases + recordatorios por hábito)
- **Publicidad**: Google AdMob (banner + intersticial), gateado por Premium
- **minSdk**: 24 | **targetSdk**: 36

---

## Arquitectura

```
com.gondroid.quoteanime/
├── data/
│   ├── local/
│   │   ├── db/                     # Room v5
│   │   │   ├── dao/                #   FavoriteQuoteDao, HabitDao, HabitCompletionDao
│   │   │   └── entity/              #   FavoriteQuoteEntity, HabitEntity, HabitCompletionEntity
│   │   └── datastore/               # UserPreferencesDataStore (prefs + flag Premium local)
│   ├── remote/                      # QuoteRemoteDataSource, HabitTemplateRemoteDataSource
│   │                                 #   (Firebase RTDB callbackFlow) + dto/
│   └── repository/                  # QuoteRepositoryImpl, UserPreferencesRepositoryImpl,
│                                     #   HabitRepositoryImpl, BillingRepositoryImpl
│                                     #   (wraps BillingClient — conexión, query de
│                                     #   ProductDetails, purchase flow, acknowledge, sync)
├── domain/
│   ├── model/                       # Quote, Category, UserPreferences, WidgetSize,
│   │                                 #   Habit, HabitTemplate, HabitWithProgress, StreakState,
│   │                                 #   SubscriptionOffer, BillingPurchaseResult
│   ├── repository/                  # Interfaces (Quote/UserPreferences/Habit/Billing)
│   └── usecase/                     # Un use case por clase (28): frases, hábitos
│                                     #   (Create/Update/Archive/Unarchive/Delete/Toggle),
│                                     #   rachas, plantillas, onboarding, Premium
│                                     #   (ObservePremiumStatus/SetPremiumStatus) y Billing
│                                     #   (GetSubscriptionOffers/LaunchSubscriptionPurchase/
│                                     #   ObservePurchaseEvents/RestorePurchases)
├── presentation/
│   ├── splash/                      # SplashScreen + SplashViewModel
│   ├── onboarding/                  # OnboardingScreen (4 páginas) + OnboardingViewModel
│   ├── home/                        # HomeScreen + HomeViewModel
│   ├── catalog/                     # CatalogScreen + CatalogViewModel
│   ├── settings/                    # SettingsScreen + SettingsViewModel (incluye fila Premium)
│   ├── routine/                     # "Mi Rutina": RoutineScreen/ViewModel, HabitCard,
│   │                                 #   HabitEditorSheet, HabitIconPicker, HabitDetailScreen
│   │                                 #   (heatmap + calendario), HabitHeatmap/HeatmapGrid,
│   │                                 #   HabitPalette/HabitIcons/HabitThemeImages
│   ├── subscription/                # PaywallScreen + PaywallViewModel
│   ├── widget/                      # HabitWidgetConfigureActivity — elige el hábito de
│   │                                 #   una instancia del widget individual
│   ├── components/                  # QuoteCard, BannerAd
│   └── navigation/                  # AppNavGraph, Screen sealed class
├── worker/                          # QuoteNotificationWorker, HabitReminderWorker,
│                                     #   UpdateQuoteWidgetWorker, UpdateRoutineSummaryWidgetWorker,
│                                     #   UpdateHabitWidgetWorker
├── widget/                          # 3 widgets Glance:
│                                     #   QuoteWidget, RoutineSummaryWidget, HabitWidget
│                                     #   (+ sus *Receiver y *State)
├── notification/                    # NotificationHelper, NotificationScheduler,
│                                     #   HabitReminderScheduler, WidgetScheduler,
│                                     #   RoutineWidgetScheduler, NextReminderCalculator
├── analytics/                       # RoutineAnalytics (Firebase Analytics)
└── di/                              # AppModule, DatabaseModule, RepositoryModule, PremiumGate
```

---

## Firebase — Schema

```
/quotes/{index}
  ├── id:     Long
  ├── quote:  String
  ├── author: String
  └── anime:  String

/habitTemplates/{id}          # opcional — sobreescribe DefaultHabitTemplates.ALL si existe
  ├── title:            String   # clave de string-resource, ej. "template_theme_ninja"
  ├── iconKey:           String
  ├── order:             Int
  ├── themeColorIndex:    Int?     # índice en HabitPalette.COLORS
  ├── themeKey:           String?  # resuelve imagen + descripción vía HabitThemeImages
  └── isPremiumOnly:      Boolean  # default false si el nodo no existe
```

> Las categorías de frases se derivan dinámicamente de los valores únicos del campo `anime`. Si `/habitTemplates` está vacío o no existe, el editor de hábitos cae en el fallback local `DefaultHabitTemplates.ALL` (5 temas: ninja, One Piece, saiyan, Pokémon, Black Clover — los últimos dos exclusivos Premium).

---

## Flujo de navegación

```
App abre
 └── Splash (2s, logo animado)
      ├── Primera vez → Onboarding (3 páginas de frases + selección de primer hábito) → Home
      └── Ya visto    → Home
           ├── Catalog (filtro por anime o favoritos)
           ├── Settings (notificaciones, widget, fila Premium)
           │    └── Paywall (beneficios + planes reales de Google Play Billing)
           └── Mi Rutina (lista de hábitos, tabs Activos/Archivados)
                ├── Habit Editor (crear/editar, bottom sheet fullscreen)
                ├── Habit Detail (heatmap grande + calendario, bottom sheet fullscreen)
                └── Paywall (al tocar una sugerencia temática bloqueada)

Widget de frase tap        → Home (scroll a la quote del widget)
Widgets de Mi Rutina tap   → Mi Rutina
Recordatorio de hábito tap → Mi Rutina
```

---

## Configuración inicial

### 1. Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com)
2. Agrega una app Android con el package `com.gondroid.quoteanime`
3. Descarga `google-services.json` y colócalo en `app/`
4. Habilita **Realtime Database** en modo lectura pública (o con reglas según tu caso)

Estructura de datos mínima en RTDB:
```json
{
  "quotes": [
    { "id": 1, "quote": "...", "author": "...", "anime": "..." }
  ]
}
```

### 2. AdMob

1. Crea una cuenta en [AdMob](https://admob.google.com)
2. Registra la app y crea un bloque de anuncios tipo Banner
3. Reemplaza los IDs en:

| Archivo | Campo | ID de prueba actual |
|---|---|---|
| `AndroidManifest.xml` | `APPLICATION_ID` meta-data | `ca-app-pub-3940256099942544~3347511713` |
| `presentation/components/BannerAd.kt` | `BANNER_AD_UNIT_ID` | `ca-app-pub-3940256099942544/9214589741` |

---

## Comandos de build

```bash
./gradlew build                # Build completo
./gradlew assembleDebug        # APK debug
./gradlew assembleRelease      # APK release
./gradlew test                 # Tests unitarios (229 tests)
./gradlew connectedAndroidTest # Tests instrumentados (requiere dispositivo/emulador)
```

---

## Permisos

| Permiso | Motivo |
|---|---|
| `INTERNET` | Firebase RTDB + AdMob |
| `POST_NOTIFICATIONS` | Notificaciones motivacionales (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | WorkManager reprograma workers tras reinicio |

---

## Política de privacidad

El archivo `privacy-policy.html` en la raíz del proyecto contiene la política de privacidad lista para publicar. Súbela a GitHub Pages, Netlify o cualquier hosting estático y usa esa URL en Google Play Console.

---

## Versiones

Historial completo en [`CHANGELOG.md`](CHANGELOG.md).

| Versión | Descripción |
|---|---|
| Unreleased | Integración real de Google Play Billing (`billing-ktx` 9.1.0) — paywall con planes dinámicos desde Play Console, sincronización de entitlement al iniciar la app |
| 1.2.0 | Widgets de Mi Rutina (resumen + por hábito), suscripción Premium (paywall, hábitos ilimitados, sin anuncios, temas exclusivos, entitlement local pre-billing), rediseño de onboarding |
| 1.1.5 | Tipografía Google Fonts, compartir la app, redes sociales, términos y condiciones |
| 1.0.0 | Lanzamiento inicial |

---

## Licencia

Uso privado. Todos los derechos reservados © 2025 Gondroid.

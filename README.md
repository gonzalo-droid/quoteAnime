# Quote Anime

Aplicación Android de frases motivacionales de anime. Experiencia inmersiva, diseño oscuro minimalista y tipografía elegante.

## Screenshots

> _Agregar capturas de HomeScreen, Onboarding, CatalogScreen y Widget_

---

## Descripción

Quote Anime muestra frases de tus series favoritas con una experiencia de lectura inmersiva. Desliza verticalmente entre frases, guarda tus favoritas, recibe notificaciones en tu horario y coloca un widget en tu pantalla de inicio.

---

## Características

| Feature | Detalle |
|---|---|
| **Frases full-screen** | `VerticalPager` con gradientes únicos por página |
| **Favoritos** | Guardado local en Room, accesible desde Catalog |
| **Explorar** | Filtro por anime con scroll horizontal |
| **Notificaciones** | Rango horario configurable, frecuencia 1–10×/día |
| **Widget** | Responsive (Small/Medium/Large), se adapta al tamaño físico |
| **Compartir** | Comparte cualquier frase con un toque |
| **Onboarding** | 3 pantallas con imágenes, solo en el primer arranque |
| **Splash** | Logo animado + transición suave |
| **Dark theme** | Siempre oscuro, sin dynamic color |
| **AdMob** | Banner en HomeScreen |

---

## Stack tecnológico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Arquitectura**: Clean Architecture + MVVM
- **DI**: Hilt
- **Base de datos local**: Room (favoritos)
- **Preferencias**: DataStore
- **Remote**: Firebase Realtime Database
- **Widget**: Glance API
- **Notificaciones**: WorkManager + NotificationCompat
- **Publicidad**: Google AdMob
- **minSdk**: 24 | **targetSdk**: 36

---

## Arquitectura

```
com.gondroid.quoteanime/
├── data/
│   ├── local/
│   │   ├── db/              # Room: FavoriteQuoteEntity, FavoriteQuoteDao, AppDatabase v2
│   │   └── datastore/       # UserPreferencesDataStore
│   ├── remote/              # QuoteRemoteDataSource (Firebase RTDB callbackFlow)
│   └── repository/          # QuoteRepositoryImpl, UserPreferencesRepositoryImpl
├── domain/
│   ├── model/               # Quote, Category, UserPreferences, WidgetSize
│   ├── repository/          # Interfaces
│   └── usecase/             # Un use case por clase
├── presentation/
│   ├── splash/              # SplashScreen + SplashViewModel
│   ├── onboarding/          # OnboardingScreen + OnboardingViewModel
│   ├── home/                # HomeScreen + HomeViewModel
│   ├── catalog/             # CatalogScreen + CatalogViewModel
│   ├── settings/            # SettingsScreen + SettingsViewModel
│   ├── components/          # QuoteCard, BannerAd
│   └── navigation/          # AppNavGraph, Screen sealed class
├── worker/                  # QuoteNotificationWorker, UpdateQuoteWidgetWorker
├── widget/                  # QuoteWidget (Glance), QuoteWidgetReceiver
├── notification/            # NotificationHelper, NotificationScheduler, WidgetScheduler
└── di/                      # AppModule, DatabaseModule, RepositoryModule
```

---

## Firebase — Schema

```
/quotes/{index}
  ├── id:     Long
  ├── quote:  String
  ├── author: String
  └── anime:  String
```

> Las categorías se derivan dinámicamente de los valores únicos del campo `anime`.

---

## Flujo de navegación

```
App abre
 └── Splash (2s, logo animado)
      ├── Primera vez → Onboarding (3 páginas) → Home
      └── Ya visto    → Home
           └── Catalog (filtro por anime o favoritos)
           └── Settings (notificaciones, widget)

Widget tap → Home (scroll a la quote del widget)
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
./gradlew test                 # Tests unitarios
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

| Versión | Descripción |
|---|---|
| 1.0.0 | Lanzamiento inicial |

---

## Licencia

Uso privado. Todos los derechos reservados © 2025 Gondroid.

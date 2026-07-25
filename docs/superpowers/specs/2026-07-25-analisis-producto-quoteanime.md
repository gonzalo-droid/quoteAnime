# Análisis de producto — Quote Anime (Android)

Fecha: 2026-07-25
Versión analizada: 1.1.5 (versionCode 7)

## 1. Estado actual del código

Aplicación de un solo módulo, Clean Architecture con MVVM en presentación. Unas 5.800 líneas de Kotlin.

| Capa | Contenido |
|---|---|
| `data/local/db` | Room v4, una sola entidad: `FavoriteQuoteEntity` |
| `data/local/datastore` | `UserPreferencesDataStore` — categorías, notificaciones, widget, onboarding |
| `data/remote` | `QuoteRemoteDataSource` sobre **Firebase Realtime Database** |
| `domain` | 5 modelos, 2 interfaces de repositorio, 11 use cases |
| `presentation` | splash, onboarding, home, catalog, settings, components, ads |
| `widget` | `QuoteWidget` con Glance, 3 breakpoints de tamaño |
| `worker` | `QuoteNotificationWorker`, `UpdateQuoteWidgetWorker` |

### Correcciones a la documentación

`CLAUDE.md` describe **Firestore** como fuente remota, pero el proyecto declara `firebase-database-ktx` y no incluye `firebase-firestore`. La fuente real es **Realtime Database**. También describe un esquema `/categories` + `/quotes` con `categoryId`, mientras que el modelo `Quote` actual usa `categories: List<String>` y `animeSlug`, con las imágenes en `/imagenes/{slug}`. Conviene actualizar `CLAUDE.md` antes de empezar trabajo nuevo.

### Fortalezas

- Separación de capas limpia y consistente; los use cases son de una sola responsabilidad.
- Estado reactivo bien resuelto: `combine()` para mezclar favoritos de Room con datos remotos, `flatMapLatest` para el cambio de pestaña en el catálogo.
- Cobertura de tests existente (99 tests unitarios según el changelog) más tests de UI en Compose.
- Compartir como imagen generada es una ventaja competitiva real y ya está construida.
- Widget Glance con múltiples instancias y deep link a la frase.

### Huecos y deuda

| Hueco | Impacto |
|---|---|
| No existe noción de tiempo ni de historial: Room solo guarda favoritos | Cualquier feature de constancia, racha o progreso parte de cero |
| Sin cuenta ni sincronización: reinstalar o cambiar de móvil borra los favoritos | Reseñas negativas; bloqueante para un habit tracker |
| Sin core library desugaring | `java.time` no disponible con minSdk 24 |
| Solo español (`values/`, 29 strings) | Limita el mercado; el nicho de frases de anime es global |
| `SettingsScreen.kt` con 1.039 líneas | El archivo más grande del proyecto; conviene trocearlo al tocarlo |
| Monetización solo por anuncios | Sin premium, sin Play Billing, sin palanca de ingresos por valor |
| Navegación sin barra inferior | Añadir una sección obliga a refactorizar la navegación |
| Sin analítica de producto (solo Crashlytics) | No hay forma de saber qué features retienen |
| Reglas de Realtime Database sin escrituras autenticadas | Bloqueante si se guardan datos de usuario |

## 2. Mercado

### Frases de anime (competencia directa)

Apps como OtaQuotes, *+8500 Anime Quotes*, *500.000+ Anime Quotes* y *Animequ*. Compiten por volumen de frases y por búsqueda por anime o personaje. Varias funcionan offline y sin anuncios.

Quejas recurrentes en sus reseñas:
- Fallos y cierres inesperados tras pocas frases.
- No poder navegar por anime o por personaje, solo búsqueda por texto.
- Poca curación: animes populares con muy pocas frases.

**Ninguna de ellas ofrece rutinas ni constancia.** El eje de competencia es cantidad de contenido, no hábito.

### Motivación con rachas (competencia adyacente)

*Motivation — Daily Quotes* y *Motivation — 365 Daily Quotes* ya gamifican la apertura diaria con contador de rachas y añaden widgets y contenido en vídeo. Demuestran que la mecánica de racha funciona sobre contenido motivacional.

### Habit trackers gamificados (referencia, no competencia directa)

Habitica, MainQuest, Finch, Streaks, Daylio. Establecen el estándar de calidad de la categoría: offline, sincronización, estadísticas y recordatorios. Finch destaca por gamificación amable que nunca culpabiliza, que es el tono correcto para este producto.

### Monetización

Datos de referencia del sector de suscripciones (RevenueCat, informe 2026): los paywalls duros convierten cerca del 10,7 % de prueba a pago a día 35 frente al 2,1 % del freemium, y generan alrededor de 8 veces más ingreso por instalación a día 60. La retención anual entre ambos modelos es casi idéntica (27 % frente a 28 %).

Lectura para este producto: el paywall duro no encaja mientras los ingresos dependan del volumen de instalaciones para anuncios. La vía razonable es mantener gratis el bucle principal y reservar el premium para límites y contenido, una vez medido que la feature retiene.

## 3. Oportunidad

El cruce **frases de anime + constancia diaria** está vacío. Los competidores directos son catálogos de contenido sin mecánica de retorno; las apps de rachas no tienen la identidad anime. Quote Anime ya tiene el contenido, las imágenes, el widget y las notificaciones: le falta la razón para volver cada día.

## 4. Mejoras propuestas, por prioridad

| # | Mejora | Esfuerzo | Impacto |
|---|---|---|---|
| 1 | Sección «Mi rutina»: hábitos, rachas y heatmap | Alto | Retención, diferenciación |
| 2 | Navegación por anime y por personaje en el catálogo | Medio | Es la queja número uno de la competencia |
| 3 | Traducción a inglés | Medio | Multiplica el mercado alcanzable |
| 4 | Sincronización anónima de favoritos y rutina | Medio | Elimina la pérdida de datos al cambiar de móvil |
| 5 | Wallpapers descargables desde el generador de imágenes existente | Bajo | Base natural para un premium futuro |
| 6 | Analítica de producto con Firebase Analytics | Bajo | Sin esto, las decisiones son a ciegas |
| 7 | Trocear `SettingsScreen.kt` | Bajo | Mantenibilidad |
| 8 | Actualizar `CLAUDE.md` (Realtime Database, esquema real) | Bajo | Evita errores en trabajo futuro |

La número 1 se especifica en `2026-07-25-mi-rutina-habit-tracker-design.md`. Las números 3 y 6 se incluyen dentro de esa misma fase por ser dependencias naturales del trabajo.

## Fuentes

- [The Best Motivation Apps of 2026 — BestApp.com](https://www.bestapp.com/best-motivation-apps/)
- [Motivation — 365 Daily Quotes, Google Play](https://play.google.com/store/apps/details?id=com.krikasoft.motivationalquotes&hl=en_US)
- [OtaQuotes: Anime Quotes, Google Play](https://play.google.com/store/apps/details?id=com.techroot.otaquotes)
- [+8500 Anime Quotes, Google Play](https://play.google.com/store/apps/details?id=linc.anime.quotes&hl=en)
- [Habitica alternatives: 8 best gamified and minimalist apps for 2026 — Headway](https://makeheadway.com/blog/habitica-alternatives/)
- [Best Habitica Alternatives 2026 — MainQuest](https://www.mainquest.net/habitica-alternatives)
- [The State of Subscription Apps in 10 minutes: benchmarks for 2026 — RevenueCat](https://www.revenuecat.com/blog/growth/subscription-app-trends-benchmarks-2026/)
- [7 Best Streak Tracker Apps in 2026 — Habi](https://habi.app/insights/best-streak-tracker-apps/)

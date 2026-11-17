# Changelog

All notable changes to Quote Anime are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [1.2.0] - 2026-11-17

Primera versión en producción desde 1.1.5 (versionCode 7 → 8). Reúne lo que en el repo se fue
numerando 1.2.0 a 1.3.1 (builds 8 a 15), que nunca llegó a la tienda: se describe aquí lo que
recibe quien actualiza desde 1.1.5. La novedad grande es **Mi Rutina**.

### Added
- **Mi Rutina (seguimiento de hábitos)**: crear, editar, archivar, restaurar y borrar hábitos
  (borrar pide confirmación y elimina su historial). Racha actual y récord, heatmap estilo
  GitHub y calendario mensual, pestañas Activos/Archivados, 126 íconos en 13 categorías con
  buscador (encuentra "musica" sin tilde), 14 colores, descripción y fechas en el formato de tu
  idioma. Sin límite de hábitos. Pestaña propia en la barra inferior
- **Sugerencias temáticas de anime**: Camino ninja, Buscar el One Piece, Sé un saiyan, Sé un
  maestro Pokémon y Sé el Rey Mago, con portada, color e ícono. Las del servidor reemplazan a
  las incluidas en la app cuando llegan, así que sin conexión también hay sugerencias
- **Recordatorios por hábito**: notificación en los días y la hora que elijas, con botón "Hecho"
  que marca el día sin abrir la app
- **Widgets de Mi Rutina**: uno de resumen con todos los hábitos activos y otro por hábito (con
  su heatmap), que se elige al agregarlo. Se actualizan al instante al marcar o editar un
  hábito
- **Primer hábito en la bienvenida**: el onboarding suma una página para elegirlo, con el mismo
  estilo que las de frases
- **Frases en inglés**: con la app en inglés, las frases se muestran traducidas (`quoteEn` en
  Firebase) en Inicio, el Catálogo, los favoritos, el detalle, la imagen para compartir, las
  notificaciones, los recordatorios y el widget. Una frase todavía sin traducir se muestra en
  español. Los favoritos guardados toman la traducción de Firebase al haber conexión
- **Idioma en Ajustes** (Android 13 o superior): English, Español o el del sistema solo para la
  app; cambia la app, las notificaciones y los widgets al momento
- **Consentimiento de anuncios**: donde la ley lo exige (Europa, Reino Unido y algunos estados
  de EE. UU.), la app pregunta antes de mostrar anuncios, con el formulario de Google. En
  Ajustes aparece "Opciones de privacidad de anuncios" para cambiar la elección

### Changed
- **La app ahora se llama AnimeQuotes**, en inglés y en español: bajo el ícono, en la pantalla
  de inicio, en la tarjeta para compartir, en la invitación y en el tutorial del widget
- **Textos en inglés de verdad**: con el teléfono en inglés ya no quedan textos en español en
  el tutorial del widget, Ajustes, las emociones del Catálogo, los mensajes de Inicio ni el
  widget. En español, toda la app trata de "tú"
- Guardar queda siempre a la vista al crear o editar un hábito
- **Notificaciones de frases a la hora justa**: llegan exactamente las veces por día elegidas,
  repartidas de punta a punta en la ventana horaria, también en ventanas que cruzan la
  medianoche (22:00–02:00)

### Fixed
- **La app ya no se reinicia al tocar un widget o una notificación** con la app abierta: sigue
  donde estaba y abre lo que pediste. Volver desde Recientes no repite el último atajo, y un
  widget o recordatorio con la app cerrada respeta el onboarding si no lo terminaste
- **La app ya no se traba al abrirla con el teléfono exigido**: los anuncios se preparan en
  segundo plano
- Las tarjetas y el detalle de las frases vuelven a usar su tipografía (Playfair Display y
  Lora): los archivos de esas fuentes estaban dañados
- Tocar el widget de frases mientras carga o tras un error abre la app sin perder la pantalla
- TalkBack anuncia qué ícono y color están elegidos en el editor de hábitos

### Internal
- **Premium construido pero apagado**: Google Play Billing (`billing-ktx` 9.1.0, producto
  `premium_subscription`), paywall y restauración en cada arranque están integrados, pero
  `PremiumGate.PAYMENTS_ENABLED = false` los oculta hasta que la suscripción exista en Play
  Console. Por eso no hay límite de hábitos ni temas bloqueados; los anuncios siguen
- La sección "Animes" de Ajustes (elegir de qué animes ver frases) está construida y corregida
  (lista animes, no emociones) pero oculta
- Room v6 (`MIGRATION_4_5` conserva los favoritos, `MIGRATION_5_6` suma campos al hábito).
  `quoteEn` no se guarda en Room: se toma de Firebase, sin migración
- UMP 3.1.0 para el consentimiento; firma de release leída de `~/.gradle/gradle.properties`
  (`QUOTEANIME_*`), fuera del repo
- Sin errores de lint: fuera la API restringida `NavController.currentBackStack` y los textos
  leídos con `LocalContext` en Compose
- Firebase Analytics para Mi Rutina (`RoutineAnalytics`)

---

## [1.1.5] - 2026-04-23

### Added
- **Tipografía Google Fonts**: fuentes Lora (frase) y Playfair Display (autor) descargadas async; fallback a Georgia / Didot del sistema. Definidas en `ui/theme/FontFamilies.kt` con certificados GMS en `res/values/font_certs.xml`
- **Compartir la app**: nuevo ítem en SettingsScreen con `Intent.ACTION_SEND` y mensaje personalizable (`R.string.share_app_message`)
- **Sección "Síguenos"**: 3 ítems en SettingsScreen (Facebook, Instagram, TikTok) con íconos vectoriales brand-colored y `openUrl()` compartido; URLs pendientes de actualizar con handles reales
- **Términos y condiciones**: nuevo ítem en `VersionSection` de SettingsScreen; comparte URL con la política de privacidad (`https://gondroid.dev/privacy-policy`)
- **Previews `@Preview`**: cobertura completa añadida en `QuoteCard`, `QuoteDetailContent`, `HomeScreen`, `SettingsScreen`, `OnboardingScreen` y `WidgetTutorialScreen`

### Changed
- Comillas tipográficas decorativas (`"` / `"`) eliminadas de `QuoteCard`, `QuoteDetailContent` y `QuoteWidget` (Small, Medium y Large); también eliminado el `Text` decorativo de apertura en el widget Large
- Fuentes del texto de frases y autores actualizadas de Georgia/Didot del sistema a las variantes Google Fonts (con mismos fallbacks)

### Technical
- Dependencia nueva: `androidx.compose.ui:ui-text-google-fonts` (gestionada por BOM de Compose)
- Iconos vectoriales nuevos en `res/drawable/`: `ic_facebook.xml`, `ic_instagram.xml`, `ic_tiktok.xml`
- Strings nuevos en `res/values/strings.xml`: `share_app_message`, `terms_and_conditions`, `politics_privacy`

---

## [1.1.4] - 2026-04-10

### Added
- **Publicidad al compartir**: se muestra un anuncio intersticial cada 3 veces que el usuario comparte una frase, aplicado en HomeScreen y CatalogScreen con contador acumulativo entre ambas pantallas

### Changed
- Banner publicitario de HomeScreen desactivado temporalmente, reemplazado por el intersticial en el flujo de compartir

### Technical
- `ShareInterstitialManager` singleton Hilt (`presentation/ads/`) — gestiona precarga, contador y presentación; se recarga automáticamente tras cada aparición
- Inyectado en `HomeViewModel` y `CatalogViewModel`

---

## [1.1.3] - 2026-04-09

### Added
- **Imágenes por anime**: cada frase muestra una imagen de fondo del anime correspondiente, seleccionada aleatoriamente y estable durante la sesión
- **Catálogo rediseñado**: hub con tres vistas — Selector (Favoritos, Todas, 10 emociones), Lista y Detalle full-screen con imagen de fondo
- **Firma en compartir**: la imagen generada incluye logo + nombre de la app en esquina inferior derecha

### Changed
- Overlay de oscurecimiento más pronunciado en pantallas y en imagen de compartir (45 % → 72 % → 92 % opacidad)
- Padding horizontal del texto en compartir aumentado para presentación más centrada
- Más espacio entre la quote y el autor en la imagen de compartir (divisor + gaps explícitos)
- `QuoteDetailContent` extraído como componente reutilizable entre HomeScreen y CatalogScreen

### Technical
- `imageUrl` reemplazado por `animeSlug` en modelo `Quote`, DTO y entidad Room
- Nuevo nodo Firebase `/imagenes/{slug}` con array de URLs; resolución al vuelo en `QuoteRepositoryImpl` con cache por slug
- Room DB versión 4
- `CatalogViewModelTest` reescrito para arquitectura hub (Selector → Lista → Detalle)

---

## [1.1.2] - 2026-04-08

### Added
- **Compartir como imagen**: frases compartidas como imagen con diseño personalizado (gradiente oscuro, tipografía serif, marca de agua)
- **Actualizaciones en la app**: flujo flexible de actualización desde Play Store sin salir de la app
- **Tutorial de widget**: pantalla con guía paso a paso accesible desde Ajustes
- **Valoración**: acceso directo a Google Play desde Ajustes
- **Versión visible**: número de versión en pantalla de Ajustes

### Changed
- Compartir frase genera imagen en lugar de texto plano

### Technical
- Firebase Crashlytics integrado
- 99 tests unitarios (use cases, ViewModels, repositorio)
- FileProvider configurado para compartir archivos de imagen

---

## [1.1.1] - 2026-03-28

### Added
- Actualización de frecuencia de notificaciones push (1–10 veces/día)
- Redimensionado del widget con breakpoints responsive (Small / Medium / Large)

### Changed
- `NotificationFrequency` enum reemplazado por `Int` (timesPerDay) en toda la pila

---

## [1.1.0] - 2026-03-20

### Added
- **Splash screen**: logo animado con fade + scale usando `core-splashscreen`
- **Onboarding**: 3 pantallas en primer arranque con imágenes full-screen y dots animados
- **AdMob**: banner publicitario en HomeScreen
- Soporte para múltiples instancias del widget

### Changed
- Fondo oscuro en toda la app para eliminar flash blanco al iniciar

---

## [1.0.0] - 2026-02-01

### Added
- **HomeScreen**: pager vertical full-screen con frases y gradientes por página
- **CatalogScreen**: exploración por categorías con filtro de favoritos
- **Favoritos**: guardado y acceso offline con Room
- **Widget**: frase aleatoria en pantalla de inicio con Glance API
- **Notificaciones**: frases diarias programadas con WorkManager
- **Ajustes**: categorías, notificaciones, widget
- Tema oscuro con paleta personalizada (AccentPurple, HeartRed, TextPrimary)
- Deep link desde widget a la frase correspondiente en la app

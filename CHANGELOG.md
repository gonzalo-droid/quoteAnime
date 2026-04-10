# Changelog

All notable changes to Quote Anime are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

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

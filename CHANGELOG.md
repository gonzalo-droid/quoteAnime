# Changelog

All notable changes to Quote Anime are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.1.2] - 2026-04-08

### Added
- **Compartir como imagen**: las frases ahora se pueden compartir como imagen con diseño personalizado (gradiente oscuro, tipografía serif, nombre del anime y marca de agua)
- **Actualizaciones en la app**: cuando hay una nueva versión disponible en Play Store, se muestra un flujo flexible de actualización sin salir de la app
- **Tutorial de widget**: nueva pantalla accesible desde Ajustes con guía paso a paso para agregar el widget a la pantalla de inicio
- **Sección de valoración**: acceso directo para valorar la app en Google Play desde la pantalla de Ajustes
- **Sección de versión**: número de versión visible en la pantalla de Ajustes

### Changed
- Compartir frase ahora genera una imagen en lugar de texto plano

### Technical
- Integración de Firebase Crashlytics para monitoreo de errores en producción
- Suite de 99 tests unitarios cubriendo use cases, ViewModels y repositorio
- FileProvider configurado para compartir archivos de imagen entre apps

---

## [1.1.1] - 2026-03-28

### Added
- Actualización de frecuencia de notificaciones push (slider 1–10 veces/día)
- Redimensionado del widget con breakpoints responsive (Small / Medium / Large)

### Changed
- `NotificationFrequency` enum reemplazado por `Int` (timesPerDay) en toda la pila

---

## [1.1.0] - 2026-03-20

### Added
- **Splash screen**: logo animado con fade + scale usando `core-splashscreen` API
- **Onboarding**: 3 pantallas en primer arranque con imágenes full-screen y dots animados
- **AdMob**: banner publicitario en HomeScreen
- Soporte para múltiples instancias del widget

### Changed
- Fondo oscuro en toda la app para eliminar flash blanco al iniciar

---

## [1.0.0] - 2026-02-01

### Added
- **HomeScreen**: pager vertical full-screen con frases, 5 gradientes rotativos por página
- **CatalogScreen**: exploración por categorías con filtro de favoritos
- **Favoritos**: guardado y acceso offline con Room
- **Widget**: frase aleatoria en pantalla de inicio con Glance API, tap abre la app
- **Notificaciones**: frases diarias programadas con WorkManager, rango horario configurable
- **Ajustes**: categorías, notificaciones, widget
- Tema oscuro exclusivo con paleta personalizada (AccentPurple, HeartRed, TextPrimary)
- Deep link desde widget a la frase correspondiente en la app

# Changelog

All notable changes to Quote Anime are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- **Elige tus animes en Ajustes**: una nueva sección "Animes" te deja marcar de qué animes
  quieres frases. El feed de Inicio muestra solo esos animes (igual que en iOS), y las
  notificaciones y el widget de frases también los respetan. "Todos los animes" (o no marcar
  ninguno) vuelve a mostrar todos. El cambio se guarda al instante y el feed se actualiza al
  volver a Inicio

### Fixed
- **La sección "Animes" de Ajustes mostraba emociones**: en lugar de nombres de anime
  listaba "motivación", "reflexión" y demás, y el feed, las notificaciones y el widget
  filtraban por emoción. Ahora ves los animes (ordenados alfabéticamente, como en iOS) y lo que
  eliges filtra por anime. Si habías marcado emociones, al abrir la app se descartan: si no
  queda ningún anime marcado vuelves a ver todos, nunca un feed vacío. Los filtros por emoción
  del Catálogo siguen igual
- **Ajustes sin conexión**: la pantalla de Ajustes ya no se queda cargando para siempre si
  abres la app sin red; solo la lista de animes espera a que vuelva la conexión
- **Frase del widget en Inicio**: después de abrir una frase desde el widget, marcar otra como
  favorita ya no te devuelve a la frase del widget
- **Textos en inglés de verdad**: con el teléfono en inglés, varios textos seguían en español
  porque estaban escritos en el código: los pasos del tutorial del widget, la sección Widget
  de Ajustes, las emociones del Catálogo, los mensajes de Inicio sin frases o con error, el
  lema de la pantalla de inicio, el botón de favorito de las tarjetas y los estados de carga y
  error del widget de frases. Ahora se muestran en tu idioma
- **La app ya no se reinicia al tocar un widget o una notificación**: con la app abierta,
  tocar el widget de frases, los de Mi Rutina, un recordatorio o una notificación de frase
  reiniciaba la app desde la pantalla de inicio. Ahora la app sigue donde estaba y abre lo que
  pediste; la notificación de frase ya no borra la pantalla en la que estabas
- **Abrir desde Recientes no repite el último atajo**: volver a la app desde la lista de apps
  recientes ya no te lleva otra vez a Mi Rutina o a la frase del último widget que tocaste
- **Frecuencia de notificaciones de frases**: el usuario recibía otra cantidad que la
  elegida — el intervalo era `24 / frecuencia` con división entera y un trabajo periódico de
  24 h descartaba los disparos fuera de la ventana (con 8–22, elegir 5 entregaba 4). Ahora
  `QuoteNotificationSlotCalculator` reparte exactamente N horarios de punta a punta en la
  ventana y cada notificación agenda la siguiente
- Las ventanas horarias que cruzan la medianoche (ej. 22:00–02:00) no enviaban ninguna
  notificación: el chequeo de ventana asumía inicio < fin
- **Widgets de Mi Rutina tras "Hecho"**: marcar un hábito desde el botón "Hecho" del
  recordatorio ahora actualiza al instante el widget de resumen y el de cada hábito; antes
  seguían mostrando el día sin marcar hasta el siguiente refresco diario
- **Onboarding respetado al abrir desde una notificación o un widget**: tocar un recordatorio
  o el widget de frases con la app cerrada ya no se salta el onboarding si no lo terminaste.
  Primero se muestra el onboarding y, al terminarlo, se abre lo que pediste. Si ya lo habías
  completado, vas directo (sin la animación de inicio), y "atrás" desde Mi Rutina vuelve a
  Inicio en lugar de cerrar la app
- **Sugerencias de hábitos sin conexión**: el editor de hábitos y el onboarding muestran las
  sugerencias incluidas en la app al instante, y se reemplazan por las del servidor cuando
  llegan. Antes, sin red, quedaban sin ninguna sugerencia
- **Sin claves internas en pantalla**: una sugerencia nueva publicada en el servidor que esta
  versión todavía no sabe nombrar ya no aparece como `template_…`; se omite hasta que una
  actualización la incluya. Los títulos escritos como texto normal se muestran tal cual
- **Widget de frases sin frase**: tocarlo mientras carga o tras un error abre la app
  normalmente, en lugar de navegar con una frase vacía y perder la pantalla en la que estabas
- **Buscador de íconos sin tildes**: escribir "musica" ahora encuentra "Tocar música", y un
  espacio al final (el que suele dejar el autocorrector) ya no vacía los resultados
- **TalkBack en el editor de hábitos**: al recorrer los íconos y los colores ahora se anuncia
  cuál está elegido ("Seleccionado, Color 3"), en lugar de leer solo "botón"
- **Íconos con el mismo nombre en inglés**: "Darte un gusto" y "Darte un capricho" se
  llamaban igual en inglés ("Treat yourself"), así que TalkBack los anunciaba igual; el
  segundo ahora es "Pamper yourself"

### Changed
- **Textos en español con un mismo registro**: los mensajes de Mi Rutina, el onboarding, la
  pantalla Premium, la hoja de cancelación y el error de carga de páginas que hablaban de
  "vos" ("Probá", "Ya sos premium", "Cancelá") ahora te tratan de "tú", como el resto de la
  app y la versión de iOS

## [1.2.1]

### Added — Google Play Billing real
- **Integración de `billing-ktx` 9.1.0**: reemplaza el flag mock de Premium por una
  suscripción real. `BillingRepositoryImpl` consulta `ProductDetails` para el producto
  `premium_subscription` (todos los base plans/ofertas configurados en Play Console se leen
  dinámicamente, nada hardcodeado del lado del cliente), lanza el flujo de compra, acknowledgea
  la compra y actualiza el mismo flag de DataStore que ya leía cada feature gateada — ningún
  call-site existente (`ObservePremiumStatusUseCase` y sus 6 consumidores) cambió
- **Paywall con planes reales**: la pantalla ya no tiene un botón "Suscribirme" fijo — muestra
  la lista de planes disponibles (precio, trial si existe) leída de Play, con selección y
  feedback de compra pendiente/cancelada/error vía snackbar
- **Sincronización al iniciar la app**: `RestorePurchasesUseCase` corre en cada arranque
  (`QuoteAnimeApplication.onCreate`) para detectar una suscripción cancelada o expirada fuera
  de la app — no hay backend propio, así que la verificación es 100% del lado del cliente
  (`Purchase.isAcknowledged` + `PurchaseState.PURCHASED`), una limitación consciente
- Botón "Quitar premium (solo pruebas)" se mantiene, ahora exclusivamente para QA

## [1.2.0]

Everything below shipped after 1.1.5 but hasn't been tagged with a new `versionName` yet —
grouped by feature area since it's too large to read commit-by-commit. The single biggest
addition is **Mi Rutina**, a full habit-tracker feature that didn't exist in any prior release.

### Added — Mi Rutina (habit tracker)
- **Hábitos**: crear, editar, archivar/restaurar y borrar (permanente, con cascada a los
  cumplimientos) — todas las acciones destructivas piden confirmación primero
- **Sugerencias 100% temáticas**: 5 plantillas basadas en anime (Camino ninja, Buscar el One
  Piece, Sé un saiyan, Sé un maestro Pokémon, Sé el Rey Mago), cada una con imagen de portada,
  color e ícono; la primera sugerencia disponible se auto-selecciona al crear un hábito nuevo
- **Racha y heatmap**: racha actual + récord (`CalculateStreakUseCase`), heatmap estilo GitHub
  de 17 semanas en la card compacta y de 26 semanas con etiquetas de día/mes en el detalle
- **Calendario mensual**: vista alternativa de un mes completo con los mismos datos que el heatmap
- **Filtro Activos/Archivados**: tabs en la lista, con estados vacíos que explican qué hace
  archivar y qué pasa con el historial
- **Recordatorios por hábito**: notificación programable con acción "Hecho" desde la propia
  notificación, día(s) de la semana configurables
- **Selector de íconos**: pantalla completa con 126 íconos en 13 categorías y buscador
- **Editor**: 14 colores, campo de descripción, fecha con formato dd/mm/aaaa, tarjeta de
  recordatorio, vista previa temática con imagen de portada
- **Onboarding**: 4ta página para elegir el primer hábito, mismo estilo visual (fondo, dots,
  botón) que las 3 páginas de frases — antes era un formulario plano sin continuidad visual
- **Widget "Mi rutina" (resumen)**: todos los hábitos activos con racha y check de "hoy" en un
  solo widget; toca para abrir la pantalla de Rutina
- **Widget por hábito**: heatmap individual de 9 semanas; al agregarlo, una pantalla de
  configuración nueva (la primera de este tipo en la app) deja elegir qué hábito seguir en esa
  instancia — se pueden agregar varios, uno por hábito
- Ambos widgets se refrescan al instante al marcar/crear/editar/archivar/borrar un hábito desde
  la app, además de un refresco diario de respaldo
- Medición de eventos con Firebase Analytics (`RoutineAnalytics`): apertura de tab, hábito
  creado/completado/archivado, racha rota/hito alcanzado
- Español e inglés en toda la feature

### Added — Premium (suscripción)
- **Paywall**: pantalla con 3 beneficios — hábitos ilimitados, sin anuncios, temas exclusivos —
  y botón de "Suscribirme"
- **Entitlement local**: flag booleano en DataStore (`ObservePremiumStatusUseCase` /
  `SetPremiumStatusUseCase`) — pre-billing a propósito: cuando se integre Google Play Billing
  real, solo cambia el call-site del botón "Suscribirme", ningún lector del flag
- **Hábitos ilimitados**: el límite gratuito (3 hábitos activos) se levanta reactivamente al
  activar premium, en toda la app
- **Sin anuncios**: el banner del detalle de frase (Catálogo) se oculta si sos premium
- **Temas exclusivos**: las sugerencias de Pokémon y Black Clover quedan bloqueadas para
  usuarios gratuitos (con candado en el chip); tocarlas abre el paywall en vez de seleccionarlas
- Puntos de entrada al paywall: fila en Ajustes, mensaje de "límite alcanzado" en Mi Rutina, y
  tocar una sugerencia bloqueada (editor u onboarding)

### Fixed
- Menú de "Editar/Archivar" (⋮ en la card de hábito) se desplazaba lejos del ícono en vez de
  anclarse debajo — el `DropdownMenu` no estaba envuelto junto a su `IconButton` en un `Box`
  propio, que es el patrón que Compose necesita para anclarlo correctamente
- Fondo de los grupos con borde redondeado del editor (fechas, recordatorio) mostraba un
  parche de color distinto en las esquinas — `ListItem` usa `colorScheme.surface` por defecto,
  que no coincide con el fondo del sheet; se corrigió con `clip()` + `ListItem` transparente
- Los chips de filtro "Activos/Archivados" cambiaban de posición al pasar de una pestaña vacía
  a una con hábitos (padding inconsistente entre el estado vacío y la lista poblada)
- Condición de carrera en el auto-select del onboarding: el estado premium se leía de un flow
  separado que podía no haber emitido todavía, así que un usuario premium a veces veía la
  primera sugerencia bloqueada como si fuera gratis
- Botón de cerrar del detalle de hábito quedaba fuera de pantalla por falta de `weight(1f)`
  en la fila de ícono+título
- Migración destructiva de Room acotada a esquemas pre-v4 únicamente (no borraba datos en
  actualizaciones posteriores)
- Fugas de accesibilidad: heatmap y selectores de ícono/color sin semántica para lectores de
  pantalla

### Changed
- Botón "Añadir hábito" movido del FAB flotante a una acción en el `TopAppBar` de Mi Rutina
- Todos los modales de Mi Rutina unificados: título a la izquierda, botón de cerrar a la
  derecha; detalle de hábito ahora es un bottom sheet fullscreen deslizable en vez de una
  pantalla con push/pop
- Copys de estado vacío reescritos (Activos: más motivacional; Archivados: explica qué implica
  archivar antes de decir "no tenés ninguno")
- Navegación por barra inferior con tab de Mi Rutina; recordatorio de hábito y widgets abren
  la app directo en esa pantalla

### Technical
- Room v5: `HabitEntity`, `HabitCompletionEntity` (con `ForeignKey CASCADE`), `HabitDao`,
  `HabitCompletionDao`
- 24 use cases en `domain/usecase/` (uno por clase), incluyendo los nuevos de Premium
- `HabitTemplate.isPremiumOnly` + campo homónimo en el DTO remoto (`/habitTemplates`)
- +130 tests unitarios nuevos (viewmodels, use cases, DAO) — 229 tests unitarios en total
- Compose `@Preview` agregado a cada vista del paquete `routine/` (múltiples estados: vacío,
  poblado, error, archivado, límite alcanzado)

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

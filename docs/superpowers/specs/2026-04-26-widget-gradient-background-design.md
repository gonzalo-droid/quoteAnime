# Widget — Gradiente de fondo mejorado

**Fecha**: 2026-04-26
**Alcance**: Solo `app/src/main/res/drawable/widget_bg.xml`

## Objetivo

Reemplazar el gradiente plano actual del widget por uno atmosférico y profundo que simule el look del HomeScreen (imagen + overlay oscuro) sin descargar archivos.

## Contexto

- `QuoteDetailContent` (Home) usa: imagen de fondo + `Brush.verticalGradient(negro 45% → 72% → 92%)`.
- `QuoteWidget` usa `.background(ImageProvider(R.drawable.widget_bg))` — un `<shape>` simple con gradiente a 135°.
- Glance no soporta Coil ni carga de URLs; mostrar imágenes dinámicas requiere descargarlas en el worker, lo que puede repetir descargas de la misma imagen. Por eso se elige la opción de gradiente enriquecido.

## Solución: `layer-list` de tres capas

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Capa 1: base sólida negra profunda -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#0C0816" />
            <corners android:radius="20dp" />
        </shape>
    </item>

    <!-- Capa 2: gradiente vertical principal (simula overlay del Home) -->
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:startColor="#2A1052"
                android:centerColor="#150930"
                android:endColor="#0A0614"
                android:angle="270" />
            <corners android:radius="20dp" />
        </shape>
    </item>

    <!-- Capa 3: acento diagonal sutil (simula luz de foto real) -->
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:startColor="#60192030"
                android:endColor="#00000000"
                android:angle="45" />
            <corners android:radius="20dp" />
        </shape>
    </item>

</layer-list>
```

## Por qué funciona

| Capa | Propósito |
|------|-----------|
| Base sólida `#0C0816` | Garantiza que no haya transparencia en ningún tamaño de widget |
| Gradiente vertical 270° | Imita la dirección del overlay del Home (oscurece más abajo donde va el texto) |
| Acento diagonal 45° | Añade sensación de profundidad / luz lateral, como las fotos reales de anime |

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `app/src/main/res/drawable/widget_bg.xml` | Reemplazar contenido por `layer-list` de 3 capas |

`QuoteWidget.kt` no cambia — ya referencia `R.drawable.widget_bg`.

## Lo que NO cambia

- Lógica del worker
- Estado del widget
- Tamaños responsivos (Small / Medium / Large)
- Colores del texto

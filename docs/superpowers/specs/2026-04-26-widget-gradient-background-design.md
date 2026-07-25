# Widget — Gradiente de fondo mejorado

**Fecha**: 2026-04-26
**Alcance**: `widget_bg.xml` + `widget_preview.xml`

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

## Solución 2: `widget_preview.xml` — Preview fiel al widget real

El preview actual usa `?android:attr/colorBackground` (fondo del sistema) y `TextView` sin estilo — no se parece al widget real. Se reescribe para usar el mismo fondo y colores.

Cambios clave:
- `android:background="@drawable/widget_bg"` — mismo gradiente que el widget real
- `android:padding="16dp"` sobre un `FrameLayout` raíz con `clipToOutline` para respetar las esquinas redondeadas del drawable
- `TextView` frase: `textColor="#F0EAFF"`, `textStyle="italic"`, `textSize="14sp"`
- `TextView` autor: `textColor="#9B8DB3"`, `textSize="11sp"`, prefijo `— `
- `TextView` anime: `textColor="#A78BFA"`, `textSize="9sp"`, `textAllCaps="true"`, `letterSpacing="0.12"`
- Texto de ejemplo en español con quote, autor y anime representativos

## Archivos afectados

| Archivo | Cambio |
|---------|--------|
| `app/src/main/res/drawable/widget_bg.xml` | Reemplazar por `layer-list` de 3 capas |
| `app/src/main/res/layout/widget_preview.xml` | Reescribir para que use `widget_bg` y los mismos colores/tipografía del widget real |

`QuoteWidget.kt` y `quote_widget_info.xml` no cambian.

## Lo que NO cambia

- Lógica del worker
- Estado del widget
- Tamaños responsivos (Small / Medium / Large)
- Colores del texto en `QuoteWidget.kt`

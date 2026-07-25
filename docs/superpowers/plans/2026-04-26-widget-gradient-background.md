# Widget Gradient Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar el fondo plano del widget por un gradiente oscuro atmosférico de 3 capas, y actualizar el preview del selector de widgets para que sea fiel al widget real.

**Architecture:** Solo cambios en recursos XML — ningún archivo Kotlin se toca. `widget_bg.xml` pasa de `<shape>` simple a `<layer-list>` de 3 capas. `widget_preview.xml` se reescribe como `FrameLayout` con el mismo drawable de fondo y los colores exactos del widget real.

**Tech Stack:** Android XML drawables (`layer-list`, `shape`, `gradient`), Android View XML layout.

---

## File Map

| Archivo | Acción | Responsabilidad |
|---------|--------|-----------------|
| `app/src/main/res/drawable/widget_bg.xml` | Modificar | Fondo del widget — 3 capas: base sólida + gradiente vertical + acento diagonal |
| `app/src/main/res/layout/widget_preview.xml` | Modificar | Preview estático del selector de widgets, fiel al widget real |

---

### Task 1: Actualizar `widget_bg.xml` con gradiente de 3 capas

**Files:**
- Modify: `app/src/main/res/drawable/widget_bg.xml`

- [ ] **Step 1: Reemplazar el contenido de `widget_bg.xml`**

Abrir `app/src/main/res/drawable/widget_bg.xml` y reemplazar todo el contenido con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Capa 1: base sólida — evita transparencia en cualquier tamaño de widget -->
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#0C0816" />
            <corners android:radius="20dp" />
        </shape>
    </item>

    <!-- Capa 2: gradiente vertical principal — top oscuro-púrpura → bottom negro puro -->
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

    <!-- Capa 3: acento diagonal sutil — simula luz lateral de una foto real -->
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

- [ ] **Step 2: Verificar que el widget real sigue compilando**

```bash
./gradlew assembleDebug
```

Resultado esperado: `BUILD SUCCESSFUL` sin errores.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/widget_bg.xml
git commit -m "feat: update widget background to atmospheric 3-layer gradient"
```

---

### Task 2: Reescribir `widget_preview.xml` para que sea fiel al widget real

**Files:**
- Modify: `app/src/main/res/layout/widget_preview.xml`

El preview actual usa `?android:attr/colorBackground` (fondo claro del sistema) y `TextView` sin estilo. Lo reemplazamos con el mismo `widget_bg` y los colores exactos del `QuoteWidget`:
- Texto frase: `#F0EAFF` italic
- Texto autor: `#9B8DB3` con prefijo `— `
- Texto anime: `#A78BFA` uppercase con letter spacing

- [ ] **Step 1: Reemplazar el contenido de `widget_preview.xml`**

Abrir `app/src/main/res/layout/widget_preview.xml` y reemplazar todo el contenido con:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Preview estático del selector de widgets (API 31+). Debe verse igual al widget real. -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/widget_bg"
    android:clipToOutline="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="center_vertical"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Soy el capitán de mi propio destino."
            android:textColor="#F0EAFF"
            android:textSize="14sp"
            android:textStyle="italic"
            android:lineSpacingMultiplier="1.3" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="— Monkey D. Luffy"
            android:textColor="#9B8DB3"
            android:textSize="11sp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="ONE PIECE"
            android:textColor="#A78BFA"
            android:textSize="9sp"
            android:letterSpacing="0.12" />

    </LinearLayout>

</FrameLayout>
```

- [ ] **Step 2: Compilar y verificar**

```bash
./gradlew assembleDebug
```

Resultado esperado: `BUILD SUCCESSFUL` sin errores.

- [ ] **Step 3: Verificar visualmente en Android Studio**

Abrir `widget_preview.xml` en Android Studio → pestaña **Design**. Debe mostrar:
- Fondo oscuro con degradado púrpura (no fondo blanco/gris del sistema)
- Texto de la frase en color claro (#F0EAFF) en italic
- Autor en gris-púrpura (#9B8DB3) con `— ` delante
- "ONE PIECE" en lila/púrpura (#A78BFA) en mayúsculas
- Esquinas redondeadas visibles

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/widget_preview.xml
git commit -m "feat: update widget preview to match real widget appearance"
```

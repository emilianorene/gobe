# RetroTV — Emulador all-in-one para Android TV (ONN Plus 4K)

> Proyecto personal (uso propio, no comercial). Frontend Libretro en Kotlin + Jetpack Compose for TV.
> Objetivo: una sola app que corra Arcade/MAME, NES, SNES y N64, con UI navegable por D-pad y acceso real a las ROMs.

---

## 1. Contexto y motivación

Las apps existentes fallan por razones concretas que este proyecto resuelve de raíz:

| App | Problema observado | Causa real |
|-----|--------------------|-----------|
| RetroArch | UI inutilizable en TV | Menú XMB no es Android TV nativo |
| Lemuroid | "No puede acceder a los juegos" | Usa SAF (Storage Access Framework); el selector de carpetas es inservible con control remoto en TV |
| Daijishō | No arranca / no accede | Mismo muro de Scoped Storage |
| Snes9x EX+ | Funciona pero es 1 solo sistema | No es all-in-one |

**Decisión de diseño central:** como es de uso personal, usamos `MANAGE_EXTERNAL_STORAGE` (acceso total a archivos) en lugar de SAF. Esto elimina el problema #1 de todas las apps anteriores.

---

## 2. Hardware objetivo

- **Dispositivo:** ONN Android TV Plus 4K (Amlogic, 2024).
- **Input primario:** control remoto (D-pad). Gamepad Bluetooth como secundario.
- **Implicancia de performance (no negociable):**
  - NES, SNES, arcade clásico (CPS1/CPS2, Neo Geo): nativo, sin problemas.
  - MAME: romsets de los 80 / early 90 OK; CPS3 y late-MAME pueden sufrir.
  - **N64: "lo que se pueda".** Mupen64Plus-Next; los first-party (Mario 64, MK64, OOT) andan razonable, otros con glitches/tirones. Es límite de hardware, no de software.

---

## 3. Arquitectura: tres capas

El proyecto **solo construye la capa 3**. Las otras dos se reutilizan.

### Capa 1 — Cores de emulación (NO se escriben)
Binarios `.so` precompilados de Libretro, empaquetados dentro del APK (estrategia de Lemuroid):
- `fbneo` o `mame2003_plus` → Arcade / Neo Geo / CPS
- `nestopia` → NES
- `snes9x` → SNES
- `mupen64plus_next` → N64

### Capa 2 — Bridge nativo (NO se reescribe)
**LibretroDroid** (C++): carga el core `.so`, maneja OpenGL ES, audio (Oboe) e input. Es la base que ya funciona bien en Lemuroid. Se integra como dependencia/módulo, no se modifica.

### Capa 3 — La app (TODO el esfuerzo va acá)
Kotlin + Jetpack Compose for TV:
- Acceso a archivos con permiso total
- Escaneo e indexado de la biblioteca de ROMs
- Detección de sistema por extensión / carpeta
- Grilla navegable por D-pad con foco correcto
- Lanzamiento del juego pasando ROM + core a la capa 2
- Persistencia de save states y "continuar jugando"

---

## 4. Decisiones técnicas clave

### 4.1 Acceso a archivos
- Declarar `MANAGE_EXTERNAL_STORAGE` en el manifest.
- Pantalla de onboarding que envía al usuario a Settings para activar "All files access" una vez.
- Lectura directa de rutas (`java.io.File`) — sin SAF.
- **Fase 1:** solo almacenamiento interno (las ROMs ya copiadas por FTP).
- **Fase posterior:** volúmenes USB removibles vía `StorageManager` (es trabajo extra, no bloquea el MVP).

### 4.2 Detección de sistema
Mapeo por extensión y/o carpeta:
```
.nes              → NES (nestopia)
.smc .sfc         → SNES (snes9x)
.z64 .n64 .v64    → N64 (mupen64plus_next)
.zip (arcade)     → FBNeo / MAME (requiere validar contra romset)
```
Para arcade, el `.zip` no se puede identificar por extensión: se valida el nombre del archivo contra la lista de romsets del core elegido.

### 4.3 UI / Focus (el verdadero trabajo de "TV-friendly")
Lo que hace que una app de TV se sienta bien NO es lo estético, es el **focus management**:
- Usar **`androidx.tv.material3`** y `Modifier.focusable()`.
- Grilla con `TvLazyVerticalGrid`.
- Garantizar que el D-pad salte limpio entre tiles, sin foco perdido ni saltos raros.
- Estado de foco visible (escala/borde en el tile enfocado).
- Botón OK = lanzar; botón Back = volver sin cerrar la app.

### 4.4 Carátulas / metadata
- MVP: nombre del archivo limpio (sin región/tags).
- Posterior: scraping de boxart (libretro-thumbnails) cacheado localmente.

### 4.5 Multijugador (hasta 4 jugadores)
El soporte de 4 jugadores tiene **dos mitades**; solo una es trabajo de software:

1. **A nivel core (resuelto):** SNES (multitap), N64 (4 puertos nativos) y MAME/Neo Geo (muchos títulos 4P) exponen múltiples puertos vía la Libretro API. No requiere escribir emulación.
2. **A nivel app (lo que se construye):** detectar cada `InputDevice`, asignarlo a un puerto P1–P4, y enrutar sus eventos al puerto correcto del core. Más la UI de gestión (ver Fase 3).

**Límite de hardware (documentar, no se resuelve por código):** el ONN Plus 4K usa Bluetooth (sin múltiples puertos USB). El BT de gama baja suele aguantar 2–3 gamepads estables; el 4º puede no parear o agregar lag. **Solución recomendada al usuario:** hub USB-OTG + receptores 2.4GHz (8BitDo/Xbox), más confiable que 4 controles Bluetooth. La app debe enrutar 4 inputs correctamente aunque el cuello de botella sea el transporte físico.

### 4.6 Settings (nivel intermedio, opciones por-core dinámicas)

Hay **tres niveles de configuración** que se resuelven en jerarquía (el más específico gana):

```
GLOBAL (app)  →  POR SISTEMA/CORE  →  POR JUEGO (override opcional)
```

**Nivel 1 — Globales (de la app):**
- Carpetas de ROMs (agregar/quitar rutas).
- Tema e idioma de la interfaz.
- Permiso de almacenamiento (atajo a la pantalla del sistema).
- Asignación de controles a puertos (link a la pantalla de la Fase 3).

**Nivel 2 — Por sistema / core (lo distintivo):**
Las opciones NO se escriben a mano. Cada core Libretro expone sus propias variables vía la API (`RETRO_ENVIRONMENT_GET_VARIABLE` / `core_options`). La app las **lee del core y renderiza la UI dinámicamente**: cada variable trae su clave, etiqueta, valor por defecto y lista de valores posibles. Así, agregar un core nuevo trae sus opciones gratis, sin tocar código de UI.
- Ejemplos típicos: región (NTSC/PAL), modelo de consola, filtros internos, precisión de emulación.
- Más opciones de video **básicas** propias de la app (no del core): aspect ratio, escalado entero (integer scale), filtro lineal on/off. Shaders avanzados quedan fuera del alcance intermedio.

**Nivel 3 — Override por juego (opcional):**
Desde el detalle de un juego se puede guardar un setting que solo aplica a ese título (ej: un juego que necesita otra región). Se persiste en Room como override y pisa al del sistema.

**Remapeo de botones (su propio sub-árbol):**
- Separado por sistema (el mapa de N64 con stick analógico ≠ el de NES).
- Separado por puerto (P1–P4 pueden tener mapeos distintos).
- Modo "presioná el botón" para capturar el binding en vivo.
- Default sensato por sistema + opción de resetear.

**Mockup — Settings principal (Ajustes globales):**
```
┌──────────────────────────────────────────────────────────────┐
│  AJUSTES                                                       │
│                                                                │
│   📂 Carpetas de ROMs                              3 carpetas ▸│
│   🎮 Controles y jugadores                          3 activos ▸│
│   🎨 Interfaz (tema / idioma)                        Oscuro  ▸│
│   📺 Video (aspect ratio, escalado)                          ▸│
│   🧩 Opciones por sistema                                    ▸│
│   🔑 Permiso de almacenamiento                      Concedido ▸│
│   💾 BIOS                                          2 faltantes ▸│
│   ℹ Acerca de                                                ▸│
└──────────────────────────────────────────────────────────────┘
```

**Mockup — Opciones por sistema (leídas del core dinámicamente):**
```
┌──────────────────────────────────────────────────────────────┐
│  OPCIONES · Nintendo 64 (mupen64plus-next)                    │
│                                                                │
│   Resolución interna         [ 320×240 (nativa) ▸ ]           │
│   Plugin de video            [ GLideN64        ▸ ]            │
│   Región                     [ Auto            ▸ ]            │
│   Framebuffer emulation      [ Activado        ▸ ]            │
│                                                                │
│   ⓘ Estas opciones las expone el core. Cambian según          │
│     el sistema seleccionado.                                  │
│                                                                │
│   [ Restaurar valores por defecto ]                           │
└──────────────────────────────────────────────────────────────┘
```

**Mockup — Remapeo de botones (por sistema / por puerto):**
```
┌──────────────────────────────────────────────────────────────┐
│  CONTROLES · SNES · Puerto 1                                  │
│                                                                │
│   Botón A      →  [ A (gamepad) ]        [ Cambiar ]          │
│   Botón B      →  [ B (gamepad) ]        [ Cambiar ]          │
│   Botón X      →  [ X (gamepad) ]        [ Cambiar ]          │
│   Botón Y      →  [ Y (gamepad) ]        [ Cambiar ]          │
│   L / R        →  [ L1 / R1     ]        [ Cambiar ]          │
│   Start/Select →  [ ☰ / ⧉       ]        [ Cambiar ]          │
│                                                                │
│   [ Sistema: SNES ▸ ]  [ Puerto: 1 ▸ ]  [ Resetear ]          │
│   Al tocar "Cambiar": "Presioná el botón a asignar…"          │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. Plan de fases (ejecutable por Claude Code)

### Fase 0 — Esqueleto
- [ ] Proyecto Android (Kotlin, Compose, min SDK acorde a Android TV del ONN).
- [ ] `MANAGE_EXTERNAL_STORAGE` en manifest + pantalla de onboarding de permiso.
- [ ] Confirmar que se listan archivos de una carpeta interna conocida.

### Fase 1 — Biblioteca
- [ ] Escáner de carpeta(s) configurable(s).
- [ ] Detección de sistema por extensión.
- [ ] Modelo de datos (Room): Game(id, path, system, displayName, lastPlayed).
- [ ] Grilla Compose for TV navegable por D-pad, agrupada por sistema.

**Mockup — Pantalla principal (Home / biblioteca):**
```
┌──────────────────────────────────────────────────────────────┐
│  RetroTV                                    🎮 3/4   ⚙ Ajustes │
│                                                                │
│  ▶ CONTINUAR JUGANDO                                           │
│   ┌────────┐ ┌────────┐ ┌────────┐                            │
│   │ Mario  │ │ Zelda  │ │ Metal  │                            │
│   │  64    │ │  OOT   │ │ Slug   │                            │
│   └────────┘ └────────┘ └────────┘                            │
│                                                                │
│  ▶ NES                                                         │
│   ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                │
│   │╔══════╗│ │ Contra │ │ Mega   │ │ Punch  │                │
│   │║ Mario║│ │        │ │ Man 2  │ │  Out   │   ← tile con    │
│   │╚══════╝│ │        │ │        │ │        │     FOCO (borde │
│   └────────┘ └────────┘ └────────┘ └────────┘     + escala)   │
│                                                                │
│  ▶ SNES        ▶ ARCADE/MAME        ▶ N64                      │
└──────────────────────────────────────────────────────────────┘
   D-pad: mover foco   •   OK: abrir juego   •   Back: salir
```
El indicador `🎮 3/4` arriba a la derecha muestra controles conectados (clave para multijugador).

### Fase 2 — Emulación (integración de capas 1 y 2)
- [ ] Integrar LibretroDroid como módulo/dependencia.
- [ ] Empaquetar cores `.so` (empezar con nestopia + snes9x).
- [ ] Pantalla de juego: pasar ROM + core, render GL, audio.
- [ ] Mapeo de control remoto / gamepad a input del core.
- [ ] **Enrutado multi-puerto:** asignar cada gamepad detectado a un puerto del core (P1–P4) vía `InputDevice` IDs. El core ya expone 4 puertos por la Libretro API; la app solo enruta.

**Mockup — Detalle de juego (al apretar OK sobre un tile):**
```
┌──────────────────────────────────────────────────────────────┐
│                                                                │
│   ┌──────────┐    SUPER MARIO 64                               │
│   │          │    Nintendo 64 · Mupen64Plus-Next               │
│   │ boxart   │    Última vez: ayer · Save state: sí            │
│   │          │                                                 │
│   └──────────┘    [ ▶ JUGAR ]  ← foco inicial                  │
│                   [ ⟳ Reanudar desde save ]                    │
│                   [ 👥 Jugadores: 1 ▸ ]                        │
│                   [ ⚙ Opciones del core ]                      │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

**Mockup — Overlay de pausa en juego (botón Back durante partida):**
```
        ┌───────────────────────────────┐
        │         ⏸  PAUSA              │
        │                               │
        │   ▶ Reanudar                  │
        │   💾 Guardar estado           │
        │   📂 Cargar estado            │
        │   👥 Jugadores (3 conectados) │
        │   ⚙ Opciones                  │
        │   ✕ Salir al menú             │
        └───────────────────────────────┘
```

### Fase 3 — Más sistemas
- [ ] Agregar FBNeo/MAME (+ validación de romset para arcade).
- [ ] Agregar Mupen64Plus-Next (N64) y testear performance real en el ONN.
- [ ] **Pantalla de gestión de controles** (asignar gamepads a P1–P4, reordenar, testear botones).
- [ ] **Settings nivel 1 (globales):** carpetas de ROMs, tema/idioma, atajo a permiso.
- [ ] **Settings nivel 2 (por-core dinámico):** leer variables del core vía la API y renderizar la UI automáticamente.
- [ ] **Remapeo de botones** por sistema y por puerto, con modo "presioná el botón".

**Mockup — Gestión de jugadores (Ajustes › Controles):**
```
┌──────────────────────────────────────────────────────────────┐
│  CONTROLES                                                     │
│                                                                │
│   PUERTO 1   🎮 8BitDo Pro 2        [ Probar ] [ Reasignar ]  │
│   PUERTO 2   🎮 Xbox Wireless       [ Probar ] [ Reasignar ]  │
│   PUERTO 3   🎮 Genérico 2.4G       [ Probar ] [ Reasignar ]  │
│   PUERTO 4   ⚪ (vacío — conectá un control)                  │
│                                                                │
│   ⓘ El ONN tiene Bluetooth limitado. Para 4 jugadores         │
│     estables, usá un hub USB-OTG con receptores 2.4GHz.       │
└──────────────────────────────────────────────────────────────┘
```

### Fase 4 — Calidad de vida
- [ ] Save states + auto-save al salir.
- [ ] Sección "Continuar jugando" (por `lastPlayed`).
- [ ] **Settings nivel 3:** override de opciones por juego (pisa al del sistema, se guarda en Room).
- [ ] Scraping de carátulas.
- [ ] Soporte de USB removible.

---

## 6. Limitaciones y riesgos (explícitos)

1. **Licencia:** LibretroDroid y la mayoría de cores son **GPL-3.0**. Para uso personal no hay problema; si alguna vez se distribuye, el código debe liberarse como GPL.
2. **N64 nunca será perfecto** en este hardware. Aceptar compatibilidad parcial.
3. **USB en Android 11+** requiere manejo especial de volúmenes; por eso queda fuera del MVP.
4. **Riesgo de heredar el bug de Lemuroid:** se evita explícitamente al NO usar SAF.
5. **BIOS:** algunos cores (no los del MVP) requieren archivos BIOS provistos por el usuario.
6. **4 jugadores:** soportado a nivel app y core, pero el Bluetooth del ONN limita cuántos controles físicos se conectan de forma estable. El 4º jugador puede requerir hub USB-OTG + receptores 2.4GHz. La app no puede "arreglar" el transporte BT por software.
7. **El proyecto no incluye ni distribuye ROMs ni BIOS.** El usuario aporta sus archivos legalmente poseídos.

---

## 7. Referencias

- Lemuroid (arquitectura modular Kotlin, cores empaquetados): https://github.com/Swordfish90/Lemuroid
- LibretroDroid (bridge C++): https://github.com/Swordfish90/LibretroDroid
- Emulair (modelo de UI en Jetpack Compose for TV): https://github.com/EmulairEmulator/Emulair-Android
- Cores precompilados: https://buildbot.libretro.com/
- Carátulas: https://github.com/libretro-thumbnails

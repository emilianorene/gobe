# Gobe — Internationalization (i18n) + English Codebase Design

> Date: 2026-06-30
> Scope: Make the whole UI multilingual (English + Spanish) and ensure all code/comments are English.
> Builds on: Fase 0+1 (TV shell + library) and Fase 2 (SNES emulation), both merged to `main`.

## 1. Purpose

Today the UI text is hardcoded in Spanish across the screens, while the code/comments are
mostly English. This sub-project:

1. Moves **all user-facing strings** into Android string resources, with **English as the
   default** (`values/`) and **Spanish** as a translation (`values-es/`).
2. Adds **language selection**: the UI follows the device language automatically, with an
   **in-app override** (System / Español / English) in a small Settings screen, persisted.
3. Ensures **all code and comments are in English** (UI text lives only in resources).

Starting languages: **English + Spanish**. Adding a language later is just another
`values-<lang>/strings.xml`.

## 2. Decision captured during brainstorming

- **Language selection = automatic (system) + in-app override.** Default follows the ONN's
  language; a Settings selector (System / Español / English) overrides it and is remembered.

## 3. In scope

- Extract every hardcoded UI string (~35, inventory in §6) to `values/strings.xml` (English)
  + `values-es/strings.xml` (Spanish). Use placeholders for dynamic values; decorative
  glyphs (▶ ⚙ ⏸ 💾 ⟳ ✕ 📂 📁) stay in Kotlin, only translatable text is a resource.
- `LocaleManager`: persists a language preference (`system` | `es` | `en`) in
  SharedPreferences and resolves it to a `Locale`.
- Per-app locale applied via `attachBaseContext()` wrapping in each Activity (works on
  minSdk 30 without AppCompat, since the app is pure Compose/`ComponentActivity`).
- A small **SettingsScreen** reached from the Home "⚙ Ajustes/Settings" entry, with:
  - **Language** selector (System / Español / English) → persists + recreates to apply.
  - **ROM folders** → opens the existing folders screen.
- All code comments verified/converted to English.

## 4. Out of scope (deferred)

- Theme (light/dark) selection and other Fase 3 settings.
- Languages beyond English/Spanish.
- Localizing game titles (those come from ROM filenames) or `System.displayName`
  (already English and treated as proper nouns: NES, SNES, N64, Arcade).
- RTL layouts (English/Spanish are LTR).

## 5. Architecture

### 5.1 LocaleManager (pure-ish, unit-testable resolution)

```
com.gobe.tv.i18n
└─ LocaleManager
   - const PREF "app_language" in SharedPreferences("gobe.settings")
   - enum AppLanguage { SYSTEM, SPANISH, ENGLISH } with a stable storage tag ("system"/"es"/"en")
   - getLanguage(context): AppLanguage          (read pref, default SYSTEM)
   - setLanguage(context, AppLanguage)          (write pref)
   - resolveLocale(lang): Locale?               (SYSTEM -> null = use system; ES -> Locale("es"); EN -> Locale("en"))  [pure, unit-tested]
   - wrap(context): Context                      (for SYSTEM, return the ORIGINAL context unchanged so the true device locale is preserved; otherwise createConfigurationContext with the resolved Locale)
```

`resolveLocale` (the tag↔Locale mapping) is pure and unit-tested. `wrap` does the Android
`createConfigurationContext` work and is exercised on-device.

### 5.2 Applying the locale

- `MainActivity` and `EmulatorActivity` override:
  ```kotlin
  override fun attachBaseContext(newBase: Context) {
      super.attachBaseContext(LocaleManager.wrap(newBase))
  }
  ```
- Changing the language in Settings calls `LocaleManager.setLanguage(...)` then
  `activity.recreate()` so the new locale takes effect immediately.
- Because `attachBaseContext` reads SharedPreferences synchronously, the preference store is
  SharedPreferences (not DataStore).

### 5.3 Settings UI

- New `ui/settings/SettingsScreen.kt`. Home's "⚙ Settings" navigates here (new `Route.Settings`).
  **Note:** today the Home settings button (`HomeScreen.kt`) is wired straight to
  `onOpenFolders` → `Route.Folders`; this work **re-points** it to `Route.Settings`, and
  Settings then offers a "ROM folders" row that navigates to `Route.Folders`. The existing
  `onOpenFolders`/Folders wiring in `GobeNavHost` is repurposed, not just added to.
- Rows (D-pad navigable, first focused, BackHandler → Home):
  - **Language / Idioma:** three selectable chips/buttons (System / Español / English);
    selecting persists + recreates the activity.
  - **ROM folders / Carpetas de ROMs:** navigates to the existing folders screen
    (`Route.Folders`), which keeps its current behavior.
- The folders screen stays as-is (its strings get localized like everything else).

### 5.4 String resources

- `values/strings.xml` — English (default).
- `values-es/strings.xml` — Spanish (the current on-screen text).
- Keys grouped by screen, e.g. `home_continue_playing`, `home_settings`, `home_no_games`,
  `detail_play`, `detail_resume_save`, `detail_back`, `pause_title`, `pause_resume`,
  `pause_save`, `pause_load`, `pause_exit`, `perm_title`, `perm_body`, `perm_grant`,
  `folders_title`, `folders_add`, `folders_active`, `folders_inactive`, `folders_remove`,
  `browser_use_folder`, `browser_up`, `emu_core_unavailable` (`%s`), `emu_rom_missing`,
  `emu_state_saved`, `emu_state_save_failed`, `emu_no_state`, `emu_state_load_failed`,
  `settings_title`, `settings_language`, `settings_folders`, `lang_system`, `lang_es`,
  `lang_en`, etc. Composables use `stringResource(R.string.key)`; the Activity uses
  `getString(...)` for toasts.

## 6. String inventory (to extract)

- **HomeScreen:** "⚙ Ajustes", "Escaneando…", "Continuar jugando", "No se encontraron
  juegos.", "Agrega una carpeta de ROMs para empezar.", "Configurar carpetas".
- **DetailScreen:** "Cargando…", "▶ Jugar", "⟳ Reanudar desde save", "▶ Jugar
  (próximamente)", "Volver", "Save state: sí", size "%s KB".
- **FoldersScreen:** "Carpetas de ROMs", "+ Agregar carpeta", "Activa", "Inactiva", "Quitar".
- **FolderBrowserScreen:** "Usar esta carpeta", ".. Subir".
- **PermissionScreen:** "Permiso de almacenamiento", the body sentence, "Conceder acceso".
- **PauseOverlay:** "⏸ Pausa", "Reanudar", "Guardar estado", "Cargar estado", "Cargar estado
  (sin guardado)", "Salir al menú".
- **EmulatorActivity (toasts):** "Core no disponible para %s", "ROM no encontrada", "Estado
  guardado", "No se pudo guardar el estado", "No hay estado guardado", "No se pudo cargar el
  estado".

(English becomes the default value; the Spanish above moves to `values-es`.)

## 7. Testing

- **Unit (JVM):** `LocaleManager.resolveLocale` mapping (`system`→null, `es`→es, `en`→en)
  and the AppLanguage storage-tag round-trip.
- **On-device:** Settings → switch to English: all screens (Home, detail, folders, browser,
  permission, pause overlay, toasts) render in English; switch to Español: all in Spanish;
  set to System: follows the ONN language; the choice persists across app restart.
  Note: to meaningfully verify the **System** option, the tester must actually change the
  ONN's system language (otherwise System looks identical to whichever matches the device
  default) — confirm System tracks the device by toggling the device language once.

## 8. Risks / notes

- `attachBaseContext` must read the pref synchronously → SharedPreferences (chosen).
- `recreate()` on language change is the simplest reliable apply; acceptable on TV.
- Compose `stringResource` re-reads from the (wrapped) context, so a recreate refreshes all
  text. The pause overlay lives in `EmulatorActivity`, which also wraps its base context.
- Keep `System.displayName` as proper nouns (not localized).

## 9. Defaults (change on request)

- Package `com.gobe.tv.i18n`; SharedPreferences file `gobe.settings`, key `app_language`;
  default `SYSTEM`; English = default resources, Spanish = `values-es`.

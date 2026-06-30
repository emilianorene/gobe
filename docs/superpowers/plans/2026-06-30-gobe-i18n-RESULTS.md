# Gobe i18n (English + Spanish) — Results

> Date: 2026-06-30
> Branch: `feat/i18n`
> Plan: [2026-06-30-gobe-i18n.md](2026-06-30-gobe-i18n.md)
> Spec: [../specs/2026-06-30-gobe-i18n-design.md](../specs/2026-06-30-gobe-i18n-design.md)

## Outcome: ✅ The UI is multilingual (English default + Spanish), code/comments are English.

The whole UI is driven by string resources. A Settings screen offers a language picker
(System / Español / English) that persists and applies immediately. Verified on the ONN:
the Home flips between English ("Continue playing" / "Settings") and Spanish ("Continuar
jugando" / "Ajustes").

## What shipped

- **String resources:** `values/strings.xml` (English, default) + `values-es/strings.xml`
  (Spanish) — all ~35 UI strings across Home, Detail, Folders, FolderBrowser, Permission,
  PauseOverlay, and EmulatorActivity toasts.
- **`LocaleManager`** (`com.gobe.tv.i18n`): SharedPreferences pref (`system`/`es`/`en`),
  `resolveLocale` (unit-tested), and `wrap()` returning a `createConfigurationContext`-wrapped
  context (original context for SYSTEM).
- **Per-app locale** applied via `attachBaseContext()` in both `MainActivity` and
  `EmulatorActivity` (works on minSdk 30 without AppCompat).
- **Settings screen** (`ui/settings/SettingsScreen.kt`): language picker (● marks the current)
  + a ROM-folders row. Home "⚙ Settings" → Settings → Folders (re-pointed routing).
- **All code comments converted to English.**
- **Bonus fix:** the top-bar Settings button is now reachable via D-pad UP from the first row
  (it was unreachable once games were present — the empty-state button had been the only path).

## Tests

- **Unit (JVM):** `LocaleManagerTest` (3) + the existing suite → all green (35 total).
- **On-device:** Settings → Español flips the whole UI to Spanish; default (System) follows
  the ONN language (English here), confirmed by the English Home/detail before switching.
  The choice is stored in SharedPreferences and survives relaunch.

## Deviations / notes

- The device's system language is English, so the default (SYSTEM) renders English; switching
  to Español proves the override + resources. To verify SYSTEM tracks a Spanish device, change
  the ONN system language (not done here).
- `recreate()` on language change resets the in-app navigation to Home — acceptable.
- A system picture-in-picture (a TV app) overlapped some screenshots; unrelated to Gobe.

## Follow-ups (out of scope)

- Theme (light/dark) and other Fase 3 settings.
- More languages (add a `values-<lang>/strings.xml`).
- Controller management / remapping / multi-port (Fase 3).
- Per-app language via the platform `LocaleManager` (API 33+) could replace the
  attachBaseContext approach later, but the current approach is correct for minSdk 30.

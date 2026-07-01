# Gobe — Home Gamepad Navigation (shortcuts + button legend)

> Date: 2026-06-30
> Scope: Faster, more discoverable Home navigation with a physical gamepad on the ONN.
> Builds on: Fase 0/1/2 + i18n + UI redesign + Arcade + Home grid redesign (all merged to `main`).
> Origin: queued user feedback (`docs/superpowers/BACKLOG.md`), re-confirmed as real friction
> while testing (search field trapping focus; navigation not obvious).

## 1. Purpose

Two concrete asks:

1. **Shoulder shortcuts.** Reaching Search or Settings today means D-padding up through the
   grid/chips. Give the two top shoulder buttons direct jumps: **L1 → Search**, **R1 → Settings**.
2. **On-screen button legend.** New users can't tell what the buttons do. Show a subtle,
   always-visible control legend on Home so navigation is discoverable.

## 2. Decisions captured during brainstorming

- **Mapping: L1 → focus Search, R1 → open Settings.** (User picked this over Y→Settings and
  over a single-button menu.)
- **Legend reflects the app's REAL bindings: `A Select · B Back`** (Android TV convention; the
  app uses A/DPAD_CENTER to click focus and B/Back to go back). The user's original "B select,
  A exit" was inverted; the legend matches reality, not the inverted phrasing.
- **Home only** for this round (in-game legend is a deferred follow-up).

## 3. In scope

- `HomeScreen`: intercept `KEYCODE_BUTTON_L1` / `KEYCODE_BUTTON_R1` (via `onPreviewKeyEvent`
  on the screen root) → L1 requests focus on the search field, R1 calls `onOpenSettings()`.
  Consume the event so it doesn't leak into normal focus navigation.
- A **search `FocusRequester`** wired into `SearchField` so L1 can focus it (focusing the field
  brings up the IME — the desired "go to search" behavior).
- A **button-legend bar** pinned at the bottom of Home: subtle (low opacity), single line:
  `Ⓐ Select · Ⓑ Back · L1 Search · R1 Settings` (localized).
- A **pure `keyToHomeAction(keyCode): HomeKeyAction?`** helper (returns `Search`, `Settings`,
  or null) so the key mapping is unit-testable in isolation from Compose.
- i18n: legend labels as string resources in `values/` (English) and `values-es/` (Spanish).

## 4. Out of scope (deferred)

- In-game control legend (the emulator already has Select+Start → menu; a legend there is a
  future follow-up).
- Remapping A/B or any change to the actual button behavior (we only label it).
- Legends on the Detail/Settings screens.
- Additional shortcuts (filters, etc.).

## 5. Architecture

- **Key mapping (pure, testable):** a small top-level `enum class HomeKeyAction { Search, Settings }`
  and `fun keyToHomeAction(keyCode: Int): HomeKeyAction?` mapping
  `KeyEvent.KEYCODE_BUTTON_L1 -> Search`, `KEYCODE_BUTTON_R1 -> Settings`, else `null`. Placed in
  `HomeScreen.kt` (or a sibling file) and unit-tested (JVM).
- **Interception (`HomeScreen`):** add `Modifier.onPreviewKeyEvent { event -> ... }` to the root
  `Column`. On `KeyEventType.KeyDown`, look up `keyToHomeAction(event.key.nativeKeyCode)`; if
  `Search` → `searchFocus.requestFocus()` and return `true` (consume); if `Settings` →
  `onOpenSettings()` and return `true`; else `false`.
- **Search focus:** `SearchField` gains a `focusRequester` param; `HomeScreen` owns a
  `searchFocus = remember { FocusRequester() }` and passes it in, applied to the `BasicTextField`
  via `Modifier.focusRequester(...)`.
- **Legend bar:** a small `HomeControlLegend()` composable — a `Row` of label chips/text at the
  bottom of Home, `MaterialTheme.typography.labelSmall`, `onSurface` at reduced alpha, so it's
  present but unobtrusive. Rendered below the grid (outside the `LazyVerticalGrid`) so it stays
  fixed while the grid scrolls. Uses the button glyphs Ⓐ/Ⓑ and text `L1`/`R1` plus localized verbs.

## 6. Testing

- **Unit (JVM):** `keyToHomeAction(KEYCODE_BUTTON_L1) == Search`,
  `keyToHomeAction(KEYCODE_BUTTON_R1) == Settings`, `keyToHomeAction(KEYCODE_BUTTON_A) == null`.
- **On-device (ONN):** with a gamepad, L1 focuses the search field (IME appears), R1 opens
  Settings; the legend bar is visible and legible at the bottom of Home; normal D-pad + A/B
  navigation is unchanged; Spanish locale shows translated legend labels.

## 7. Risks

1. **`onPreviewKeyEvent` vs. focus-based key delivery** — preview key events on the root should
   fire before focus consumes them; if L1/R1 don't arrive (some TV remotes lack shoulder
   buttons — the user's Pro Controller has them), verify on-device. Fallback: handle in
   `MainActivity.dispatchKeyEvent` gated to the Home route.
2. **L1 while the IME/search is already focused** — L1 re-focusing search is harmless (no-op).
3. **Legend clutter** — mitigated by low opacity + single small line; tunable on-device.

## 8. Defaults (change on request)

- Legend text (localized): `Ⓐ Select · Ⓑ Back · L1 Search · R1 Settings`; bottom-aligned,
  labelSmall, onSurface ~60% alpha. L1→Search, R1→Settings. All tunable on-device.

# Gobe — Master-detail library navigation (Android TV) — Design

**Date:** 2026-07-07
**Status:** Approved design, pre-implementation
**Scope:** Core of the queued "Android TV UI/UX redesign" (see `docs/superpowers/BACKLOG.md`).
This spec covers **only** the game-browsing screen (level-2 library + search results).
Home, standalone Detail, and in-game pause are explicit follow-ups, out of scope here.

## Problem

The current game-browsing screen (`LibraryScreen`) is a `LazyVerticalGrid` of cover tiles. On a
10-foot TV with a D-pad this has two costs the redesign targets:

- **Low density / slow scan.** Few games per screen; scanning a large library (100s–1000s) is slow.
- **Too many presses to see info.** Seeing a game's description/metadata requires navigating into a
  separate full-screen `DetailScreen` (a push + a pop for every game inspected).

## Solution overview

Rework the single screen `LibraryScreen` into a **master–detail** layout (validated visually as
"Option B — poster rail + detail panel", Plex/Apple-TV style):

- **Left (~40%): a vertical poster rail** — a `LazyColumn` of selectable rows, each with a medium
  cover thumbnail + name + `year · genre` + a ★/♥ badge. Art-forward but still text-labelled.
- **Right (~60%): a live detail panel** — reflects the currently *focused* row, updating as focus
  moves. Big cover, metadata, description, and actions (Play / Resume / Favorite).

`LibraryScreen` already serves all four `LibrarySection`s (`Console`, `Recommended`, `Favorites`,
`SearchAll`), and search results render through it too. So redesigning this one screen covers **both**
level-2 library browsing and search in one change.

### Design goals (evaluation criteria)

- **Presses-to-see-info = 0**: focusing a row shows its full info in the panel immediately.
- **Presses-to-launch = 1**: `A` on a focused row launches the game directly.
- **Higher density** than the tile grid; art still aids recognition.
- **Fast traversal** of large libraries via page-jump.

## Interaction model

| Input | Action |
|-------|--------|
| **Up / Down** | Move focus in the poster rail; the detail panel updates live. |
| **A** (on a rail row) | **Launch the game directly** (`loadState = false`). 0 extra presses. |
| **Right** | Move focus into the detail panel (Play focused → Resume → Favorite). |
| **Left** (in panel) | Return focus to the rail. |
| **L1 / R1** | Page-jump the rail (previous / next screenful). |
| **B** | Back to Home. |

**On-screen legend** for this screen: `Ⓐ Jugar · Ⓑ Volver · ▶ Derecha: acciones · L1/R1 Página`.

**Legend/keymap note:** globally (Home) L1/R1 are Search/Settings (`HomeKeyMap`). Inside the library
they are repurposed to page-jump — Search/Settings are unnecessary while browsing. This is an
intentional per-screen remap; the library legend reflects the library bindings, not the Home ones.

## Components

All under `app/src/main/java/com/gobe/tv/ui/`.

### `library/LibraryScreen.kt` (rewritten)
- Top bar: section title + game count + sort indicator (cycles Recommended/Title/Year, existing
  `vm.cycleSort()`) + genre chips (existing `vm.setGenre()`).
- Body: a `Row` of two panes — the poster rail (`LazyColumn`) and the `GameDetailPanel`.
- Owns **focused-game UI state** (the focused game's `id`, held in the composable via
  `remember`/`rememberSaveable` — it is ephemeral focus state, **not** ViewModel state).
- `onPreviewKeyEvent` handler: L1/R1 → page-jump (via `LibraryKeyMap`); default D-pad handled by
  Compose focus.
- Renders the per-screen control legend.

### `library/GameRow.kt` (new)
- One rail row: medium cover thumbnail (reuses `coverUrl(game)` + `DefaultCover` fallback), name,
  `year · genre` subtitle, ★ (recommended) / ♥ (favorite) badge.
- Selected/focused state uses the section accent (orange) highlight.
- Exposes an `onFocused` callback so the screen can update the panel, and `onClick` = launch.

### `detail/GameDetailPanel.kt` (new, shared)
- Extracted from the current `DetailScreen` body: large cover (`CoverArt`), name, meta
  (system · year · players · genre), chips (★ Recomendado, 💾 Guardado disponible), description
  (`maxLines`, ellipsis), and the action buttons.
- Parameterized with callbacks: `onPlay`, `onResume` (shown only when a save state exists),
  `onToggleFavorite`, plus `playable` and `hasState` flags.
- **Reused by** `DetailScreen` (full-screen) so launch/save-state/favorite logic lives in one place.

### `detail/DetailScreen.kt` (refactored, behavior preserved)
- Keeps its role as the destination for Home's "Continue playing" (out of scope, unchanged behavior),
  but now composes `GameDetailPanel` for its right-hand content instead of inlining it.

### `library/LibraryKeyMap.kt` (new, pure)
- `enum LibraryKeyAction { PagePrev, PageNext }`
- `fun keyToLibraryAction(keyCode: Int): LibraryKeyAction?` — L1→PagePrev, R1→PageNext, else null.
- Mirrors `HomeKeyMap`; pure and unit-tested.

### ViewModel
- `LibraryViewModel` is **unchanged** — it already exposes `games`, `genres`, `selectedGenre`,
  `sortMode`, `setGenre()`, `cycleSort()`. No new reactive state needed.

### Implementation notes (from spec review)
- **`CoverArt` (DetailScreen) and `DefaultCover` (GameTile) are currently `private`.** They are not
  drop-in reusable — the plan must extract them (or relax visibility) into a shared location so both
  `GameRow` and `GameDetailPanel` can use them. Not a new behavior, just a refactor step.
- **`LibraryScreen` owns the shared `launch(loadState)` function** and feeds it to *both* the rail
  row's `A`-press (`launch(false)`) and the panel's `onPlay`/`onResume` callbacks. `GameDetailPanel`
  stays **presentational** (logic passed in via callbacks, not owned) — this reconciles "logic in one
  place" with the callback parameterization.
- **Legend text via `stringResource`** (not a hardcoded string), matching the existing
  `HomeControlLegend` pattern; add the needed string resources.
- **Favorite badge freshness:** toggling favorite in the panel should update the rail row's ♥ badge
  live. This works if `repo.searchGames(...)` is a Room-backed reactive flow that re-emits on the
  `favorite` column write (it is — same mechanism as the current library list). The plan should
  verify the row re-composes after a favorite toggle.

## Data flow

1. `LibraryScreen` collects `games`, `genres`, `selectedGenre`, `sortMode` from `LibraryViewModel`
   (unchanged).
2. On entry, focus lands on the first rail row → `focusedId` = first game → panel renders it.
3. Moving focus updates `focusedId` (via `GameRow.onFocused`) → panel re-renders live.
4. `A` on a row → `launch(loadState = false)` (same `Intent` + `EmulatorArgs` path as today, sets
   `returnToHomeOnResume = true`).
5. Save-state existence (`SaveStateStore.hasState(id)`) and favorite state are read for the focused
   game and refreshed on `ON_RESUME` (returning from the emulator), matching current `DetailScreen`
   behavior — logic now inside `GameDetailPanel`.
6. Genre/sort changes reload `games`; the screen re-focuses the first row and the panel follows.

## Edge cases

- **No cover art** → branded `DefaultCover` (existing) in both the rail row and the panel.
- **Empty list** (section or search with no results) → rail shows the `library_empty` message; panel
  shows an empty state (no game selected).
- **Missing description** → omitted (no placeholder), as today.
- **Non-playable game** (no core for its system) → Play disabled (as today).
- **Focus after filter/sort change** → re-focus first row, panel updates.
- **Long names / genres** → single-line ellipsis in rows; panel wraps.

## Testing

- **Unit (JVM, no device):**
  - `LibraryKeyMap`: L1→PagePrev, R1→PageNext, other → null.
  - A pure row-subtitle helper (e.g. `rowSubtitle(year, genre)`) mirroring `tileCaption`'s pattern
    (year/genre presence/absence combinations).
- **Manual on-device (ONN):** browse NES/SNES; verify live panel update on focus move, `A` = launch,
  Right = panel actions (Play/Resume/Favorite), L1/R1 = page-jump, ★/♥ badges, empty search, and a
  game with a save state shows Resume. Requires the ONN + adb install — **ask before installing.**

## Non-goals / follow-ups (out of scope)

- Home redesign (console Hero tiles stay as shipped in v0.3).
- In-game pause overlay redesign.
- Alphabet index / hold-to-accelerate fast-scroll (only page-jump L1/R1 is in scope).
- The competitive-analysis research deliverable and full per-screen mockup set from the backlog item
  (this spec is the implementation slice; those remain as design-research follow-ups if desired).

## References

- Backlog item: `docs/superpowers/BACKLOG.md` → "Android TV UI/UX redesign".
- Related shipped work: v0.3 richer metadata (`description`, `igdbCover`) powers the panel;
  `coverUrl`, `DefaultCover`, `SaveStateStore`, `EmulatorArgs` reused.
- Visual exploration: `.superpowers/brainstorm/` (Option B — poster rail + detail panel, refined).

# Gobe Home Screen Redesign — Cinematic Console Carousel

**Date:** 2026-07-07
**Status:** Approved (design), pending implementation plan
**Author:** brainstorming session (Gobe + Claude)

## Problem

The current home screen ([HomeScreen.kt](../../../app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt))
renders every entry as an identical flat `SectionTile` inside one
`LazyVerticalGrid`, in this order: Continue-playing rail → Recommended tile →
Favorites tile → "Consoles" header → NES / SNES / N64 / Arcade tiles.

Problems this creates:

- **Console choice is buried.** The consoles — the thing the user actually
  comes to pick — sit *below* Recommended and Favorites, and look identical to
  them.
- **No visual hierarchy / no "wow".** Same-sized controller-icon cards in a grid
  read as a settings menu, not a 10-foot entertainment surface.
- **Recommended/Favorites take prime real estate** despite being secondary.

## Goal

Redesign the home screen so that **choosing a console is the hero action**,
with a cinematic, immersive presentation appropriate for a TV ("wow" effect),
while demoting Continue-playing and removing Recommended/Favorites from the home
surface (without losing that functionality elsewhere).

Non-goals: changing the master-detail Library screen, the Detail screen,
Settings, emulation, or the scan/metadata pipeline. This spec covers the home
screen and the assets/data it needs.

## Chosen Direction — "Cinematic Carousel"

One console occupies the center of the screen at a time. Moving left/right
changes the focused console and re-tints the entire background to that console's
accent color. Selected during brainstorming over two alternatives (a 4-card
"Console Wall" and a "Hero + Rail" split) as the most immersive.

### Visual reference

Mockups produced and approved during brainstorming live in
`.superpowers/brainstorm/` (git-ignored). The approved look: full-bleed
color-tinted stage, a real photo of the console's controller on a light "display
plate" with an accent-colored glow, the console name, a game count, and a row of
console-name "dots" indicating position in the carousel.

## Layout

Full-screen stage, top → bottom:

1. **Top bar (persistent):** Gobe logo (left), search field (center, flex), ⚙
   Settings button (right). Unchanged from today functionally.
2. **Center stage (the hero):**
   - Controller photo on a light rounded "display plate" with a glow in the
     focused console's accent color.
   - Console display name (large).
   - Game count subtitle ("148 games").
   - A horizontal row of the four console names as pills; the focused one is
     filled with the accent color, the rest are muted. This is the position
     indicator, not a separate control.
3. **Left/right chevrons** (‹ ›) as affordances that more consoles exist to the
   sides. Purely decorative hints; navigation is via the D-pad.
4. **Continue strip (contextual, demoted):** a single slim `LazyRow` of square
   game tiles pinned to the bottom over a fade gradient. See "Continue-playing"
   below.
5. **Control legend** at the very bottom (as today).

Background: radial-gradient stage tinted with
`color-mix(focusedConsole.accent, background)`. The tint animates when the
focused console changes.

## Navigation & focus

- **D-pad Left / Right:** move to the previous / next console. The carousel does
  **not** wrap (NES is leftmost, Arcade is rightmost) — decided for
  predictability; revisit only if users request wrap-around.
- **D-pad Down:** move focus from the hero to the Continue strip (only if the
  strip is visible). **Up:** return focus to the top bar (search / settings).
- **A / OK on the hero:** open that console's Library (`Route.Library(section =
  Console(system))` via the existing `onOpenSection`).
- **A / OK on a Continue tile:** open that game's Detail
  (`Route.Detail(gameId)`), as today.
- **L1 = Search, R1 = Settings** — the existing `HomeKeyMap` is preserved.
- **Initial focus:** the console hero (never the Continue strip), so the primary
  action is always one button-press away.
- The focused console persists in `HomeViewModel` state so returning from a
  Library keeps the user on the same console.

## Continue-playing behavior

**Per-console, contextual (chosen).** The bottom strip shows the recent games
**for the currently focused console only**, ordered by `lastPlayed DESC`. If the
focused console has no recent games, **the strip is hidden entirely** and the
hero re-centers vertically. As the user moves between consoles, the strip
updates or disappears accordingly.

Rationale: keeps console choice the hero, keeps Continue demoted, and makes the
strip meaningful in the console-focused context (matches the user's "smaller, at
the very end / only in the context of a console" intent).

Rejected alternative: a single global "Continue playing" rail across all
systems. Recorded here in case we want to revisit; not implemented.

## What is removed / preserved

- **Removed from home:** the Recommended and Favorites `SectionTile`s.
- **Preserved:** Recommended and Favorites remain fully accessible as **filters
  inside each console's Library** — `searchGames` already supports
  `recommendedOnly` and `favoritesOnly`, and the Library exposes filtering. No
  data or capability is lost; only the top-level home tiles are removed.
- **Consoles with zero games are hidden** from the carousel to keep it clean.
- **First-run / no games at all:** if no console has any games, show the
  existing onboarding path (prompt to add ROM folders — reuse the `Route.Folders`
  / `Route.FolderBrowser` flow), not an empty carousel.

## Art / asset pipeline

- **Assets:** bundle public-domain controller photos (Evan-Amos / Vanamo Online
  Game Museum, via Wikimedia Commons — public domain, safe to ship in the APK):
  - NES — `NES-Controller-Flat.jpg` (flat top-down view)
  - SNES — `Nintendo-Super-NES-Controller.jpg`
  - N64 — `N64-Controller-Gray.jpg`
  - Downscale to a TV-appropriate size (e.g. long edge ~1080px), convert to
    WebP, store in `res/drawable-nodpi/` (alongside `gobe_logo.png`).
- **Arcade:** no suitable public-domain arcade asset sourced yet. **v1 uses the
  existing `ic_controller_arcade.xml` vector as a placeholder** on the same
  display plate; sourcing a proper arcade photo (arcade stick / cabinet) is a
  tracked follow-up, not a blocker.
- **Display-plate rendering (v1 finish):** render each photo on a light rounded
  Compose surface (light gradient background) with the console accent color as an
  outer glow, using `BlendMode.Multiply` (or equivalent) so the photo's white
  background merges into the plate — **no per-image background removal required.**
- **Transparent-float finish** (controller floating directly on the dark stage
  via background-removed transparent PNGs) is deferred as a later polish, not in
  v1.
- **Attribution:** credit Evan Amos on the licenses/About screen (courtesy;
  public domain does not require it).

## Data / state changes

`HomeViewModel` / repository must provide:

- **Per-console game counts** (to show "N games" and to decide which consoles are
  non-empty / shown). New `GameDao` query, e.g. count grouped by `system`.
- **Recent games filtered by system** (`lastPlayed IS NOT NULL` AND `system = ?`
  ORDER BY `lastPlayed DESC` LIMIT n) to feed the contextual Continue strip. This
  extends the existing `observeContinuePlaying` pattern with a system filter.
- **`focusedSystem`** in `HomeUiState`, defaulting to the first non-empty console.

`SectionVisuals` (or a small new mapping) maps each `System` to its bundled photo
drawable (and Arcade to the placeholder vector).

## Components / boundaries

- `HomeScreen.kt` — rewritten as the carousel: consumes `HomeUiState`
  (`consoles` list with counts, `focusedSystem`, `continueForFocused`), renders
  stage + hero + contextual strip + top bar, handles D-pad key events, delegates
  to `onOpenSection` / `onOpenGame` (unchanged callbacks).
- A `ConsoleHero` composable — plate + photo + name + count + dot row for one
  console; pure/stateless given a console + count.
- A `ContinueStrip` composable — the existing `GameTile` row, shown only when
  non-empty.
- `HomeViewModel.kt` — extended state and the two new data flows above.
- `GameDao` / `LibraryRepository` — the two new queries.
- `SectionVisuals.kt` — photo mapping.
- Unchanged: `GobeNavHost`, `Routes`, `LibraryScreen`, `DetailScreen`,
  `Settings`, `HomeKeyMap` (semantics preserved), theme/color tokens (the
  per-console accents already exist in `Color.kt`).

## Error / edge handling

- **Loading:** while the library is loading, show a minimal placeholder (e.g.
  the current loading state) rather than an empty carousel.
- **Focused console becomes empty** (e.g. games removed): recompute
  `focusedSystem` to the nearest non-empty console; if none remain, fall through
  to the onboarding path.
- **Missing / failed photo asset:** fall back to the console's existing vector
  controller icon on the plate (same as Arcade's placeholder path), never a
  blank plate.
- **Continue strip empty:** hide the strip; do not reserve its space.

## Testing

- **ViewModel unit tests:** per-console counts, contextual recent-games flow
  (correct system filter + ordering + limit), default `focusedSystem` selection,
  and re-selection when the focused console empties.
- **DAO tests:** the new count-by-system and recent-by-system queries against an
  in-memory Room DB.
- **UI/interaction (where the project already tests Compose):** Left/Right
  changes focused console and updates count + strip; A on hero routes to
  `Library(Console)`; A on a Continue tile routes to `Detail`; strip hidden when
  the focused console has no recent games; Recommended/Favorites tiles absent.
- **Manual on-device (ONN / Android TV):** verify D-pad flow, focus visibility,
  background re-tint animation, and photo rendering at 1080p/4K, consistent with
  the project's existing on-device verification practice.

## Open follow-ups (not blocking v1)

- Source a proper public-domain **Arcade** controller/cabinet photo to replace
  the placeholder vector.
- Optional **transparent-float** finish for the controller art.
- Optional **carousel wrap-around** if users want it.

# Gobe — Console section tiles with controller art (Home redesign)

> Date: 2026-07-06
> Scope: Replace the flat text tiles on the console-first Home (Recommended, Favorites, and one per
> console) with **Hero cards**: a per-console **controller illustration** (or ★/♥ for the special
> collections) centered on a per-section accent glow, name underneath. Plus light polish of the Home
> grid (spacing, "Consoles" header, Continue-playing row). A UI/UX pass to make the Home look
> professional. No navigation, data, or level-2 library changes.

## 1. Purpose

The level-1 Home sections are currently plain left-aligned text labels in a 96.dp Card — they read as
a debug list, not a polished 10-foot TV UI. Give each console a recognizable **controller image**
(the strongest, most attractive differentiator) and unify the special collections (Recommended /
Favorites) into the same visual language, so the first screen looks like a finished product.

## 2. Decisions captured during brainstorming

- **Imagery = the console's controller.** Each console tile shows its controller: NES pad, SNES pad
  (colored face buttons), N64 trident, arcade stick. This was chosen over brand logos (trademark risk;
  Gobe is deliberately careful about legality), live box-art collages, and pure-typographic tiles.
- **Art technique = custom `VectorDrawable` illustrations.** Hand-authored multi-path vector art (not
  simple tinted glyphs), bundled in `res/drawable/`. Chosen over photoreal controller images
  (licensing/trademark risk, heavy assets, theme poorly on dark) and over coupling art into the domain
  enum. Scales crisp at any TV size, themes cleanly, zero legal exposure.
- **Layout = "Hero card"** (Layout 1 of the mockups): controller/icon large and centered on a soft
  radial accent glow, name centered underneath. Chosen over a horizontal "banner" and a "big
  watermark" layout. Matches the existing square game-tile language.
- **Special collections = same Hero card.** Recommended uses a **★** (gold `#FFD54F`, = existing star
  badge); Favorites uses a **♥** (red, = existing heart badge). The grid stays uniform.
- **Scope = tiles + grid polish.** Beyond the tiles: tune grid cell sizing/spacing, the "Consoles"
  header typography, and the Continue-playing row's header/spacing so it doesn't crowd the grid. The
  deeper "Continue-playing is too dominant" rework remains a **separate backlog item**, not this spec.
- **Domain stays pure.** `System` (used by the data layer and unit tests without Android) does **not**
  gain color/drawable fields; the console→(color, icon) table lives in the UI layer.
- **Focus polish.** Tiles keep the TV Material Card focus scale/glow and the existing initial-focus
  behavior; on focus the accent glow intensifies.

## 3. Architecture

Three units, each with one responsibility:

### 3.1 `ui/home/SectionVisuals.kt` — pure visual mapping (new)

```kotlin
data class SectionVisual(
    @DrawableRes val iconRes: Int,   // controller drawable, or star/heart drawable
    val accent: Color,               // per-section accent used for the glow
)

/** Pure: maps a level-1 section to its icon + accent. The single source of the
 *  console -> (controller art, color) table. Unit-tested. */
fun sectionVisual(section: LibrarySection): SectionVisual
```

- `Console(system)` → the matching controller drawable + that console's accent. The inner
  `when (system)` over the `System` enum is **exhaustive with no `else`**, so adding a future console
  is a compile error until it gets art (see §5).
- `Recommended` → star drawable + gold `#FFD54F`.
- `Favorites` → heart drawable + red.
- `SearchAll` → a fixed neutral fallback: the search icon (`Icons.Filled.Search`) + the brand accent
  (`GobeAccent`). This branch is **never rendered as a section tile** (the Home sections list only
  contains Console/Recommended/Favorites), so it exists solely to keep the outer
  `when (section)` total. `SectionVisualsTest` does **not** assert on it.

Labels continue to come from string resources at the call site (unchanged), so this unit stays free of
`Context`/`stringResource` and is trivially testable.

### 3.2 Controller + collection drawables (new assets)

`res/drawable/ic_controller_nes.xml`, `ic_controller_snes.xml`, `ic_controller_n64.xml`,
`ic_controller_arcade.xml` — refined vector versions of the brainstorm mockups (multi-path, with the
controllers' own button colors baked in, so they are illustrations rather than single-tint icons).
Star and heart use Material's `Icons.Filled.Star` / `Icons.Filled.Favorite` (tinted gold/red) — no new
asset needed — OR small vector drawables if a filled Material glyph looks off; decide at implementation.

### 3.3 `SectionTile` Hero card (rewrite in `HomeScreen.kt`)

Replace the current text-in-a-Card `SectionTile` with:

- A TV Material `Card` (keeps `onClick`, focus scale/glow, and the `requestInitialFocus` behavior).
- A consistent aspect ratio (~1.3:1) so tiles rhyme with the square game tiles.
- Inside a `Box`:
  - a **radial gradient glow** using `visual.accent` (brighter when the card is focused — read focus
    via `Card`'s interaction/focus state);
  - the `visual.iconRes` image centered and large (`Image`/`Icon`, `ContentScale.Fit`);
  - the `label` centered below in `titleMedium`/`labelLarge`.

The tile takes `(label: String, visual: SectionVisual, onClick, requestInitialFocus)`.

### 3.4 Home grid polish (`HomeScreen.kt`)

- Adjust `LazyVerticalGrid` cell sizing (e.g. `Adaptive(180.dp)`) and gaps so Hero cards breathe.
- "Consoles" header placement: **today the single `itemsIndexed(sections)` pass renders one merged
  list (Recommended + Favorites + all consoles), so the header currently sits above the whole thing.**
  To put it *between* the collections and the console tiles, split that pass into two item groups —
  the two collection tiles first, then a full-span "Consoles" header item, then the per-console tiles —
  building the two groups from `LibrarySection`'s `Recommended`/`Favorites` vs `Console(...)` kinds.
  Small uppercase section-header typography, consistent spacing.
- Continue-playing `LazyRow`: give it a clear header and spacing consistent with the rest so it reads
  as one row and doesn't visually dominate. (Light touch only.)

## 4. Colors

| Section      | Accent            | Note                                  |
|--------------|-------------------|---------------------------------------|
| NES          | warm red          | distinct from Favorites red (below)   |
| SNES         | violet `#6C5CE7`  | distinct from brand accent `#7C5CFF`  |
| N64          | teal `#17B3A3`    |                                       |
| Arcade       | amber `#F5A623`   |                                       |
| Recommended  | gold `#FFD54F`    | = existing ★ badge                    |
| Favorites    | red `#E53935`     | = existing ♥ badge                    |

⚠️ **Red collision:** Favorites red (`#E53935`) and a naïve NES red would look identical. Resolve by
giving NES a distinct hue (e.g. a deeper/orange-leaning red) or shifting Favorites toward rose. Exact
hexes finalized in the implementation plan; the accents are defined as named constants (likely in
`ui/theme/Color.kt`) so they are reviewable in one place.

## 5. Error / edge handling

- A drawable fails to load → `Image`/`Icon` simply renders nothing over the glow; the label + card
  remain, so the tile is still usable (no crash). Not expected for bundled vectors.
- New consoles added to `System` later → `sectionVisual` must handle every enum value; a `when` over
  `System` with no `else` makes a missing mapping a **compile error** (safer than a runtime default).
- Long labels → single line, ellipsis, as today.

## 6. Testing

- **Unit test** `SectionVisualsTest`: every `System` value and `Recommended`/`Favorites` return the
  expected `iconRes` + `accent`; asserts NES ≠ Favorites accent (guards the color collision). Follows
  the repo's pattern of testing pure functions (JUnit4, `:app:testDebugUnitTest`).
- **Compile:** `:app:assembleDebug`.
- **On-device (ONN):** the Home shows controller Hero cards for the 4 consoles + ★/♥ collections,
  focus moves correctly with the D-pad and the glow intensifies on focus, labels are legible at 10ft,
  and nothing is clipped.

## 7. Out of scope

- Navigation, `LibrarySection`, data layer, the level-2 `LibraryScreen`, game tiles.
- The "Continue-playing is too dominant" rework (separate backlog item — only light spacing polish
  here).
- Per-console theming beyond the tile accent (e.g. tinting the level-2 screen) — not now.

## 8. Files

**Create:**
- `app/src/main/java/com/gobe/tv/ui/home/SectionVisuals.kt`
- `app/src/main/res/drawable/ic_controller_{nes,snes,n64,arcade}.xml`
- `app/src/test/java/com/gobe/tv/ui/home/SectionVisualsTest.kt`
- (maybe) `ic_section_recommended.xml` / `ic_section_favorites.xml` if Material glyphs look off.

**Modify:**
- `app/src/main/java/com/gobe/tv/ui/home/HomeScreen.kt` — `SectionTile` rewrite + grid polish.
- `app/src/main/java/com/gobe/tv/ui/theme/Color.kt` — named accent constants.

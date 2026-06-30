# Gobe — Fase 0 + Fase 1 Design (TV Shell + Library Browser)

> Date: 2026-06-30
> Scope: First coherent, testable sub-project of the Gobe Android TV emulator.
> Parent spec: [RETRO_TV_SPEC.md](../../../RETRO_TV_SPEC.md) (formerly "RetroTV"; app is now **Gobe**).

## 1. Purpose

Gobe is a personal all-in-one Libretro frontend for Android TV (ONN Plus 4K). This
sub-project builds **only Fase 0 (skeleton) + Fase 1 (library browser)** — a TV-native
app that scans real ROM files and presents a D-pad-navigable library. **No emulation**
is included; that is a later sub-project (Fase 2).

The goal of this milestone is to **de-risk the foundation end to end**: toolchain →
APK build → deploy to the ONN → `MANAGE_EXTERNAL_STORAGE` actually granting file
access (the exact thing every competitor app failed at) → a Compose-for-TV grid with
correct, visible focus. It must be visible and usable on the real TV without touching
the heavy, GPL-licensed native core integration.

## 2. Decisions captured during brainstorming

- **App name:** Gobe (replaces "RetroTV").
- **Build/test workflow:** full local loop on the user's Mac — install JDK + Android
  SDK + Gradle, build APKs locally, sideload to the ONN over the network via `adb`.
- **First sub-project:** Fase 0 + Fase 1 combined (shell + library), emulation deferred.
- **ROM packaging:** **ROMs are NOT bundled in the APK.** The app scans real folders on
  the device via `java.io.File`. Bundling would bypass the very storage architecture this
  milestone exists to validate, would be inflexible (rebuild to add a game), and conflicts
  with the project's "never include/distribute ROMs" rule (parent spec §6.7). The ONN
  already holds the ROMs; the Mac copy is just a backup/source.
- **ROM path:** confirmed via `adb shell` during implementation, then baked in as the
  default scan path. App also supports adding/removing folders in-app.
- **Device access:** ONN is on the same network; wireless debugging enabled in Fase 0.

## 3. In scope

- Android TV project skeleton that builds, deploys, and launches into a TV-native UI
  (appears in the Android TV launcher via the Leanback launcher intent).
- `MANAGE_EXTERNAL_STORAGE` onboarding flow: detect → route to system "All files access"
  → return → re-check → proceed.
- ROM folder scanner over real `java.io.File` paths (no SAF), configurable, with a
  confirmed default path.
- System detection by file extension; cleaned display names.
- Room persistence: `Game` and `RomFolder` tables.
- Compose-for-TV home: rows grouped by system, D-pad navigable, correct/visible focus,
  OK → game detail stub, Back → exit cleanly (only from Home).
- Minimal in-app ROM-folder management: add/remove/enable a folder via a custom D-pad
  folder browser (the deliberate replacement for the broken SAF picker).
- Game detail **stub** screen (no launch).

## 4. Out of scope (deferred to later sub-projects)

- Any emulation: LibretroDroid, `.so` cores, game launching (→ Fase 2 spec).
- Boxart scraping (tiles show cleaned name on a colored placeholder).
- Arcade `.zip` romset **validation** against a core's romset list (needs the core). In
  Fase 1, `.zip` files are listed under an "Arcade (unverified)" group.
- "Continuar jugando" row **with data** — the `lastPlayed` field exists in the model now,
  but the row stays hidden while empty; it only fills once Fase 2 enables playing.
- Per-core / per-game settings, controller-port management (P1–P4), button remapping,
  USB removable volumes, BIOS management.

## 5. Platform decisions

Recommended defaults; the SDK/Android-version values are confirmed via `adb` during setup.

- **minSdk 30 (Android 11)** — required by `MANAGE_EXTERNAL_STORAGE`; the ONN supports it.
  Target the latest stable SDK.
- **Language/UI:** Kotlin + Jetpack Compose for TV — `androidx.tv:tv-material3` and
  `androidx.tv:tv-foundation` (lazy grid/rows). Leanback launcher intent + banner so the
  app appears on the Android TV home.
- **Persistence:** Room. **Async/reactive:** Kotlin Coroutines + Flow.
- **DI:** intentionally minimal — manual construction + a ViewModel factory. No Hilt yet
  (AGENTS.md: avoid abstractions before the first end-to-end flow). Revisit for Fase 2.
- **Modules:** single app module for now (no multi-module split yet).
- **Package:** `com.gobe.tv`.

## 6. Architecture

### 6.1 Package structure (single module, clear boundaries)

```
com.gobe.tv
├─ data
│  ├─ db        (GobeDatabase, GameDao, RomFolderDao, entities)
│  ├─ scan      (RomScanner: walks folders → candidate files)
│  └─ system    (SystemDetector: extension → System; name cleaner)
├─ domain       (Game, System, RomFolder models; LibraryRepository)
├─ ui
│  ├─ onboarding (permission flow)
│  ├─ home       (library grid, rows-by-system, focus)
│  ├─ folders    (D-pad folder browser + folder list)
│  ├─ detail     (game detail stub)
│  └─ theme
└─ MainActivity / GobeApp
```

Each unit is independently understandable and testable:
- `SystemDetector` + name cleaner: pure functions → straightforward JVM unit tests.
- `RomScanner`: takes injectable root path(s) → testable against a temp directory.
- `LibraryRepository`: mediates scan ↔ DB ↔ UI; the single source of library state.

### 6.2 Data model (Room)

```
Game(
  id: Long (PK, autogen),
  path: String (unique index),
  system: System (enum, stored as String),
  displayName: String,
  fileName: String,
  sizeBytes: Long,
  lastPlayed: Long? (epoch millis, nullable),
  dateAdded: Long (epoch millis)
)

RomFolder(
  id: Long (PK, autogen),
  path: String (unique index),
  enabled: Boolean (default true)
)
```

`System` enum: `NES, SNES, N64, ARCADE` (extensible). Arcade entries from Fase 1 are
treated as "unverified" (no romset validation yet).

### 6.3 System detection

| Extension(s)        | System            |
|---------------------|-------------------|
| `.nes`              | NES               |
| `.smc`, `.sfc`      | SNES              |
| `.z64`, `.n64`, `.v64` | N64            |
| `.zip`              | ARCADE (unverified) |

Unknown extensions are ignored. Matching is case-insensitive.

**Name cleaner:** strip the extension, strip bracket/paren tags (e.g. `(USA)`, `(En,Fr)`,
`[!]`, `[b1]`), collapse separators (`_`, `.`) to spaces, and normalize whitespace.
Example: `Super_Mario_64 (USA).z64` → `Super Mario 64`.

### 6.4 Scan flow

On launch and on manual "Refresh":
1. Repository reads enabled `RomFolder`s.
2. Off the main thread, recursively walks each folder via `java.io.File`.
3. Detects system per file; computes cleaned display name.
4. Upserts into Room, diffing by `path`: insert new files, keep unchanged, delete rows
   whose backing file no longer exists. `lastPlayed` and `dateAdded` are preserved across
   rescans for surviving entries.
5. UI observes Room via Flow → grid updates reactively.

Errors (unreadable folder, permission revoked mid-session) surface as a non-blocking
banner; the app never crashes on a bad path.

## 7. Screens and focus (the real TV work)

What makes a TV app feel right is focus management, not aesthetics. Each screen sets an
explicit initial `focusRequester`; tiles use `Modifier.focusable()`; no focus traps;
**Back closes the app only from Home.**

1. **Onboarding / permission** — shown only if `MANAGE_EXTERNAL_STORAGE` is not granted.
   Explains why, with one large focused "Grant access" button → opens the system All-files-
   access screen → on return, re-checks; if granted, proceeds to Home; Back exits.

2. **Home / library** — top bar (`Gobe`, a gamepad-count indicator **placeholder**,
   ⚙ Ajustes entry). A vertical list of system rows (NES, SNES, Arcade, N64); each row is
   a horizontal `TvLazyRow` of tiles. "Continuar jugando" row is hidden until it has data.
   Focused tile shows scale + border. OK → game detail stub. Back → exit. Loading and
   empty states: if there are no folders or no games, route the user to folder management
   with a clear call to action.

3. **Folder management** (Fase-1 settings slice) — lists current ROM folders with
   enable/remove, plus "Add folder" → a **custom D-pad folder browser** that lists
   directories from `/storage/emulated/0` downward (OK to enter a directory, an explicit
   "Use this folder" action to select). This is the deliberate replacement for the broken
   SAF picker.

4. **Game detail (stub)** — cleaned name, system, file info, and a disabled
   "▶ Jugar (próximamente)". Proves OK-navigation and the back stack without emulation.
   The real detail screen and launch arrive in Fase 2.

## 8. Testing strategy

- **Unit (JVM, written test-first):** `SystemDetector` extension mapping; name-cleaner
  edge cases (tags, separators, multi-region); scan-diff logic (added/removed/unchanged,
  preservation of `lastPlayed`/`dateAdded`); repository grouping into system rows.
- **Instrumented (lighter):** Room DAO CRUD round-trips; a smoke test that Home renders
  rows from seeded data.
- **Manual on-device checklist (cannot be unit-tested):** permission round-trip; folder
  scan finds expected files; D-pad focus moves cleanly between tiles/rows with no lost
  focus; Back behavior (exits only from Home); empty-state path.

## 9. Build and deploy loop

The user chose to set up tooling on the Mac. Performed during implementation (not during
design); recorded here as the target loop:

1. Install JDK 17 + Android command-line SDK tools + required platform/build-tools.
   Gradle via the project wrapper (no global Gradle needed).
2. Enable wireless debugging on the ONN; `adb connect <ONN-ip>:5555`.
3. `adb shell` to confirm the Android version and the real ROM folder path; finalize the
   default scan path and minSdk accordingly.
4. `./gradlew installDebug` to deploy; run the on-device test checklist (§8) each build.

## 10. Risks and notes

- **Focus feel is only judgeable on-device** — the ONN remote is the real test; budget
  iteration time here.
- **minSdk / Android version** are assumptions until confirmed via `adb`; the storage
  model requires API 30+, which the ONN supports.
- **Permission revocation:** the user can revoke All-files-access in system settings; the
  app must re-detect on resume and route back to onboarding rather than crash.
- **Arcade in Fase 1 is unverified** — listing `.zip` without romset validation may show
  non-game zips; acceptable for this milestone, resolved when the arcade core lands.

## 11. Open defaults (change on request)

- minSdk 30; package `com.gobe.tv`; no Hilt; single module; default ROM path TBD via adb.

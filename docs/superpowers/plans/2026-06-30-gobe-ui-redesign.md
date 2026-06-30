# Gobe UI Redesign + Box Art + Player Counts + In-Game Menu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign Gobe's library into a searchable box-art grid with system filter chips and player-count badges, fed by a bundled offline metadata index + on-demand libretro box art (Coil), and add a gamepad-accessible in-game menu (Select+Start) with "Exit to Gobe".

**Architecture:** Pure, unit-tested helpers (`BoxartUrlBuilder`, `NameNormalizer`, `GameMatcher`, `MetadataIndex`) resolve box-art URLs + player counts by normalized name; results persist in Room (migration 1→2 adds `players`,`boxartName`). Compose-for-TV Home becomes a `LazyVerticalGrid` with live Room-backed search + chips; `EmulatorActivity` detects Select+Start to open the pause menu. A committed build-time tool generates the metadata JSON assets.

**Tech Stack:** Kotlin, Jetpack Compose for TV, Coil (`coil-compose`), Room (migration), Coroutines/Flow, libretro-thumbnails + libretro-database.

**Spec:** [docs/superpowers/specs/2026-06-30-gobe-ui-redesign-design.md](../specs/2026-06-30-gobe-ui-redesign-design.md)

**Branch:** create `feat/ui-redesign` off `main`. **Device:** ONN Plus 4K (Android 14, `armeabi-v7a`), `adb connect 192.168.1.219:5555` (confirm with `adb devices`).

---

## Conventions

Export the toolchain before any gradle/adb command; pass `dangerouslyDisableSandbox: true`:
```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```
Builds: `./gradlew --no-daemon ...`. git identity configured. End commits with:
```
Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
```
New user-facing strings MUST be added to BOTH `res/values/strings.xml` (English) and
`res/values-es/strings.xml` (Spanish), per the existing i18n setup, and referenced via
`stringResource(...)` / `getString(...)`.

## File Structure

```
app/src/main/
├─ AndroidManifest.xml                     (+ INTERNET permission)
├─ assets/metadata/{snes,nes,n64,arcade}.json   (generated; Task 9)
├─ java/com/gobe/tv/
│  ├─ data/art/BoxartUrlBuilder.kt          (system+name -> libretro URL)
│  ├─ data/metadata/NameNormalizer.kt       (canonical normalization)
│  ├─ data/metadata/MetadataIndex.kt        (load assets/metadata/*.json)
│  ├─ data/metadata/GameMatcher.kt          (game -> {players, boxartName})
│  ├─ data/db/GameEntity.kt                 (+ players, boxartName)
│  ├─ data/db/GameDao.kt                    (+ searchGames; + update meta)
│  ├─ data/db/GobeDatabase.kt               (v2 + Migration(1,2))
│  ├─ data/LibraryRepository.kt             (populate meta on rescan; searchGames)
│  ├─ ui/home/HomeScreen.kt + HomeViewModel.kt + GameTile.kt   (grid + search + chips)
│  ├─ ui/detail/DetailScreen.kt             (cover art + players)
│  └─ emulation/EmulatorActivity.kt         (Select+Start combo; Exit to Gobe)
├─ res/values/strings.xml + values-es/strings.xml  (new strings)
tools/build-metadata-index/                  (committed generator; Task 9)
app/src/test/java/com/gobe/tv/...            (unit tests)
```

---

## Task 1: INTERNET permission + Coil + BoxartUrlBuilder (TDD)

**Files:** Modify `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`; Create `data/art/BoxartUrlBuilder.kt`, test.

- [ ] **Step 1: Manifest + Coil dep**
  - Add to `AndroidManifest.xml` (above `<application>`): `<uses-permission android:name="android.permission.INTERNET" />`
  - In `libs.versions.toml`: `coil = "2.7.0"`; `[libraries]` `coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }`.
  - In `app/build.gradle.kts` deps: `implementation(libs.coil.compose)`.

- [ ] **Step 2: Failing test** `app/src/test/java/com/gobe/tv/data/art/BoxartUrlBuilderTest.kt`:
```kotlin
package com.gobe.tv.data.art

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoxartUrlBuilderTest {
    private val b = BoxartUrlBuilder()

    @Test fun snesUrl() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Super%20Mario%20World.png",
        b.url(System.SNES, "Super Mario World"),
    )
    // libretro replaces & * / : ` < > ? \ | " with _ in the filename, then it is URL-encoded.
    @Test fun sanitizesIllegalChars() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Super%20Nintendo%20Entertainment%20System/Named_Boxarts/Tom%20_%20Jerry.png",
        b.url(System.SNES, "Tom & Jerry"),
    )
    @Test fun nullWhenNoName() = assertNull(b.url(System.SNES, null))
    @Test fun nesFolder() = assertEquals(
        "https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%20Entertainment%20System/Named_Boxarts/Contra.png",
        b.url(System.NES, "Contra"),
    )
}
```

- [ ] **Step 3: Run → fail.** `./gradlew --no-daemon :app:testDebugUnitTest --tests "com.gobe.tv.data.art.BoxartUrlBuilderTest"`

- [ ] **Step 4: Implement** `data/art/BoxartUrlBuilder.kt`:
```kotlin
package com.gobe.tv.data.art

import com.gobe.tv.domain.System
import java.net.URLEncoder

/** Builds a libretro-thumbnails box-art URL for a (system, canonical name). */
class BoxartUrlBuilder {
    fun url(system: System, name: String?): String? {
        if (name.isNullOrBlank()) return null
        val folder = folder(system) ?: return null
        val sanitized = name.replace(Regex("""[&*/:`<>?\\|"]"""), "_")
        return "https://thumbnails.libretro.com/${enc(folder)}/Named_Boxarts/${enc(sanitized)}.png"
    }

    private fun folder(system: System): String? = when (system) {
        System.SNES -> "Nintendo - Super Nintendo Entertainment System"
        System.NES -> "Nintendo - Nintendo Entertainment System"
        System.N64 -> "Nintendo - Nintendo 64"
        System.ARCADE -> "MAME"
    }

    // Encode for a path segment: %20 for spaces (URLEncoder uses + for spaces, so fix it).
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
```

- [ ] **Step 5: Run → pass.** **Step 6: build** `./gradlew --no-daemon assembleDebug` (Coil resolves). **Commit** `feat(ui): INTERNET + Coil + BoxartUrlBuilder with tests`.

---

## Task 2: NameNormalizer + MetadataIndex + GameMatcher (TDD)

Pure name resolution against a bundled JSON index. Tests use a tiny in-test JSON; the real assets come in Task 9.

**Files:** Create `data/metadata/{NameNormalizer,MetadataIndex,GameMatcher}.kt` + tests.

- [ ] **Step 1: NameNormalizer test + impl.**
Test `NameNormalizerTest.kt`:
```kotlin
package com.gobe.tv.data.metadata
import org.junit.Assert.assertEquals
import org.junit.Test
class NameNormalizerTest {
    private val n = NameNormalizer()
    @Test fun stripsTagsCaseAndPunctuation() {
        assertEquals("supermarioworld", n.normalize("Super Mario World (USA) [!]"))
        assertEquals("themadams", n.normalize("The M.Adams"))
        assertEquals("finalfightguy", n.normalize("Final Fight Guy (NA)"))
    }
}
```
Impl `NameNormalizer.kt`:
```kotlin
package com.gobe.tv.data.metadata
/** Normalizes a game name for matching: lowercase, drop bracket/paren tags, leading "the",
 *  and all non-alphanumerics. */
class NameNormalizer {
    private val tags = Regex("""[\(\[].*?[\)\]]""")
    fun normalize(name: String): String {
        var s = tags.replace(name, " ").lowercase().trim()
        if (s.startsWith("the ")) s = s.removePrefix("the ")
        return s.replace(Regex("""[^a-z0-9]"""), "")
    }
}
```
Run → fail → pass.

- [ ] **Step 2: MetadataIndex test + impl.** The index parses a JSON object `{ "<normalized>": {"boxart": "<canonical>", "players": N}, ... }`.
Test (parse from a String, no Android):
```kotlin
package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class MetadataIndexTest {
    private val json = """{"supermarioworld":{"boxart":"Super Mario World","players":2},
                          "contra":{"boxart":"Contra","players":2}}"""
    @Test fun looksUpNormalized() {
        val idx = MetadataIndex.parse(json)
        assertEquals("Super Mario World", idx["supermarioworld"]?.boxart)
        assertEquals(2, idx["contra"]?.players)
        assertNull(idx["unknown"])
    }
}
```
Impl `MetadataIndex.kt` (use `org.json.JSONObject` — available on Android; for the JVM unit test add `testImplementation("org.json:json:20240303")` to `app/build.gradle.kts`):
```kotlin
package com.gobe.tv.data.metadata

import org.json.JSONObject

data class GameMeta(val boxart: String?, val players: Int?)

class MetadataIndex(private val map: Map<String, GameMeta>) {
    operator fun get(normalized: String): GameMeta? = map[normalized]
    companion object {
        fun parse(json: String): MetadataIndex {
            val obj = JSONObject(json)
            val m = HashMap<String, GameMeta>(obj.length())
            for (key in obj.keys()) {
                val o = obj.getJSONObject(key)
                m[key] = GameMeta(
                    boxart = o.optString("boxart").ifBlank { null },
                    players = if (o.has("players")) o.optInt("players") else null,
                )
            }
            return MetadataIndex(m)
        }
    }
}
```

- [ ] **Step 3: GameMatcher test + impl.** `GameMatcher(normalizer)` matches a name to a `GameMeta` using an index it is given.
Test:
```kotlin
package com.gobe.tv.data.metadata
import org.junit.Assert.*
import org.junit.Test
class GameMatcherTest {
    private val idx = MetadataIndex.parse("""{"supermarioworld":{"boxart":"Super Mario World","players":2}}""")
    private val m = GameMatcher(NameNormalizer())
    @Test fun matchesByNormalizedName() {
        val meta = m.match("Super Mario World (USA) [!]", idx)
        assertEquals("Super Mario World", meta?.boxart); assertEquals(2, meta?.players)
    }
    @Test fun nullWhenNoMatch() = assertNull(m.match("Nonexistent Game", idx))
}
```
Impl:
```kotlin
package com.gobe.tv.data.metadata
class GameMatcher(private val normalizer: NameNormalizer) {
    fun match(displayName: String, index: MetadataIndex): GameMeta? =
        index[normalizer.normalize(displayName)]
}
```
Run all three → pass. **Commit** `feat(meta): name normalizer + metadata index + matcher with tests`.

---

## Task 3: Room migration 1→2 (players, boxartName) + searchGames

**Files:** Modify `data/db/GameEntity.kt`, `GameDao.kt`, `GobeDatabase.kt`; add to instrumented `GameDaoTest`.

- [ ] **Step 1:** Add to `GameEntity`: `val players: Int? = null`, `val boxartName: String? = null`.
- [ ] **Step 2:** `GameDao` add:
```kotlin
@Query("SELECT * FROM games WHERE displayName LIKE '%' || :q || '%' AND (:system IS NULL OR system = :system) ORDER BY displayName COLLATE NOCASE ASC")
fun searchGames(q: String, system: String?): kotlinx.coroutines.flow.Flow<List<GameEntity>>

@Query("UPDATE games SET players = :players, boxartName = :boxartName WHERE id = :id")
suspend fun updateMeta(id: Long, players: Int?, boxartName: String?)
```
- [ ] **Step 3:** `GobeDatabase`: bump `version = 2`; add:
```kotlin
val MIGRATION_1_2 = androidx.room.migration.Migration(1, 2) { db ->
    db.execSQL("ALTER TABLE games ADD COLUMN players INTEGER")
    db.execSQL("ALTER TABLE games ADD COLUMN boxartName TEXT")
}
```
and register it where the DB is built (in `GobeApp`: `Room.databaseBuilder(...).addMigrations(GobeDatabase.MIGRATION_1_2).build()` — expose `MIGRATION_1_2` from a companion).
- [ ] **Step 4:** Instrumented test in `GameDaoTest.kt`: a migration test using `MigrationTestHelper` OR a simpler test that inserts a game with players/boxartName and reads it back via `searchGames`. Run `./gradlew --no-daemon :app:connectedDebugAndroidTest`.
- [ ] **Step 5: Commit** `feat(db): players/boxartName columns + migration 1->2 + searchGames`.

---

## Task 4: Repository — populate metadata on rescan + search

**Files:** Modify `data/LibraryRepository.kt`, `GobeApp.kt`.

- [ ] **Step 1:** `GobeApp` builds a `GameMatcher` + per-system `MetadataIndex` (loaded from assets lazily; if an asset is missing, treat as empty). Inject into the repository (constructor or setter).
- [ ] **Step 2:** In `rescan()`, after inserting new games, for each game without metadata, `matcher.match(displayName, indexFor(system))` and `gameDao.updateMeta(id, meta?.players, meta?.boxart)`. Keep it incremental (only games with null metadata).
- [ ] **Step 3:** Add `fun searchGames(query: String, system: System?): Flow<List<Game>>` mapping `gameDao.searchGames(query, system?.name)`.
- [ ] **Step 4:** Build (`assembleDebug`) + unit tests green. **Commit** `feat(repo): populate players/boxart on rescan + searchGames`.

---

## Task 5: GameTile with box art (Coil) + player badge

**Files:** Modify `ui/home/GameTile.kt`. Add strings if any.

- [ ] **Step 1:** `GameTile` builds the box-art URL via `BoxartUrlBuilder().url(game.system, game.boxartName ?: game.displayName)` and renders Coil `AsyncImage` filling the tile, with `error`/`fallback` = the existing text-tile content. Overlay a small badge `👥${game.players}` (top-end) when `game.players != null && game.players >= 2`. Keep focus scale/border (tv-material3 Card).
- [ ] **Step 2:** Build + install + on-device check: tiles that match show box art (after a moment, cached), others show text; badges appear on known multiplayer games. (Real coverage improves after Task 9.) **Commit** `feat(ui): GameTile box art via Coil + player badge`.

---

## Task 6: Home redesign — search + chips + LazyVerticalGrid

**Files:** Modify `ui/home/HomeViewModel.kt`, `HomeScreen.kt`. New strings: `search_hint`, `filter_all`.

- [ ] **Step 1: HomeViewModel** — add `query` + `selectedSystem` state; expose a `StateFlow<HomeState>` where the grid is `repo.searchGames(query, selectedSystem)` (debounced/`flatMapLatest` on query+system), plus `continuePlaying`. Keep the initial scan/`rescan` + `ensureDefaultFolder`. Unit-test the query/system state transitions where pure.
- [ ] **Step 2: HomeScreen** — top bar: logo + a **search field** (Compose `TextField`/`BasicTextField` that raises the system IME on focus; updates `vm.query`) + ⚙ Settings (keep the UP-reachability fix). A **Row of system filter chips** (All / SNES / NES / Arcade / N64) toggling `vm.selectedSystem`. A fixed **Continue-playing** `LazyRow` (when present). Then a **`LazyVerticalGrid`** (`GridCells.Adaptive(140.dp)` or `Fixed(6)`) of `GameTile`s from the filtered list, D-pad navigable, initial focus on the first tile (or the search field). OK on a tile → detail.
- [ ] **Step 3:** Build + install + on-device: typing filters live; chips filter by system; grid scrolls; focus is clean; continue row works. **Commit** `feat(ui): searchable box-art grid Home with system filter chips`.

---

## Task 7: Detail redesign — cover art + players

**Files:** Modify `ui/detail/DetailScreen.kt`. New strings: `detail_players`.

- [ ] **Step 1:** Two-column detail: left = large Coil `AsyncImage` cover (fallback placeholder), right = name, system, **players** (`stringResource(R.string.detail_players)` + count or "—"), size, save-state line, and the existing buttons (Play / Resume-from-save / Back). Keep the `hasState` resume logic + the ON_RESUME refresh.
- [ ] **Step 2:** Build + install + on-device: detail shows the cover + player line. **Commit** `feat(ui): detail with cover art + player count`.

---

## Task 8: In-game menu — Select+Start + Exit to Gobe

**Files:** Modify `emulation/EmulatorActivity.kt`, `emulation/ui/PauseOverlay.kt`. New string: `pause_exit_to_gobe`.

- [ ] **Step 1:** In `EmulatorActivity.dispatchKeyEvent`, track held buttons: when both `KEYCODE_BUTTON_SELECT` and `KEYCODE_BUTTON_START` are down (or `KEYCODE_BUTTON_MODE`, or remote `KEYCODE_BACK`), open the pause overlay (toggle), and **swallow** those events (don't forward Select/Start to the core while the combo is active). Keep forwarding all other inputs when not paused.
- [ ] **Step 2:** PauseOverlay: keep Resume/Save/Load; rename/relabel the exit to **"Exit to Gobe" / "Salir a Gobe"** (`pause_exit_to_gobe`).
- [ ] **Step 3:** "Exit to Gobe" auto-saves (state+SRAM) and `finish()`es. To land on the **grid**: the library `MainActivity` should reset its nav route to `Home` on resume after returning from the emulator (e.g., the NavHost re-checks on ON_RESUME, or set a flag). Implement so exiting a game shows the grid, not the detail.
- [ ] **Step 4:** Build + install + on-device with the gamepad: from a running SNES game, **Select+Start opens the menu**; "Exit to Gobe" returns to the grid to pick another game. **Commit** `feat(emu): Select+Start in-game menu + Exit to Gobe (returns to grid)`.

---

## Task 9: Metadata index dataset (generator tool + bundled assets)

This is the data-heavy task (the spec's main risk). Produces `assets/metadata/<system>.json`.

**Files:** Create `tools/build-metadata-index/` (a committed script) + `app/src/main/assets/metadata/{snes,nes,n64,arcade}.json`.

- [ ] **Step 1:** Build a generator (Kotlin/Python/Node — pick one, commit it) that reads the **libretro database** for each supported system and emits `{ "<normalized name>": {"boxart":"<canonical name>","players":N} }`. Primary source: the libretro `.rdb` "users" field + the No-Intro/canonical names. The `boxart` value is the canonical name used by libretro-thumbnails `Named_Boxarts`.
- [ ] **Step 2 (fallback if RDB parsing is impractical):** derive a curated players dataset for the supported systems (e.g. from openvgdb or a published multiplayer list) and use the No-Intro names for `boxart`. Document the source + coverage in RESULTS.
- [ ] **Step 3:** Run the tool → write the JSON assets. Keep them reasonably sized (a few hundred KB/system is fine).
- [ ] **Step 4:** Build + install + on-device: rescan now populates players/boxart; the grid shows real box art for matched games and 👥 badges. Record approximate match coverage. **Commit** `feat(meta): bundled libretro metadata index (players + boxart names) + generator`.

(Note for the controller: this task is exploratory/data-heavy — it may be handled directly rather than by an autonomous subagent.)

## Task 10: Localize new strings

- [ ] **Step 1:** Ensure every new string added in Tasks 1–9 exists in BOTH `values/strings.xml` and `values-es/strings.xml` (`search_hint`, `filter_all`, `detail_players`, `pause_exit_to_gobe`, etc.). Grep for stray literals. Build. **Commit** `feat(i18n): localize new UI-redesign strings`.

## Task 11: On-device acceptance + RESULTS + finish

- [ ] **Step 1:** `./gradlew --no-daemon :app:testDebugUnitTest` + `:app:connectedDebugAndroidTest` → green.
- [ ] **Step 2: Manual checklist on the ONN** (gamepad + remote):
  - [ ] Search filters the grid live; system chips filter by console.
  - [ ] Box art loads + caches; matched games show covers, others show text fallback.
  - [ ] Player badges (👥N) show on known multiplayer games.
  - [ ] Continue-playing row works; grid focus is clean.
  - [ ] Detail shows cover + player count.
  - [ ] In a running game, **Select+Start** opens the menu; "Exit to Gobe" returns to the grid.
  - [ ] Room migration upgraded an existing install without data loss (no rescan-from-zero).
- [ ] **Step 3:** Write `docs/superpowers/plans/2026-06-30-gobe-ui-redesign-RESULTS.md` (outcome, match coverage, screenshots, deviations, players-dataset source). Commit.
- [ ] **Step 4:** Finish the branch — use superpowers:finishing-a-development-branch (merge to `main`, push to `origin`).

---

## Notes for the implementer

- Coil needs the INTERNET permission (Task 1) — without it, all box art fails silently.
- Box art works from `displayName` even before the metadata index exists (Task 9 improves hit rate via canonical names); build the UI first, fill data last.
- Keep `System.displayName` and game titles unlocalized; only chrome strings are localized.
- The Select+Start combo must not leak Select/Start to the core (swallow while combo fires).
- Reference skills: @superpowers:test-driven-development (Tasks 1–4), @superpowers:systematic-debugging (device issues), @superpowers:verification-before-completion (Task 11).
```

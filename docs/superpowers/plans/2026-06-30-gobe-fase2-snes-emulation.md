# Gobe Fase 2 (SNES) Emulation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Play SNES games in Gobe — integrate LibretroDroid + the `snes9x` core to launch a ROM from the detail screen with OpenGL render, audio, gamepad input, a pause overlay with save states, and a "Continuar jugando" row.

**Architecture:** A dedicated `EmulatorActivity` hosts LibretroDroid's `GLRetroView` (render/audio/input) with a Compose pause overlay. The `snes9x` core ships as a bundled `.so` in `jniLibs` and is loaded by path. Pure helpers (`SaveStateStore`, `CoreManager`) are unit-tested; the native integration is verified on the ONN. An upfront **integration spike** chooses JitPack vs vendored LibretroDroid and pins the exact API before UI work.

**Tech Stack:** Kotlin, Jetpack Compose for TV, LibretroDroid (`GLRetroView`), `snes9x` libretro core (arm64-v8a), Room, Coroutines. Existing toolchain (JDK 17, Android SDK, Gradle 8.9 wrapper) + possibly NDK/CMake if the vendored fallback is needed.

**Spec:** [docs/superpowers/specs/2026-06-30-gobe-fase2-snes-emulation-design.md](../specs/2026-06-30-gobe-fase2-snes-emulation-design.md)

**Device:** ONN Plus 4K, Android 14, connected over Wi-Fi adb (serial like `192.168.1.219:36637`; confirm with `adb devices`). ROMs at `/storage/emulated/0/Download/ROMs/Snes/*.sfc`.

---

## Conventions for every task

Export the toolchain at the start of any gradle/adb command (do not assume inherited); pass `dangerouslyDisableSandbox: true` on gradle/adb Bash calls:
```
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
export ANDROID_SERIAL="<device serial from 'adb devices'>"
```
Builds: prefer `./gradlew --no-daemon ...` (avoids daemon contention seen in Fase 1). Commit on `feat/fase2-emulacion`; git identity is configured. End commit messages with:
```
Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
```
On-device install: `./gradlew --no-daemon installDebug` (or `adb install -r app/build/outputs/apk/debug/app-debug.apk`). Grant storage for testing: `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow`. Screenshot to verify: `adb exec-out screencap -p > shot.png` (wake first with `adb shell input keyevent KEYCODE_WAKEUP`).

---

## File Structure

```
app/
├─ build.gradle.kts                       (+ LibretroDroid dep OR :libretrodroid module; jniLibs packaging)
├─ src/main/jniLibs/armeabi-v7a/  (ONN is 32-bit — confirmed in the spike; NOT arm64)
│  └─ libsnes9x_libretro_android.so       (bundled core; git-tracked or via a fetch script)
├─ src/main/AndroidManifest.xml           (+ EmulatorActivity; extractNativeLibs=true)
├─ src/main/java/com/gobe/tv/
│  ├─ emulation/
│  │  ├─ CoreManager.kt                   (System -> bundled core .so path)
│  │  ├─ SaveStateStore.kt               (state/SRAM file paths + atomic read/write)
│  │  ├─ EmulatorContract.kt             (Intent extras keys + payload helpers)
│  │  ├─ EmulatorActivity.kt             (hosts GLRetroView + overlay; lifecycle; input)
│  │  ├─ EmulatorViewModel.kt            (pause/save/load state, lastPlayed)
│  │  └─ ui/PauseOverlay.kt              (Compose overlay)
│  ├─ data/LibraryRepository.kt          (+ updateLastPlayed; observeContinuePlaying)
│  ├─ data/db/GameDao.kt                 (+ touchLastPlayed; continue-playing query)
│  └─ ui/
│     ├─ detail/DetailScreen.kt          (JUGAR enabled; Reanudar desde save)
│     └─ home/HomeViewModel.kt/HomeScreen.kt (Continuar jugando row)
└─ src/test/java/com/gobe/tv/emulation/  (SaveStateStore, CoreManager unit tests)
```

---

## Task 1: Integration spike — boot a real SNES ROM via LibretroDroid on the ONN

**This is the gate.** Prove the native stack works and PIN the exact LibretroDroid API/version before building anything else. Throwaway-quality code is OK here; we harden it in later tasks. NOT TDD (device integration).

**Files:**
- Modify: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/jniLibs/armeabi-v7a/  (ONN is 32-bit — confirmed in the spike; NOT arm64)libsnes9x_libretro_android.so`
- Create (temporary): `app/src/main/java/com/gobe/tv/emulation/SpikeEmulatorActivity.kt`

- [ ] **Step 1: Confirm device ABI**

Run: `adb shell getprop ro.product.cpu.abi`
Expected: `arm64-v8a`. If different, fetch the matching core ABI in Step 3.

- [ ] **Step 2: Add LibretroDroid (Approach A — JitPack)**

In `settings.gradle.kts` ensure `maven { url = uri("https://jitpack.io") }` is in `dependencyResolutionManagement.repositories`. In `app/build.gradle.kts` add (resolve the latest tag from https://jitpack.io/#Swordfish90/LibretroDroid):
```kotlin
implementation("com.github.Swordfish90:LibretroDroid:<latest-tag>")
```
Run `./gradlew --no-daemon :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -i libretrodroid` to confirm it resolves and pulls native libs.
**If JitPack fails** (won't resolve, or no arm64 native libs): switch to **Approach B** — see Step 2-Fallback below — and record the choice in the commit.

- [ ] **Step 2-Fallback (only if A fails): vendor LibretroDroid + NDK/CMake**

```
sdkmanager "ndk;26.3.11579264" "cmake;3.22.1"
git submodule add https://github.com/Swordfish90/LibretroDroid.git libretrodroid
```
Add `include(":libretrodroid")` to `settings.gradle.kts` (point to the library module inside the submodule), `implementation(project(":libretrodroid"))` in `app`, and set `ndkVersion`/`externalNativeBuild` per the submodule's README. Build once to confirm the native compile succeeds. Document the exact module path used.

- [ ] **Step 3: Fetch + bundle the snes9x core**

```bash
cd /tmp && curl -L -O https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/snes9x_libretro_android.so.zip
unzip -o snes9x_libretro_android.so.zip
mkdir -p "<repo>/app/src/main/jniLibs/arm64-v8a"
cp snes9x_libretro_android.so "<repo>/app/src/main/jniLibs/armeabi-v7a/  (ONN is 32-bit — confirmed in the spike; NOT arm64)libsnes9x_libretro_android.so"
```
(The `lib` prefix is required for jniLibs packaging.) In `app/build.gradle.kts` `android {}` add:
```kotlin
packaging { jniLibs { useLegacyPackaging = true } }
```
and in `AndroidManifest.xml` `<application ... android:extractNativeLibs="true">` so the core is extracted to `nativeLibraryDir` on install.

**Verify the extraction actually happened** (a merged manifest from the LibretroDroid AAR or a dependency could override `extractNativeLibs`/`useLegacyPackaging`): after install, run
`adb shell run-as com.gobe.tv ls -la $(adb shell pm path com.gobe.tv >/dev/null; echo)` — more simply, log `applicationInfo.nativeLibraryDir` from the app and `adb shell run-as com.gobe.tv ls -la <that dir>` and confirm `libsnes9x_libretro_android.so` is present on disk. If it is NOT extracted, the core path won't resolve — fix the packaging (force `useLegacyPackaging = true`, check the merged manifest) before proceeding.

- [ ] **Step 4: Minimal spike Activity**

Create `SpikeEmulatorActivity` that builds `GLRetroView` with the core path
`applicationInfo.nativeLibraryDir + "/libsnes9x_libretro_android.so"` and a HARDCODED real
ROM path on the device (e.g. one `.sfc` from `adb shell ls "/storage/emulated/0/Download/ROMs/Snes"`),
adds it as the content view, registers it with the lifecycle, and logs the first-frame event.
Use the API for the resolved LibretroDroid version (the exact `GLRetroViewData` field names,
input methods `sendKeyEvent`/`sendMotionEvent`, state methods, and the first-frame/“core ready”
signal differ by version — read the resolved source/AAR and use what it actually exposes).
Temporarily register it as a LAUNCHER or LEANBACK_LAUNCHER activity so `monkey` can start it.

- [ ] **Step 5: Build, install, run on the ONN; confirm render + audio + input**

```
./gradlew --no-daemon installDebug
adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
adb shell monkey -p com.gobe.tv -c android.intent.category.LEANBACK_LAUNCHER 1
```
Wake the screen and screenshot. Expected: the SNES game renders. Plug in the gamepad and
confirm inputs move the game. Confirm audio (subjective; the user can verify). If it boots
from the real `/storage/emulated/0/...` path, the `MANAGE_EXTERNAL_STORAGE` ROM-access model
is validated.

- [ ] **Step 6: PIN the API — write `docs/superpowers/plans/fase2-libretrodroid-api-notes.md`**

Record, from the resolved version: the dependency coordinate/module path chosen (A or B), the
exact `GLRetroViewData` constructor/fields used, how input is forwarded
(`sendKeyEvent`/`sendMotionEvent` signatures + port arg), `serializeState`/`unserializeState`,
the **SRAM save/load method names and the `saves/` directory wiring in `GLRetroViewData`**
(so SRAM persists across quit/relaunch — acceptance item in Task 10), the first-frame/"core
ready" signal, and lifecycle hooks. **Later tasks reference this file.** Commit:
```bash
git add -A && git commit -m "spike(emu): LibretroDroid + snes9x boots a real SNES ROM on the ONN; pin API notes"
```

- [ ] **Step 7: Keep or remove the spike Activity**

Leave `SpikeEmulatorActivity` in place for now (it is replaced by the real `EmulatorActivity`
in Task 6; remove its launcher intent-filter once Task 8 wires the real launch). Note this in
the commit so it isn't forgotten.

---

## Task 2: CoreManager (System → bundled core path)

Pure resolver; TDD.

**Files:** Create `emulation/CoreManager.kt`, `src/test/java/com/gobe/tv/emulation/CoreManagerTest.kt`

- [ ] **Step 1: Failing test**
```kotlin
package com.gobe.tv.emulation

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreManagerTest {
    private val cm = CoreManager(nativeLibDir = "/data/app/x/lib/arm64")

    @Test fun snesResolvesToBundledCore() =
        assertEquals("/data/app/x/lib/arm64/libsnes9x_libretro_android.so", cm.corePath(System.SNES))

    @Test fun unsupportedSystemsAreNullForNow() {
        assertNull(cm.corePath(System.NES))
        assertNull(cm.corePath(System.N64))
        assertNull(cm.corePath(System.ARCADE))
    }
}
```

- [ ] **Step 2: Run → fail.** `./gradlew --no-daemon :app:testDebugUnitTest --tests "com.gobe.tv.emulation.CoreManagerTest"`

- [ ] **Step 3: Implement**
```kotlin
package com.gobe.tv.emulation

import com.gobe.tv.domain.System

/** Resolves the bundled Libretro core .so path for a system. Only SNES is wired in Fase 2. */
class CoreManager(private val nativeLibDir: String) {
    fun corePath(system: System): String? {
        val lib = when (system) {
            System.SNES -> "libsnes9x_libretro_android.so"
            else -> return null
        }
        return "$nativeLibDir/$lib"
    }
}
```

- [ ] **Step 4: Run → pass.**

- [ ] **Step 5: Commit** `feat(emu): CoreManager maps system to bundled core path + tests`

---

## Task 3: SaveStateStore (paths + atomic state IO)

Pure path logic is TDD'd on the JVM with a temp dir; the actual byte IO uses temp-file-then-rename.

**Files:** Create `emulation/SaveStateStore.kt`, `src/test/java/com/gobe/tv/emulation/SaveStateStoreTest.kt`

- [ ] **Step 1: Failing test** (uses `TemporaryFolder` as the `filesDir`)
```kotlin
package com.gobe.tv.emulation

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SaveStateStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun store() = SaveStateStore(tmp.root)

    @Test fun noStateInitially() = assertFalse(store().hasState(42))

    @Test fun writeThenReadRoundTrips() {
        val s = store()
        val bytes = byteArrayOf(1, 2, 3, 4)
        s.writeState(42, bytes)
        assertTrue(s.hasState(42))
        assertArrayEquals(bytes, s.readState(42))
    }

    @Test fun writeIsAtomicNoTmpLeftBehind() {
        val s = store()
        s.writeState(7, byteArrayOf(9))
        val states = java.io.File(tmp.root, "states").list()?.toList() ?: emptyList()
        assertTrue(states.contains("7.state"))
        assertFalse(states.any { it.endsWith(".tmp") })
    }
}
```

- [ ] **Step 2: Run → fail.**

- [ ] **Step 3: Implement**
```kotlin
package com.gobe.tv.emulation

import java.io.File

/** Stores emulator save states under <filesDir>/states/<gameId>.state (atomic writes). */
class SaveStateStore(filesDir: File) {
    private val dir = File(filesDir, "states").apply { mkdirs() }

    private fun stateFile(gameId: Long) = File(dir, "$gameId.state")

    fun hasState(gameId: Long): Boolean = stateFile(gameId).exists()

    fun readState(gameId: Long): ByteArray? =
        stateFile(gameId).takeIf { it.exists() }?.readBytes()

    /** temp-file-then-rename so a crash mid-write never leaves a truncated .state. */
    fun writeState(gameId: Long, bytes: ByteArray) {
        val tmp = File(dir, "$gameId.state.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(stateFile(gameId))) {
            stateFile(gameId).writeBytes(bytes) // fallback
            tmp.delete()
        }
    }
}
```

- [ ] **Step 4: Run → pass.**

- [ ] **Step 5: Commit** `feat(emu): SaveStateStore with atomic state IO + tests`

---

## Task 4: Repository — lastPlayed + "continue playing"

**Files:** Modify `data/db/GameDao.kt`, `data/LibraryRepository.kt`; Test `src/androidTest/.../GameDaoTest.kt` (add cases) — or unit-test the repo mapping where pure.

- [ ] **Step 1: Add DAO queries** in `GameDao.kt`:
```kotlin
@Query("UPDATE games SET lastPlayed = :ts WHERE id = :id")
suspend fun touchLastPlayed(id: Long, ts: Long)

@Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
fun observeContinuePlaying(limit: Int): kotlinx.coroutines.flow.Flow<List<GameEntity>>
```

- [ ] **Step 2: Add repository methods** in `LibraryRepository.kt`:
```kotlin
suspend fun updateLastPlayed(id: Long, now: Long = java.lang.System.currentTimeMillis()) =
    gameDao.touchLastPlayed(id, now)

fun observeContinuePlaying(limit: Int = 12): Flow<List<Game>> =
    gameDao.observeContinuePlaying(limit).map { list -> list.map { it.toDomain() } }
```

- [ ] **Step 3: Instrumented test** — the DAO/ordering assertions need a real SQLite engine, so they run **instrumented** (not JVM unit): add to `GameDaoTest.kt`: insert two games, `touchLastPlayed` on one, assert `observeContinuePlaying(10)` (first emission) contains only it. Run on device:
`./gradlew --no-daemon :app:connectedDebugAndroidTest`
(Only the pure `GameEntity.toDomain()` mapping, if you choose to test it in isolation, is JVM-unit-testable — the queries are instrumented.)

- [ ] **Step 4: Commit** `feat(emu): repo lastPlayed update + continue-playing query`

---

## Task 5: EmulatorContract (Intent payload)

Tiny typed wrapper for launching the emulator. TDD the parse/build round-trip with a fake Intent or a plain data holder.

**Files:** Create `emulation/EmulatorContract.kt`, test `src/test/.../EmulatorContractTest.kt`

- [ ] **Step 1: Failing test** — build args → bundle keys → parse back equal. (Use a plain `EmulatorArgs` data class + extension to/from a `Map<String,Any?>` to keep it JVM-unit-testable; the Activity adapts it to a real Intent.)
```kotlin
package com.gobe.tv.emulation

import com.gobe.tv.domain.System
import org.junit.Assert.assertEquals
import org.junit.Test

class EmulatorContractTest {
    @Test fun roundTrip() {
        val args = EmulatorArgs(gameId = 5, romPath = "/r/x.sfc", system = System.SNES, loadState = true)
        val restored = EmulatorArgs.fromMap(args.toMap())
        assertEquals(args, restored)
    }
}
```

- [ ] **Step 2: Run → fail.**

- [ ] **Step 3: Implement** `EmulatorArgs` with `toMap()/fromMap()` and the Intent-extra key constants; an `Intent` helper (`putEmulatorArgs`/`readEmulatorArgs`) that delegates to the map.

- [ ] **Step 4: Run → pass. Commit** `feat(emu): typed EmulatorArgs contract + tests`

---

## Task 6: EmulatorActivity (real)

Host `GLRetroView`, lifecycle, input forwarding. NOT TDD (device). Uses the API pinned in Task 1's notes file.

**Files:** Create `emulation/EmulatorActivity.kt`; Modify `AndroidManifest.xml` (declare `EmulatorActivity`, landscape, `configChanges`, no launcher filter); remove the spike's launcher filter.

- [ ] **Step 1: Declare the Activity** in the manifest (landscape, `android:configChanges="orientation|screenSize|keyboard|keyboardHidden|navigation"`, exported false).

- [ ] **Step 2: Implement EmulatorActivity** using the pinned API:
  - Read `EmulatorArgs` from the Intent.
  - Resolve core via `CoreManager(applicationInfo.nativeLibraryDir)`; if null → finish with an error toast/log.
  - Build `GLRetroViewData` (core path, ROM path = `args.romPath`, system/saves dirs under `filesDir`); create `GLRetroView`, add to a `FrameLayout`, register with lifecycle.
  - Immersive fullscreen (hide system bars), keep screen on.
  - Forward `dispatchKeyEvent`/`onGenericMotionEvent` to the view's input methods on port 0 — EXCEPT the Back button, which toggles the pause overlay (Task 7).
  - On first-frame/"core ready" signal: call `repository.updateLastPlayed(args.gameId)`; if `args.loadState`, apply the saved state from `SaveStateStore`.
  - `onPause`: pause the core + auto-save state + persist SRAM (use the SRAM method + `saves/` dir pinned in Task 1's notes). `onDestroy`: release the view.
  - Get `repository`/`SaveStateStore` from `application as GobeApp` (extend the ServiceLocator if needed; keep DI manual per project convention).

- [ ] **Step 3: Build + install + on-device check** — temporarily launch via the spike or a hardcoded test Intent; confirm a SNES game boots, gamepad works, screen stays on. (Full launch from detail comes in Task 8.)

- [ ] **Step 4: Commit** `feat(emu): EmulatorActivity hosts GLRetroView with lifecycle + input`

---

## Task 7: EmulatorViewModel + PauseOverlay

**Files:** Create `emulation/EmulatorViewModel.kt`, `emulation/ui/PauseOverlay.kt`; Modify `EmulatorActivity.kt` (host the overlay in a Compose layer over the GL view).

- [ ] **Step 1: EmulatorViewModel** — holds `paused: StateFlow<Boolean>`, `hasState`, and exposes `save()`, `load()`, callbacks wired by the Activity to `GLRetroView.serialize/unserialize` + `SaveStateStore`. Keep emulator-touching calls injected as lambdas so the VM stays testable.

- [ ] **Step 2: PauseOverlay** (Compose, tv-material3, D-pad navigable; first item focused): **Reanudar / Guardar estado / Cargar estado / Salir al menú**. Uses `LocalContentColor` light (consistent with the Fase 1 contrast fix; the Activity's Compose layer must provide it or wrap in `GobeTheme`).

- [ ] **Step 3: Wire Back** in `EmulatorActivity`: Back toggles `paused`; while paused, pause the core (frame speed 0 / `GLRetroView` paused) and show the overlay; Resume hides it and resumes; Salir auto-saves and `finish()`es.

- [ ] **Step 4: Build + on-device check** — Back opens overlay, emulation pauses, Save then Load restores, Salir returns to the launching screen. Screenshot the overlay.

- [ ] **Step 5: Commit** `feat(emu): pause overlay with save/load/exit + emulator viewmodel`

---

## Task 8: Wire detail screen → launch

**Files:** Modify `ui/detail/DetailScreen.kt`; remove the spike Activity + its launcher filter.

- [ ] **Step 1: Enable JUGAR** — the previously-disabled play button now starts `EmulatorActivity` with `EmulatorArgs(gameId, romPath = game.path, system = game.system, loadState = false)`. Only enable for `System.SNES` this milestone (other systems keep "próximamente" since `CoreManager` returns null).

- [ ] **Step 2: "Reanudar desde save"** — show this button when `SaveStateStore(filesDir).hasState(gameId)`; it launches with `loadState = true`. Get `filesDir`/`SaveStateStore` via context/ServiceLocator.

- [ ] **Step 3: Remove the spike** — delete `SpikeEmulatorActivity.kt` and its manifest entry.

- [ ] **Step 4: Build + on-device check** — from the SNES row → detail → JUGAR boots the game; exit; "Reanudar desde save" appears and resumes. Confirm `lastPlayed` updates (next task surfaces it).

- [ ] **Step 5: Commit** `feat(emu): launch SNES from detail (Jugar + Reanudar desde save)`

---

## Task 9: Home "Continuar jugando" row

**Files:** Modify `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`.

- [ ] **Step 1: HomeViewModel** — combine `observeContinuePlaying()` into `HomeState` as a leading optional row (only when non-empty). Keep the existing scanning/loading logic.

- [ ] **Step 2: HomeScreen** — render a "Continuar jugando" row first (when present) above the per-system rows, reusing `GameTile`; tapping a tile opens its detail (same as other tiles). Initial focus goes to the first tile of this row when present, else the first system row.

- [ ] **Step 3: Build + on-device check** — after playing a game, return to Home: "Continuar jugando" shows it first; selecting it reopens the detail/launch.

- [ ] **Step 4: Commit** `feat(home): Continuar jugando row from lastPlayed`

---

## Task 10: On-device acceptance + docs

- [ ] **Step 1: Full unit + instrumented suite** — `./gradlew --no-daemon :app:testDebugUnitTest` and `:app:connectedDebugAndroidTest` → green.

- [ ] **Step 2: Manual checklist on the ONN (record pass/fail)**
  - [ ] A real SNES ROM boots and renders with audio.
  - [ ] Gamepad (P1) controls the game.
  - [ ] Back opens the pause overlay; emulation pauses; D-pad navigates it.
  - [ ] Guardar estado then Cargar estado restores the prior moment.
  - [ ] Salir auto-saves and returns to the detail/Home.
  - [ ] "Reanudar desde save" on the detail resumes from the saved state.
  - [ ] SRAM persists across a full quit/relaunch (a battery-save SNES game).
  - [ ] "Continuar jugando" row appears and launches the last game.
  - [ ] No crash on missing core/unreadable ROM (shows error, returns).

- [ ] **Step 3: Write RESULTS** `docs/superpowers/plans/2026-06-30-gobe-fase2-snes-emulation-RESULTS.md` (outcome, A-vs-B chosen, LibretroDroid version/API, deviations, screenshots, follow-ups). Commit.

- [ ] **Step 4: Finish the branch** — use superpowers:finishing-a-development-branch.

---

## Notes for the implementer

- **Task 1 is the gate** — do not build Tasks 6–9 until the spike renders a real SNES ROM and the API notes file exists. Everything emulator-facing references those pinned signatures.
- **Core licensing:** `snes9x` + LibretroDroid are GPL-3.0; fine for personal use (parent spec §6.1). If the core `.so` is committed to git, note its source/license in the commit.
- **Do not** call `serialize/unserialize` before the core-ready signal (racy).
- Reference skills: @superpowers:test-driven-development (Tasks 2,3,4,5), @superpowers:systematic-debugging (Task 1 device issues), @superpowers:verification-before-completion (Task 10).
- Keep DI manual/minimal (extend `GobeApp` ServiceLocator with `SaveStateStore`/`CoreManager` if convenient) — consistent with Fase 1.
```

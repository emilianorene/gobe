# Gobe Arcade (MAME/FBNeo) Support — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the user's arcade `.zip` games playable by bundling a MAME Libretro core, wiring `System.ARCADE` through the existing Fase 2 emulation stack, and adding non-crashing error UX for romset/BIOS mismatches.

**Architecture:** Reuse the Fase 2 stack unchanged (`EmulatorActivity` + `GLRetroView`, `CoreManager`, jniLibs core packaging, save states, Select+Start menu). This sub-project (a) **spikes** the right core/romset on-device against the user's real `.zip`, (b) bundles that core for `armeabi-v7a`, (c) maps `ARCADE → core` in `CoreManager`, (d) broadens the detail-screen "Jugar" gate to "a core exists for this system", (e) moves the emulator `systemDirectory` to a user-accessible external folder so the user can drop BIOS zips there, and (f) subscribes to LibretroDroid's error stream so a bad romset/missing BIOS shows a message and returns to the library instead of crashing.

**Tech Stack:** Kotlin, Jetpack Compose for TV, LibretroDroid 0.14.0 (`GLRetroView`, `GLRetroViewData`, `getGLRetroErrors()`), a MAME Libretro core (`mame2003_plus` candidate, `armeabi-v7a`), Room, JUnit (JVM unit tests) + on-device verification over wireless adb.

**Spec:** `docs/superpowers/specs/2026-06-30-gobe-arcade-design.md`

---

## Pre-flight (controller, before Task 1)

- Branch off `main`: the previous fases used `feat/<name>` branches. Create a worktree/branch `feat/arcade` (see superpowers:using-git-worktrees) so work is isolated and `main` stays releasable.
- The app is currently installed on the ONN (build of `main`, `MANAGE_EXTERNAL_STORAGE` granted) for the user to test. Re-grant the permission after any `connectedAndroidTest` run (it uninstalls/reinstalls): `adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow`.
- The device is **32-bit `armeabi-v7a`** — every native core MUST be this ABI. Verify any downloaded `.so` with `file <core>.so` → must say `ARM, EABI5` / `32-bit`, never `aarch64`.

## File map (what each task touches)

- `app/src/main/jniLibs/armeabi-v7a/lib<core>_libretro_android.so` — **Create**: the bundled arcade core (exact `<core>` pinned by the spike). Sits beside `libsnes9x_libretro_android.so`.
- `app/src/main/java/com/gobe/tv/emulation/CoreManager.kt` — **Modify**: add `System.ARCADE → "lib<core>_libretro_android.so"`.
- `app/src/test/java/com/gobe/tv/emulation/CoreManagerTest.kt` — **Modify**: ARCADE now resolves; only NES/N64 stay null.
- `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt` — **Modify**: external `systemDirectory` (created if missing); subscribe to `getGLRetroErrors()` to show a message + finish.
- `app/src/main/res/values/strings.xml` + `app/src/main/res/values-es/strings.xml` — **Modify**: add the load-error string (EN + ES).
- `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt:47` — **Modify**: `playable = CoreManager(...).corePath(g.system) != null`.
- `docs/superpowers/plans/2026-06-30-gobe-arcade-RESULTS.md` — **Create** (final task): resolved core, romset version, BIOS folder path, error-signal notes.

---

### Task 1: SPIKE — pin the arcade core, romset version, and load-error signal (on-device, NOT TDD)

This task is **exploratory**, runs on the real device against the user's real ROMs, and produces *findings + a committed core*, not unit tests. Do not write app code beyond what's needed to launch a candidate core. Everything downstream depends on its outputs.

**Outputs to pin (record verbatim for the RESULTS doc):**
1. The exact core filename `lib<core>_libretro_android.so` that boots a user game.
2. The MAME **romset version** the user's `.zip` files match (e.g. "MAME 0.78 / mame2003-plus set").
3. The concrete **load-failure signal** from LibretroDroid (confirm `getGLRetroErrors()` emits on a bad romset; note the emitted value/shape).
4. Which user games boot, which don't, and which need BIOS.

- [ ] **Step 1: Inventory the user's arcade ROMs on the device**

Run:
```bash
adb shell ls -la /storage/emulated/0/Download/ROMs/arcade/ 2>/dev/null || \
adb shell 'find /storage/emulated/0 -iname "*.zip" -path "*rcade*" 2>/dev/null'
```
Expected: a list of `.zip` files (contra, dino, ffight, ga2, mk2, mk3, …). Note 2-3 names to test (prefer a CPS1/early title like `ffight` and one likely-Neo-Geo title to surface the BIOS path). If the path differs, use the actual path the user stores ROMs under.

- [ ] **Step 2: Get the `mame2003_plus` core for `armeabi-v7a`**

Download from the libretro buildbot (Android, `armeabi-v7a`):
```bash
cd /private/tmp/claude-501/.../scratchpad
curl -L -o mame2003_plus.so.zip \
  "https://buildbot.libretro.com/nightly/android/latest/armeabi-v7a/mame2003_plus_libretro_android.so.zip"
unzip -o mame2003_plus.so.zip
file mame2003_plus_libretro_android.so
```
Expected: `... ARM, EABI5 ... 32-bit ...` (NOT `aarch64`). If 64-bit or 404, try the exact buildbot filename for the core, or `mame2010`/`fbneo` as the spec's fallbacks.

- [ ] **Step 3: Temporarily wire the candidate core so a game can launch**

Place the `.so` in jniLibs and add a temporary ARCADE mapping just to boot the spike (this becomes permanent in Tasks 2-3 once pinned):
```bash
cp mame2003_plus_libretro_android.so \
  "app/src/main/jniLibs/armeabi-v7a/libmame2003_plus_libretro_android.so"
```
Temporarily edit `CoreManager.corePath` to map `System.ARCADE -> "libmame2003_plus_libretro_android.so"`, and temporarily set `DetailScreen` `playable` to also allow ARCADE, so a game can be launched. (These are throwaway edits to enable the spike; Tasks 2-5 implement them properly with tests.)

- [ ] **Step 4: Build, install, and launch a user arcade game on the ONN**

```bash
./gradlew :app:installDebug
adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
adb shell monkey -p com.gobe.tv -c android.intent.category.LAUNCHER 1
```
Navigate to an arcade game → detail → Jugar. Capture the screen:
```bash
adb exec-out screencap -p > /private/tmp/.../scratchpad/spike-arcade.png
```
Expected (success): the game renders. Expected (failure): observe the failure mode (black screen / immediate finish / log error) — that's data for Step 6.

- [ ] **Step 5: If it fails, iterate the core/version**

Per spec §2/§9, romset/core-version mismatch is the dominant risk. If the candidate doesn't boot the user's set, try, in order: another MAME version (`mame2010` ≈ 0.139), then `fbneo`. Repeat Steps 2-4 with each until a user game boots. Record which core + which games boot. If multiple boot under one core, pin that core.

- [ ] **Step 6: Confirm the load-error signal via `getGLRetroErrors()`**

With the pinned core, launch a deliberately-bad romset (e.g. a game whose set doesn't match, or a renamed/empty `.zip`). While it loads, watch logs and confirm LibretroDroid surfaces the failure through its error stream:
```bash
adb logcat -c && adb logcat | grep -iE "libretro|GLRetro|core|rom" &
```
In code (temporary, for the spike only) add `lifecycleScope.launch { retroView.getGLRetroErrors().collect { ... Log.e } }` and confirm it emits on the failure. **Record the emitted value/type** — Task 4 relies on this being the real hook. If `getGLRetroErrors()` does NOT emit for romset failures, record the actual observable signal (e.g. no `FrameRendered` within N seconds) so Task 4 uses a working fallback.

- [ ] **Step 7: Commit the pinned core + spike findings**

Keep the bundled `.so` (it's the real deliverable); revert the throwaway `CoreManager`/`DetailScreen` spike edits so Tasks 2-5 add them cleanly with tests. Write the spike findings into a scratch note for the RESULTS doc.
```bash
git add app/src/main/jniLibs/armeabi-v7a/lib<core>_libretro_android.so
git commit -m "feat(arcade): bundle <core> core (armeabi-v7a) — pinned by on-device spike"
```
Record for RESULTS: pinned `<core>`, romset version, BIOS-needing games, and the confirmed error signal.

**Controller note:** Task 1 is on-device and may need user help (ROM paths, gamepad). If a candidate needs a BIOS to even boot (Neo Geo), have the user drop the BIOS in the folder Task 3 establishes, or pick a non-BIOS game for the spike. Surface to the human if no core boots any user game.

---

### Task 2: `CoreManager` maps ARCADE to the bundled core (TDD)

**REQUIRED SUB-SKILL:** superpowers:test-driven-development

**Files:**
- Modify: `app/src/test/java/com/gobe/tv/emulation/CoreManagerTest.kt`
- Modify: `app/src/main/java/com/gobe/tv/emulation/CoreManager.kt`

Use the exact `<core>` filename pinned in Task 1 (placeholder below: `mame2003_plus`).

- [ ] **Step 1: Update the failing test**

In `CoreManagerTest.kt`, add an ARCADE-resolves assertion and remove ARCADE from the null list:
```kotlin
    @Test fun arcadeResolvesToBundledCore() =
        assertEquals("/data/app/x/lib/arm/libmame2003_plus_libretro_android.so", cm.corePath(System.ARCADE))

    @Test fun unsupportedSystemsAreNullForNow() {
        assertNull(cm.corePath(System.NES))
        assertNull(cm.corePath(System.N64))
    }
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.CoreManagerTest"`
Expected: FAIL — `arcadeResolvesToBundledCore` expects the arcade path but gets `null`.

- [ ] **Step 3: Add the ARCADE mapping**

In `CoreManager.kt`, extend the `when`:
```kotlin
        val lib = when (system) {
            System.SNES -> "libsnes9x_libretro_android.so"
            System.ARCADE -> "libmame2003_plus_libretro_android.so"
            else -> return null
        }
```
Update the class KDoc: "Resolves the bundled Libretro core .so path for a system. SNES (snes9x) and Arcade (mame2003_plus) are wired; NES/N64 not yet."

- [ ] **Step 4: Run the test, verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gobe.tv.emulation.CoreManagerTest"`
Expected: PASS (all 3 tests green).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/CoreManager.kt app/src/test/java/com/gobe/tv/emulation/CoreManagerTest.kt
git commit -m "feat(arcade): CoreManager maps ARCADE to bundled MAME core"
```

---

### Task 3: External, user-accessible `systemDirectory` for BIOS (applies to all systems)

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt:74-80`

Per spec §5: move only the BIOS/`systemDirectory` out of app-private `filesDir` to a user-accessible external folder (`/storage/emulated/0/Download/ROMs/system`), created if missing. `savesDirectory` and save-state files stay under `filesDir`. The app already holds `MANAGE_EXTERNAL_STORAGE`. Harmless for SNES (no BIOS).

This is a small, mechanical change with no JVM-unit-testable surface (it touches Android `Environment`/filesystem), so it is verified on-device in Task 6, not by a unit test.

- [ ] **Step 1: Add a helper that resolves & creates the external system dir**

In `EmulatorActivity.kt`, near `savesDir` (line ~46), add:
```kotlin
    /** User-accessible folder where the user drops arcade BIOS zips (e.g. neogeo.zip). Created if missing. */
    private val systemDir: File
        get() = File(android.os.Environment.getExternalStorageDirectory(), "Download/ROMs/system").apply { mkdirs() }
```

- [ ] **Step 2: Point `GLRetroViewData.systemDirectory` at it**

Change line 77 from `systemDirectory = filesDir.absolutePath` to:
```kotlin
            systemDirectory = systemDir.absolutePath
```
Leave `savesDirectory = savesDir.absolutePath` unchanged.

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt
git commit -m "feat(arcade): point emulator systemDirectory at user-accessible Download/ROMs/system for BIOS"
```

---

### Task 4: Non-crashing error UX via the LibretroDroid error stream

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`
- Modify: `app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt`

Use the **exact error signal confirmed in Task 1 Step 6**. The default below assumes `getGLRetroErrors()` emits on load failure; if Task 1 found a different signal, implement that instead (note the deviation in RESULTS).

- [ ] **Step 1: Add the load-error string (EN + ES)**

`values/strings.xml` (beside the other `emu_` strings, ~line 55):
```xml
    <string name="emu_load_failed">Couldn\'t load this game. The romset may not match the core, or a BIOS is missing in Download/ROMs/system.</string>
```
`values-es/strings.xml` (matching key):
```xml
    <string name="emu_load_failed">No se pudo cargar este juego. El romset puede no coincidir con el core, o falta un BIOS en Download/ROMs/system.</string>
```

- [ ] **Step 2: Subscribe to the error stream and finish gracefully**

First add a one-shot flag next to `coreReadyHandled` (line 44), mirroring the existing convention:
```kotlin
    private var loadErrorHandled = false
```
Then in `EmulatorActivity.onCreate`, alongside the existing `getGLRetroEvents()` collector (lines 112-119), add a collector for errors:
```kotlin
        lifecycleScope.launch {
            view.getGLRetroErrors().collect {
                if (!loadErrorHandled && !isFinishing) {
                    loadErrorHandled = true
                    Toast.makeText(this@EmulatorActivity, getString(R.string.emu_load_failed), Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
```
(Imports already present: `Toast`, `lifecycleScope`, `launch`, `R`. If `getGLRetroErrors()` returns an Rx type rather than a Flow in 0.14.0, adapt per what Task 1 observed — e.g. `.asFlow()` or the documented subscribe API.)

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gobe/tv/emulation/EmulatorActivity.kt app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml
git commit -m "feat(arcade): show error + return to library when a romset/BIOS fails to load"
```

---

### Task 5: Enable "Jugar" whenever a core exists for the system

**Files:**
- Modify: `app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt:47`

Per spec §5: broaden `playable` from `system == SNES` to "a core is bundled for this system". This enables ARCADE and keeps SNES unchanged; NES/N64 stay "próximamente" until their cores ship.

- [ ] **Step 1: Replace the `playable` gate**

`DetailScreen.kt` line 47, change:
```kotlin
    val playable = g?.system == System.SNES
```
to:
```kotlin
    val playable = g != null &&
        com.gobe.tv.emulation.CoreManager(context.applicationInfo.nativeLibraryDir).corePath(g.system) != null
```
(`context` is already in scope from `LocalContext.current` at line 40. This reuses the single source of truth — `CoreManager` — so detail and `EmulatorActivity` can never disagree about what's playable.)

- [ ] **Step 2: Build to verify it compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/gobe/tv/ui/detail/DetailScreen.kt
git commit -m "feat(arcade): enable Play when a core exists for the game's system"
```

---

### Task 6: On-device acceptance

Reinstall a clean build and verify the full arcade flow on the ONN. Re-grant storage permission after install.

- [ ] **Step 1: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (CoreManager arcade test green).

- [ ] **Step 2: Install on the ONN and grant permission**

```bash
./gradlew :app:installDebug
adb shell appops set com.gobe.tv MANAGE_EXTERNAL_STORAGE allow
adb shell monkey -p com.gobe.tv -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 2b: Confirm the BIOS folder exists for the user**

```bash
adb shell ls -la /storage/emulated/0/Download/ROMs/system/
```
Expected: the folder exists (created on first launch of an arcade game). Tell the user this is where BIOS zips go.

- [ ] **Step 3: Acceptance checklist (capture a screenshot for each, save to scratchpad)**

  - Arcade game appears in the grid → opens detail → **Jugar is enabled** (not "próximamente").
  - Launch a known-good arcade game (from the Task 1 spike) → it **renders with audio**.
  - **Gamepad** controls move/act in-game.
  - **Select+Start** opens the pause menu; **Exit to Gobe** returns to the grid.
  - A **deliberately-bad romset** shows the `emu_load_failed` message and returns to the grid (no crash).
  - SNES still launches (regression: the broadened `playable` + external `systemDirectory` didn't break it).

```bash
adb exec-out screencap -p > /private/tmp/.../scratchpad/arcade-accept-<n>.png
```

- [ ] **Step 4: If any check fails**

Triage: wrong core → revisit Task 1 (pin a different core/version); BIOS missing → confirm the file is in `Download/ROMs/system`; error path crashes instead of toasting → revisit Task 4's signal. Re-run from the failed task; don't paper over a crash.

---

### Task 7: RESULTS doc, finish & merge

**Files:**
- Create: `docs/superpowers/plans/2026-06-30-gobe-arcade-RESULTS.md`

- [ ] **Step 1: Write RESULTS**

Document: the pinned `<core>` + why; the user's **romset version**; the **BIOS folder** (`/storage/emulated/0/Download/ROMs/system`) and which games need BIOS; the **error signal** used (and any deviation from `getGLRetroErrors()`); which user games boot vs. don't; known performance limits (CPS3/late-MAME on the ONN); and any follow-ups.

- [ ] **Step 2: Commit RESULTS**

```bash
git add docs/superpowers/plans/2026-06-30-gobe-arcade-RESULTS.md
git commit -m "docs(arcade): RESULTS — pinned core, romset version, BIOS folder, error signal"
```

- [ ] **Step 3: Finish the branch**

**REQUIRED SUB-SKILL:** superpowers:finishing-a-development-branch — verify tests pass, then merge `feat/arcade` → `main` and push (the established pattern for this project). Re-grant `MANAGE_EXTERNAL_STORAGE` after the final install if instrumented tests ran.

---

## Notes for the implementer

- **ABI is non-negotiable:** every core `.so` must be `armeabi-v7a` (32-bit). Verify with `file`.
- **Single source of truth:** `CoreManager` decides what's playable; `DetailScreen` and `EmulatorActivity` both consult it (Tasks 2 & 5) so they can't disagree.
- **Don't fix romsets:** the core decides compatibility; we surface failures (Task 4), we don't convert sets (out of scope, spec §4).
- **No BIOS bundling:** the user provides BIOS in the external folder; never commit or distribute BIOS.
- **Saves stay private:** only `systemDirectory` moves external; `savesDirectory`/save-state files remain under `filesDir`.

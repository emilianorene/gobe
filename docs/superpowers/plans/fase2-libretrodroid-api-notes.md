# LibretroDroid API notes (pinned by the Fase 2 spike)

> These are the integration facts later tasks must use. Sourced from LibretroDroid **0.14.0**.

## Integration approach: A (JitPack)

- `settings.gradle.kts` repositories include `maven { url = uri("https://jitpack.io") }`.
- `app/build.gradle.kts`: `implementation("com.github.Swordfish90:LibretroDroid:0.14.0")`
- JitPack reports build "ok" for 0.14.0; it provides `GLRetroView` + native libs (arm64-v8a).
- Fallback B (vendored NDK/CMake) NOT needed unless the on-device load fails.

## GLRetroView

```kotlin
class GLRetroView(context: Context, data: GLRetroViewData) : GLSurfaceView, LifecycleObserver
```
- **Lifecycle:** register with `someLifecycle.addObserver(glRetroView)`. It creates the core on
  `ON_CREATE` (`LibretroDroid.create(... data.coreFilePath, data.systemDirectory,
  data.savesDirectory ...)`) and tears down on `ON_DESTROY`. The **game is loaded from
  `data.gameFilePath`** (no separate loadGame call needed).
- It is a `GLSurfaceView` — add it as the content view or inside a `FrameLayout` (so a Compose
  overlay can sit on top). `keepScreenOn` is set internally; `preserveEGLContextOnPause = true`.
- **Pause/resume emulation:** `glRetroView.frameSpeed = 0` to pause logic, `= 1` to resume;
  `glRetroView.audioEnabled = false/true` to mute/unmute. (The GL thread itself follows the
  Activity via the lifecycle observer.)

## GLRetroViewData (all fields mutable; construct with `GLRetroViewData(context)`)

- `coreFilePath: String?` — absolute path to the core `.so`.
- `gameFilePath: String?` — absolute path to the ROM.
- `systemDirectory: String` (default `filesDir`), `savesDirectory: String` (default `filesDir`).
- `saveRAMState: ByteArray?` — set this to **load SRAM at boot** (battery saves).
- `shader: ShaderConfig` (default `ShaderConfig.Default`), `viewportAlignment`,
  `rumbleEventsEnabled`, `preferLowLatencyAudio`.

## Input (Player 1 = port 0)

```kotlin
fun sendKeyEvent(action: Int, keyCode: Int, port: Int = 0)   // action = KeyEvent.ACTION_DOWN/UP
fun sendMotionEvent(source: Int, xAxis: Float, yAxis: Float, port: Int = 0)
// sources: GLRetroView.MOTION_SOURCE_DPAD / MOTION_SOURCE_ANALOG_LEFT / MOTION_SOURCE_ANALOG_RIGHT
```
- For a standard Android gamepad, forward from the Activity:
  - `dispatchKeyEvent(e)` → `glRetroView.sendKeyEvent(e.action, e.keyCode, 0)` (return true if consumed).
    EXCEPT `KEYCODE_BACK` → toggle the pause overlay instead.
  - `onGenericMotionEvent(e)` → read axes (`MotionEvent.AXIS_HAT_X/Y` for dpad,
    `AXIS_X/Y` left stick, `AXIS_Z/RZ` right stick) → `sendMotionEvent(...)`.
- The libretro core maps RetroPad buttons; standard Android gamepad keycodes (BUTTON_A/B/X/Y/L1/R1,
  DPAD_*, BUTTON_START/SELECT) are understood by LibretroDroid's default handling.

## Save states + SRAM

```kotlin
fun serializeState(useEmulationThread: Boolean = true): ByteArray
fun unserializeState(data: ByteArray, useEmulationThread: Boolean = true): Boolean
fun serializeSRAM(useEmulationThread: Boolean = true): ByteArray
fun unserializeSRAM(data: ByteArray, useEmulationThread: Boolean = true): Boolean
```
- **Save state:** `serializeState()` → write to `filesDir/states/<gameId>.state` (atomic).
- **Load state:** read file → `unserializeState(bytes)`. Only AFTER core ready (see below).
- **SRAM persist:** on exit `serializeSRAM()` → write `filesDir/saves/<gameId>.srm` (atomic);
  on next launch set `data.saveRAMState = <those bytes>` so the core boots with the battery save.
  (`savesDirectory` is also given so the core can manage its own save files.)

## Core-ready / first-frame signal

```kotlin
fun getGLRetroEvents(): Flow<GLRetroEvents>
sealed class GLRetroEvents { object FrameRendered; object SurfaceCreated }
```
- Treat the **first `FrameRendered`** as "core ready". Do `updateLastPlayed` and apply a
  load-state there. Calling `serialize/unserialize` before this is racy — do not.

## Core packaging (snes9x)

- **ABI `armeabi-v7a` (32-bit).** IMPORTANT: the ONN Plus 4K runs **32-bit Android**
  (`ro.product.cpu.abi = armeabi-v7a`, `abilist = armeabi-v7a,armeabi`) — there is **no
  arm64** on this device. The spike initially shipped an arm64 core and it failed to load
  (`nativeLibraryDir` was `.../lib/arm`). Use the **armeabi-v7a** core only.
- Core from libretro buildbot (`armeabi-v7a/snes9x_libretro_android.so.zip`), renamed
  `libsnes9x_libretro_android.so`, placed in `app/src/main/jniLibs/armeabi-v7a/`.
- ROMs are often nested (e.g. `Snes/<Collection>/<game>.sfc`) — find them recursively.
- `android { packaging { jniLibs { useLegacyPackaging = true } } }` + manifest
  `android:extractNativeLibs="true"` so it lands in `applicationInfo.nativeLibraryDir`.
- Verify post-install: `adb shell run-as com.gobe.tv ls -la <nativeLibraryDir>` shows the `.so`.
- Load path: `applicationInfo.nativeLibraryDir + "/libsnes9x_libretro_android.so"`.

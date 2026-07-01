# Gobe — Controllers: Detection + Test (sub-project A)

> Date: 2026-07-01
> Scope: A "Controllers" screen that lists connected USB/Bluetooth gamepads and a live test that
> highlights buttons/sticks as they're pressed.
> Part of a larger controller-config effort. **Sub-projects (separate cycles):**
> **A) detection + test (this spec)** → B) player/port assignment → C) button remapping.
> Suggested extra settings for later: A/B swap, rumble toggle, analog deadzone, menu-hotkey config.

## 1. Purpose

Give the user a way to (1) see which game controllers Android has detected (USB or Bluetooth) and
(2) verify each button/stick actually registers — a "does my controller work?" tool. Today all
emulator input is hardcoded to port 0 with no visibility into what's connected. This screen is the
foundation the later port-assignment (B) and remapping (C) sub-projects build on.

## 2. Decisions captured during brainstorming

- **Dedicated `ControllersActivity`** launched from Settings (not a Compose nav route), because it
  must capture ALL input (keys + motion) independent of Compose focus — mirroring how
  `EmulatorActivity` owns input.
- **Live list** via `InputManager` + `InputDeviceListener` (updates on connect/disconnect).
- **Test UI = a labeled button panel that highlights** on press (not a drawn gamepad diagram) +
  live stick positions and a last-event readout.
- **No persistence** in A (no port assignment, no remapping — those are B/C).

## 3. In scope

- A "Controllers" entry in `SettingsScreen` that starts `ControllersActivity`.
- `ControllersActivity`: enumerates connected controllers, live-updates on hotplug, and renders the
  detection list + the test panel; overrides `dispatchKeyEvent` / `onGenericMotionEvent` to drive
  the highlight state.
- A pure `keyCodeToPadButton(keyCode: Int): PadButton?` mapper (Android gamepad keycode → a
  `PadButton` enum: A, B, X, Y, L1, R1, L2, R2, L3, R3, SELECT, START, DPAD_UP/DOWN/LEFT/RIGHT),
  unit-tested.
- Strings (EN/ES) for the screen (title, empty state, labels, "last input", etc.).

## 4. Out of scope (deferred to B/C or later)

- Assigning controllers to players/ports (sub-project B).
- Button remapping / capture (sub-project C).
- Rumble/deadzone/A-B-swap/menu-hotkey settings (later).
- Distinguishing USB vs Bluetooth transport (Android doesn't expose it reliably; show the name).
- Persisting anything.

## 5. Architecture

- **`PadButton` enum + `keyCodeToPadButton`** (new, e.g. `emulation/input/PadButton.kt`, Compose-free
  so it's JVM-unit-testable): maps `KeyEvent.KEYCODE_BUTTON_A → A`, `..._B → B`, `..._X → X`,
  `..._Y → Y`, `..._L1 → L1`, `..._R1 → R1`, `..._L2 → L2`, `..._R2 → R2`, `..._THUMBL → L3`,
  `..._THUMBR → R3`, `..._SELECT → SELECT`, `..._START → START`, `KEYCODE_DPAD_* → DPAD_*`, else null.
- **`ControllerDevice`** data (name, deviceId, vendorId, productId) — a small model built from
  `InputDevice`.
- **Device enumeration** (`ControllerLister` helper or inline in the Activity): iterate
  `InputDevice.getDeviceIds()`, keep those whose `sources` include `SOURCE_GAMEPAD` or
  `SOURCE_JOYSTICK`; map to `ControllerDevice`. Skip the TV remote (no gamepad source).
- **`ControllersActivity`** (`ComponentActivity`, `attachBaseContext` locale wrap like the others):
  - Holds Compose state: `devices: List<ControllerDevice>`, `activeButtons: Set<PadButton>`,
    `leftStick`/`rightStick` positions, `lastEvent` (device name + button/keycode).
  - Registers an `InputManager.InputDeviceListener` in `onResume` (unregister in `onPause`) to
    refresh `devices`.
  - `override dispatchKeyEvent`: for gamepad key codes, add/remove the mapped `PadButton` from
    `activeButtons` on DOWN/UP and set `lastEvent`; consume gamepad keys so they don't leak. Let
    Back propagate (to leave the screen).
  - `override onGenericMotionEvent`: read `AXIS_HAT_X/Y` (D-pad) → toggle DPAD_* highlights, and
    `AXIS_X/Y` (left) / `AXIS_Z/RZ` (right) → stick positions.
  - Renders `ControllersScreen(devices, activeButtons, leftStick, rightStick, lastEvent, onBack)`.
- **`ControllersScreen`** (Compose): the device list (or empty state) + a `PadButton` panel where
  each label highlights when in `activeButtons`, two stick readouts, and the last-event line; a Back
  button.
- **Launch**: `SettingsScreen` gets a "Controllers" button → `startActivity(Intent(context, ControllersActivity::class.java))`.

## 6. Testing

- **Unit (JVM):** `keyCodeToPadButton` returns the right `PadButton` for representative keycodes
  (BUTTON_A→A, BUTTON_START→START, DPAD_UP→DPAD_UP) and null for a non-gamepad key (e.g. KEYCODE_A).
- **On-device (ONN):** connect a gamepad → it appears in the list (and disappears on disconnect);
  open the test → pressing each face/shoulder button highlights its label, the D-pad highlights,
  and moving the sticks updates the readouts; the device name shows for the last input; Back returns
  to Settings. (If no controller is handy, the ONN's own input still exercises D-pad/Back.)

## 7. Risks

1. **Compose can't easily see motion events** → handled by overriding the Activity's
   `onGenericMotionEvent` and pushing to Compose state (the established `EmulatorActivity` pattern).
2. **Keycode variety across controllers** (some report `BUTTON_C/Z`, different A/B labels) → the
   mapper covers the standard set; unknown codes show in the raw last-event readout so nothing is
   invisible. Extending the map is cheap.
3. **The TV remote is not a gamepad** → excluded by the `SOURCE_GAMEPAD/JOYSTICK` filter; its D-pad
   still works for navigating the screen.
4. **Consuming keys** in the test could trap the user → only gamepad button/motion are consumed;
   Back always propagates so the user can leave.

## 8. Defaults (change on request)

- Dedicated `ControllersActivity` from Settings; live `InputManager` list; labeled highlight panel +
  stick readouts + last-event line; no persistence. All tunable on-device.

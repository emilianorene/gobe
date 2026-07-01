# Gobe — Controllers: List, Select & Player Assignment (sub-project B)

> Date: 2026-07-01
> Scope: Restructure the Controllers screen into a list → select flow, let the user assign each
> controller to a player (P1–P4), persist it, and route emulator input to the assigned port.
> Part of the controller-config effort. A) detection+test **(done)** → **B) this** → C) remapping.

## 1. Purpose

Two connected needs:
1. **See the list of detected controllers and select one** *before* testing (user request) — the
   current single screen mixes list + test.
2. **Choose which controller is Player 1/2/3/4** and have the emulator actually route each
   controller's input to its port (today all input goes to port 0).

## 2. Decisions captured during brainstorming

- **Two-level UI**: a controllers **list** (rows = detected controllers, showing assigned player) →
  select a row → a **controller detail** (assign player P1–P4 + the test panel, scoped to that
  controller).
- **Persist by `InputDevice.getDescriptor()`** (stable across reconnects) in SharedPreferences
  (same mechanism as the language setting: `gobe.settings`).
- **Unique ports**: assigning a controller to a port unassigns whoever held it (no accidental
  duplicates).
- **Default routing**: an unassigned controller falls back to **P1 (port 0)** so single-player
  keeps working.
- **P1–P4** supported (the core uses what it supports).

## 3. In scope

- **`ControllerAssignments`** (pure, unit-tested): a descriptor→port map with `assign(descriptor,
  port)` enforcing unique ports, `portFor(descriptor): Int?`, and a snapshot for display. Backed by
  a tiny SharedPreferences store (`ControllerPrefs`) — the pure logic is separated from the Android
  prefs so it's JVM-testable.
- **`portForDevice(descriptor, assignments): Int`** — returns the assigned port or **0** (P1)
  default. Pure, unit-tested.
- **Controllers list screen**: rows of connected controllers (name + "P1"/…/"—"); empty state;
  selecting a row opens the detail.
- **Controller detail**: the controller's name, a **player selector** (P1–P4) that writes the
  assignment, and the **test panel** (reused from A) filtered to that controller's `deviceId`.
- **`EmulatorActivity` routing**: replace the hardcoded `0` port in `sendKeyEvent`/`sendMotionEvent`
  with `portForDevice(descriptorOf(event.deviceId), assignments)`. Load assignments once in
  `onCreate`.

## 4. Out of scope (deferred)

- Button remapping (sub-project C).
- Per-game assignments (global only; YAGNI).
- Drag-to-reorder (a simple per-controller P1–P4 picker is enough).
- Rumble/deadzone/A-B-swap/menu-hotkey settings (later).
- Multitap / >4 players.

## 5. Architecture

- **Pure core (JVM-testable), e.g. `emulation/input/ControllerAssignments.kt`:**
  - `data class ControllerAssignments(val byDescriptor: Map<String, Int>)` with:
    - `fun assign(descriptor: String, port: Int): ControllerAssignments` — sets the port and
      **removes any other descriptor** currently on that port (unique-port).
    - `fun clear(descriptor: String): ControllerAssignments`.
    - `fun portFor(descriptor: String): Int?`.
  - `fun portForDevice(descriptor: String?, a: ControllerAssignments): Int =
     descriptor?.let { a.portFor(it) } ?: 0` (default P1).
- **Persistence `ControllerPrefs` (Android):** serialize `byDescriptor` to the `gobe.settings`
  SharedPreferences (e.g. a small `descriptor=port;descriptor=port` string, or per-key). Load →
  `ControllerAssignments`, save on change. Thin; no pure logic here.
- **UI `ui/controllers/`:**
  - `ControllersScreen` becomes the **list**: takes `controllers: List<ControllerInfo>` (name,
    deviceId, descriptor, assignedPort?) + callbacks `onSelect(descriptor)`. Rows show name + player
    label. Keeps the empty state.
  - New `ControllerDetailScreen`: takes the selected `ControllerInfo`, current assignment, the live
    test state (activeButtons/sticks/lastInput for *this* device), `onAssign(port)`, `onBack`.
    Renders the player selector (P1–P4 chips, selected highlighted) + the test panel from A.
  - The button/stick **highlight is filtered to the selected controller** — the Activity tracks the
    active device id for the test state so other controllers don't drive the panel.
- **`ControllersActivity`:** holds `assignments` (from `ControllerPrefs`), the list, a `selected`
  descriptor (list vs detail), and the per-device test state. On assign → update `assignments` +
  save. Back from detail → list; Back from list → finish.
- **`EmulatorActivity`:** load `assignments` in `onCreate`; add `descriptorOf(deviceId)` (via
  `InputDevice.getDevice(id)?.descriptor`); replace the three `sendKeyEvent(..., 0)` /
  `sendMotionEvent(..., 0)` port args with `portForDevice(descriptorOf(event.deviceId), assignments)`.
  The Select+Start pause combo and Back handling are unchanged (still global).

## 6. Testing

- **Unit (JVM):**
  - `ControllerAssignments.assign` enforces unique ports (assigning descriptor B to port 0 removes
    descriptor A from port 0); `portFor` returns the set port; `clear` removes it.
  - `portForDevice` returns the assigned port, and **0** for an unknown/null descriptor.
- **On-device (ONN):** the Controllers list shows the connected controller with its player label;
  selecting it opens the detail; assigning P2 persists (reopen shows P2); the test panel still
  highlights that controller's buttons. With two controllers, each routes to its assigned port in a
  2-player game (best-effort; note in RESULTS if only one pad is available — verify the single-pad
  path and that an unassigned pad still drives P1).

## 7. Risks

1. **Descriptor stability / availability** — `getDescriptor()` is generally stable per device
   model+port; if a controller returns an empty/duplicate descriptor, assignment may be coarse.
   Mitigated: default-to-P1 keeps input working; document any oddity in RESULTS.
2. **Routing regression for single-player** — mitigated by the P1 default (unassigned → port 0),
   preserving today's behavior; covered by the on-device single-pad check.
3. **Core multiplayer support varies** — routing to port N is correct; whether the game responds is
   the core/game's concern, not ours. Documented.
4. **Test-panel cross-talk** — filtering the highlight to the selected `deviceId` avoids another
   controller lighting the panel.

## 8. Defaults (change on request)

- Two-level list→detail UI; unique ports; unassigned→P1; P1–P4; global (not per-game); persistence
  in `gobe.settings` SharedPreferences by device descriptor. All tunable.

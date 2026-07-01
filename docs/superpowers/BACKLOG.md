# Gobe — Backlog (queued follow-ups)

Short, queued improvements to pick up between/after the main sub-projects.

## UI: grid tiles + Continue-playing row (queued 2026-06-30)

User feedback on the redesigned Home grid:

- **Continue-playing row is too dominant.** When present it's pinned and takes too much
  vertical space, pushing the other games down / cutting them off so you can't see them.
  → Make it less dominant: smaller tiles for that row, and/or don't let it eat the whole
  first screen (e.g. a compact row, or fold it so the main grid stays visible).
- **Tile size/shape.** Make tiles a bit **smaller** and **more horizontal (or square)** —
  most box art the user sees is horizontal/square (arcade, some consoles), and the current
  tall portrait tiles waste space and crop wide art. Reconsider the grid cell aspect ratio.
  (Note: SNES/NES covers are portrait; pick a ratio that's a good compromise, or letterbox
  consistently — Fit already prevents cropping.)
- **Show title + year under the cover.** Below each tile's box art, show the game name in
  text with the **year in parentheses**, e.g. `Super Mario World (1991)`. (Year is already in
  the metadata index + Room.)

Acceptance: Home shows more games per screen; Continue-playing doesn't dominate; each tile
has the cover + "Name (Year)" caption; nothing cut off.

## Gamepad navigation shortcuts + button legend (DONE 2026-06-30 — feat/gamepad-nav)

> Shipped: L1→Search, R1→Settings, and a bottom control legend
> (`Ⓐ Select · Ⓑ Back · L1 Search · R1 Settings`). See
> `plans/2026-06-30-gobe-gamepad-nav-RESULTS.md`. Original notes below.


Make Home navigation faster/more intuitive with a physical gamepad (Pro Controller):

- **Jump to Search with a shoulder button.** A dedicated gamepad button should focus the
  search field directly, so you don't D-pad up to it every time. Suggested: **L1 (top-left
  shoulder)** → Search (matches the user's "arriba a la izq" = top-left). Wire via
  `KEYCODE_BUTTON_L1` in the Home key handler.
- **Jump to Settings with a shoulder button.** Suggested counterpart: **R1 (top-right
  shoulder)** → Settings. (Open to alternatives — e.g. Y face button — decide at impl.)
- **On-screen button legend.** A persistent hint bar (bottom of Home, and adaptable per
  screen) showing the control map so navigation is discoverable, e.g.:
  `Ⓐ Select · Ⓑ Back · L1 Search · R1 Settings`. In-game the legend would read e.g.
  `Select+Start Menu · Ⓑ/Back Pause`. Keep it subtle (low-opacity strip) so it doesn't
  clutter the grid.

Note on semantics: on Android TV gamepads, A = confirm/select and B = back/cancel by default
(so "A salir / B seleccionar" from the message is inverted vs. the Android convention — the
legend should reflect whatever the app actually binds; today Compose focus uses A=select,
B=back). Confirm the real bindings when implementing and make the legend match them.

Acceptance: L1 focuses search, R1 opens settings, and a readable button legend is visible
on Home; bindings and legend text agree.

## USB controller compatibility — not detected (queued 2026-07-01)

USB gamepads (tried several types) are NOT recognized. Setup: the ONN has a single USB-C port; the
user connects controllers through a multiport USB-C adapter (HDMI/SD/USB hub). Needed to play with
multiple controllers.

Two hypotheses to check (needs a USB controller connected to diagnose):

1. **Our detection filter (software — likely culprit for at least some pads).** `ControllersActivity.isGamepad`
   currently requires **both** `SOURCE_GAMEPAD` AND `SOURCE_JOYSTICK` (tightened in sub-project A to
   exclude the ONN remote + virtual devices). Many USB gamepads / arcade encoders report
   `SOURCE_GAMEPAD` **without** `SOURCE_JOYSTICK`, so this AND-filter would hide them. Fix idea:
   accept `SOURCE_GAMEPAD` alone but exclude the remote/virtual another way (e.g. `!InputDevice.isVirtual`,
   or exclude devices whose sources are DPAD-only/keyboard-remote). Re-check against the ONN remote so
   it stays excluded.
2. **Hardware / USB-OTG (environment).** The ONN's USB-C may not host the controller through that
   particular hub (power, or the adapter is display-oriented, not a data hub). Verify Android even
   sees the device: `adb shell dumpsys input | grep -iE "Name:|Sources:"` and
   `adb shell getprop | grep -i otg` while the USB pad is plugged. If it doesn't appear in `dumpsys`
   at all, it's the hub/OTG (try a powered hub / different adapter), not our app.

Diagnosis step: connect one USB controller through the adapter and capture `dumpsys input` — if it
shows up with `SOURCE_GAMEPAD` (with or without JOYSTICK), the fix is our filter (#1); if it doesn't
show at all, it's hardware (#2).

**DIAGNOSED 2026-07-01 → hardware (#2), NOT our app.** With the USB controller connected through the
multiport adapter: `dumpsys input` shows no gamepad; `dumpsys usb` reports `host_connected=false`,
`kernel_state=DISCONNECTED`; `/dev/bus/usb/` is empty. The ONN *does* advertise
`feature:android.hardware.usb.host`, so it supports OTG — but the multiport dongle isn't presenting
any USB device to the ONN in host mode. **No code fix applies** (Android never sees the device).
Recommendations for the user: (a) use a dedicated USB-C **OTG** adapter/cable (CC-pin configured for
host) with the controller plugged directly, not through the display dongle; (b) try a **powered**
USB-C hub; (c) sanity-check the dongle by inserting a USB flash drive / SD card — if those aren't
seen either, the dongle isn't doing host-mode data on this device; (d) **Bluetooth is the reliable
multi-controller path** on the ONN (the Pro Controller already works via BT; pair 2+ BT pads).
Revisit our `isGamepad` filter (relax to `GAMEPAD && !isVirtual`) only if/when a USB pad actually
enumerates and is still hidden.

## Other noted follow-ups

- Genre filter/browse on Home (genre data already in the index + Room).
- Detail cover for unmatched games could reuse the branded default placeholder.

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

**CONFIRMED 2026-07-01:** a USB flash drive AND an SD card in the same multiport adapter are ALSO
not seen (no USB/SD volume in `sm list-volumes` / `/mnt/media_rw`; `host_connected=false`). So the
adapter does no host-mode USB data with the ONN for any device → definitively the adapter/OTG, not
our app. Action is on the user's side (proper USB-C OTG adapter or powered hub); Bluetooth remains
the reliable multi-controller path. Nothing to implement here.

**RESOLVED (won't-fix, hardware) 2026-07-01:** the user tried two USB-C docks (MOKIN MOUC0501,
UCN3270-2) including the **powered** scenario (charger → dock PD-in → dock → ONN, so the dock powers
the ONN and exposes downstream USB). Still `host_connected=false`, nothing enumerated. Conclusion:
the ONN's single USB-C port acts as **power sink only** and does **not** do simultaneous
PD-sink + USB **host** (no dual-role/DRP host+power like a laptop). USB controllers are therefore not
viable on this ONN via that port; **Bluetooth is the supported multi-controller path** (pair 2+ BT
pads, use the player-assignment from sub-project B). No app work possible.

## FDS disk-swap button never appears (BUG, diagnosed 2026-07-01)

The v0.2 FDS disk-swap (merged to main) does not work: the "Change disk (n/N)" button in the pause
menu **never shows**, even for a genuine 2-side FDS game that prompts "INSERT SIDE B" (verified with
Zelda no Densetsu on the ONN). The button is gated on `diskCount > 1`, and `diskCount` comes from
`GLRetroView.getAvailableDisks()`, which is returning ≤1.

Diagnosis so far:
- Emulation itself is fine (FDS BIOS `disksys.rom` loads, game boots to title, asks to flip disk).
- Reading the count at `onCoreReady()` (first rendered frame) is too early — that's the FDS BIOS boot
  screen. Changing it to re-read on every pause-open did **NOT** fix it (still no button at the title
  screen). So it's not purely a timing issue.
- Bytecode (`javap -c` on LibretroDroid 0.14.0): `getAvailableDisks()` → `getAvailableDisks(true)` →
  `runOnEmulationThread(true, {…})`, i.e. it runs **synchronously** on the emulation thread and waits
  for the result. So it's not an async/returns-0 race.

Next step (needs the ONN, uninterrupted): add temporary logging of the RAW `getAvailableDisks()` /
`getCurrentDisk()` return values (and any exception) in the disk read, launch a 2-side FDS game, let
it fully boot, open the menu, and read `adb logcat -s GobeDisk`. That tells us whether the call
returns 1, 0, throws, or whether fceumm just doesn't expose FDS sides via the libretro disk-control
interface the way PS1 M3U multi-disc does (LibretroDroid's disk API may be M3U-oriented). If fceumm
doesn't expose it, options: (a) FDS side-swap via a different core mechanism, (b) drop disk-swap for
FDS and keep it for future M3U-based multi-disc systems, (c) document as unsupported. **Until fixed,
the button is simply never shown (harmless — no crash), and disk-swap must NOT be advertised in the
v0.2 release notes.**

## Top-100 Arcade curated list (queued 2026-07-05)

The user wants to grow the arcade library toward "the 100 best arcade games". **We do NOT download or
source ROMs** — that's copyright infringement and contradicts Gobe's own "bring your own legally-obtained
files" policy (README). The legal, useful deliverable instead:

- Export a **ranked "Top 100 Arcade" list** derived from IGDB (we already fetch arcade ratings; ~200
  arcade games are flagged `recommended`). For each, include the **FBNeo/MAME set name** (the `.zip`
  basename the core expects, e.g. `sf2ce`, `mvsc`) so the user knows exactly what to look for/dump.
  Source the set names from the MAME/FBNeo DAT or the libretro MAME `.rdb` we already parse in
  `build.py`. Output a simple markdown/CSV the user can check off.
- Optional companion: a small "romset doctor" — point it at `Download/ROMs/Arcade/`, report which of
  the Top-100 are present, which `.zip`s are missing files the core needs, and which are absent.

Purely a curation/verification aid; no game content is distributed.

## Other noted follow-ups

- Genre filter/browse on Home (genre data already in the index + Room).
- Detail cover for unmatched games could reuse the branded default placeholder.

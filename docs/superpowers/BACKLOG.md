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

## Gamepad navigation shortcuts + button legend (queued 2026-06-30)

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

## Other noted follow-ups

- Genre filter/browse on Home (genre data already in the index + Room).
- Detail cover for unmatched games could reuse the branded default placeholder.

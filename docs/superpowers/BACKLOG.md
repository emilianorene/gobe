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

## Other noted follow-ups

- Genre filter/browse on Home (genre data already in the index + Room).
- Detail cover for unmatched games could reuse the branded default placeholder.

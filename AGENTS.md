# AGENTS.md

## Project purpose
This repository contains the product specification for RetroTV, a personal Android TV emulator app for the ONN Plus 4K device. The current repo is spec-first; implementation work should follow the decisions captured in [RETRO_TV_SPEC.md](RETRO_TV_SPEC.md).

## Core product context
- Goal: one Android TV app for arcade, NES, SNES, and N64 games.
- Primary UX: remote control with D-pad, with Bluetooth gamepads as a secondary input path.
- Stack direction: Kotlin + Jetpack Compose for TV, with Libretro cores integrated through a native bridge layer.
- The app layer is the main implementation target; the emulator cores and native bridge are treated as reusable dependencies/modules.

## Design constraints to preserve
- Prefer Android TV-native interaction patterns over generic phone/tablet UI.
- Prioritize focus management, D-pad navigation, and visible focus states.
- Follow the storage model in the spec: use full external storage access via MANAGE_EXTERNAL_STORAGE rather than SAF for this project.
- Keep the architecture split into three layers: emulator cores, native bridge, and app UI/business logic.
- Treat core-specific options as dynamic data exposed by the core when possible, rather than hardcoding UI for each system.

## Implementation guidance
- Start from the phased plan in [RETRO_TV_SPEC.md](RETRO_TV_SPEC.md) and implement the earliest milestone that is coherent and testable.
- When adding features, keep the TV experience first-class: grid navigation, focus handling, and remote input should be explicit design considerations.
- For game/library features, favor a simple, reliable MVP over extra polish.
- If a decision differs from the spec, update the spec or document the rationale clearly before changing the architecture.

## Working conventions
- Keep changes scoped and incremental.
- Prefer readable Compose UI and a clear separation between UI, persistence, and emulation integration.
- Preserve the project’s personal-use context and avoid overengineering for general public distribution.
- Avoid introducing unnecessary abstractions before the first working end-to-end flow exists.

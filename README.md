<div align="center">

# 🎮 Gobe

**An all-in-one Libretro emulator frontend for Android TV.**

Browse your retro library with box art and play SNES, Arcade and NES/Famicom Disk System games
from your couch — built natively in Kotlin + Jetpack Compose for TV.

![version](https://img.shields.io/badge/version-0.2.0--beta-blue)
![platform](https://img.shields.io/badge/platform-Android%20TV-green)
![minSdk](https://img.shields.io/badge/minSdk-24-orange)
![license](https://img.shields.io/badge/license-GPLv3-lightgrey)

</div>

> **Beta.** Gobe is an early, working build tested on an ONN Google TV 4K Plus (`armeabi-v7a`).
> It does **not** include any games or BIOS files — you provide your own (see below).

---

## Screenshots

| Home (box-art grid + filters) | Game detail | In-game |
|---|---|---|
| ![home](docs/screenshots/01-home.png) | ![detail](docs/screenshots/02-detail.png) | ![ingame](docs/screenshots/03-ingame.png) |

| Browse by genre | Controller setup |
|---|---|
| ![genre](docs/screenshots/05-genre.png) | ![controllers](docs/screenshots/04-controllers.png) |

## Features

- **Emulation** (via [LibretroDroid](https://github.com/Swordfish90/LibretroDroid)):
  - **SNES** (snes9x)
  - **Arcade** (FBNeo)
  - **NES / Famicom Disk System** (FCEUmm) — `.fds` supported (needs the FDS BIOS, see below)
- **Library**: automatic ROM scanning, a searchable **box-art grid** (art from the
  [libretro-thumbnails](https://github.com/libretro-thumbnails) repos), filter by **system** and
  **genre**, a **Continue playing** row, and per-game metadata (players / genre / year).
- **In-game**: save states + SRAM, a pause menu (Save / Load / Exit), and an on-launch controls hint.
  Open the menu with **Select + Start** (or Back).
- **Controllers**: detect connected gamepads with a live button test, assign each controller to a
  **player (P1–P4)**, quick **Swap A/B / X/Y**, and full **button remapping by capture** — all
  per-controller and saved.
- **Navigation**: gamepad shortcuts (**L1** → search, **R1** → settings) with an on-screen legend.
- **Languages**: English and Spanish.

## Install

Gobe isn't on the Play Store, so you install it by **sideloading** the APK. Pick whichever method
matches your TV — the whole thing takes about 5 minutes.

### Step 1 — Allow installs from unknown sources

Android TV blocks sideloaded apps until you allow the app that will do the installing:

- **Google TV / Android TV:** **Settings → System → About**, then click **Android version /
  build** a few times to confirm it's Android 7+ (see [Requirements](#requirements)). Then go to
  **Settings → Apps → Security & restrictions → Unknown sources** and turn **ON** the app you'll
  use to install (e.g. **Downloader**, **Files**, or **Drive**).
- **Fire TV:** **Settings → My Fire TV → Developer options → Install unknown apps**, then enable
  the app you'll install from (e.g. **Downloader**).

### Step 2 — Get the APK onto your TV

Choose one:

- **Downloader app (easiest, no computer):** install [Downloader] from your TV's app store, open
  it, and enter the direct link to the latest `app-release.apk` from the
  [**Releases**](../../releases) page. It will download and offer to install.
- **USB drive:** on a computer, download `app-release.apk` from the
  [**Releases**](../../releases) page and copy it to a USB stick. Plug it into the TV, open a file
  manager (e.g. **X-plore** or **Files**), browse to the APK and open it.
- **ADB (for developers):** with USB debugging on, run
  `adb install app-release.apk` from your computer.

### Step 3 — Install and open

1. When the installer opens, choose **Install**. If **Google Play Protect** warns you the app is
   from an unknown developer, tap **"Install anyway"** (see troubleshooting below) — this is
   expected for sideloaded apps.
2. Open **Gobe** from your TV's apps row.
3. On first launch Gobe asks for the **all-files access** permission — grant it so Gobe can scan
   your ROM folders. (You can also grant it later in **Android Settings → Apps → Gobe →
   Permissions**.)

> **"There was a problem parsing the package"?** Your device is below the minimum Android
> version, or the download is incomplete. Gobe needs **Android 7.0 (API 24) or newer** — see
> [Requirements](#requirements) below. Re-download if the transfer was interrupted.
>
> **Google Play Protect says "app blocked to protect your device"?** This is the normal warning
> for any sideloaded app from a developer Play hasn't seen before — it is **not** an error. Tap
> **"Install anyway"** (the text link above the "Got it" button) to continue.

## Requirements

- **Android TV 7.0 (API 24) or newer** — Google TV, Fire TV (FireOS 6+), or a generic Android TV box.
- **CPU**: ARM (32-bit `armeabi-v7a` / 64-bit `arm64-v8a`) or x86 / x86_64 — all bundled in the APK.
- ~150 MB free for the app, plus space for your ROMs.
- A gamepad or Bluetooth controller (a TV remote works for browsing, not for gameplay).
- **Phones/tablets are not supported** — Gobe is a TV (leanback) app.

| ✅ Compatible | ❌ Not supported |
|---|---|
| Nvidia Shield TV | Phones / tablets (TV-only app) |
| Chromecast with Google TV, ONN | Android TV older than 7.0 |
| Fire TV Stick / Cube (FireOS 6+) | |
| Xiaomi Mi Box / TV Stick | |
| TCL / Hisense / Sony Google TV | |
| Generic Android TV boxes (Android 7+) | |

> **Updating from an earlier build:** the beta APK is signed with Gobe's release key. If you had a
> development (debug) build installed, uninstall it first — Android won't update across different
> signing keys. From v0.1.0 onward, releases update in place.

[Downloader]: https://www.aftvnews.com/downloader/

## Setup: adding your games

Gobe ships **no games or BIOS** — you supply your own, legally-obtained files. Here's the full flow.

### Step 1 — Create the folder layout on your TV

By default Gobe looks in **`Download/ROMs/`** on the device's internal storage. Create this
structure (you can add more folders later in **Settings → ROM folders**):

```
Download/
└── ROMs/
    ├── (your game files go here, e.g. mario.sfc, game.zip, zelda.fds)
    └── system/
        └── disksys.rom   ← BIOS, only needed for Famicom Disk System games
```

### Step 2 — Copy your game files onto the TV

Choose whichever is convenient:

- **USB drive:** copy your ROMs into `ROMs/` on a USB stick, plug it into the TV, and use a file
  manager (e.g. **X-plore**, **Files**) to move them into `Download/ROMs/` on internal storage.
- **Network / cloud:** put the files on a shared folder, Google Drive, or an SMB share and pull
  them down with a file manager on the TV.
- **ADB (developers):** `adb push mygame.sfc /sdcard/Download/ROMs/`

**Supported formats:**

| System | File types | Notes |
|---|---|---|
| SNES | `.smc`, `.sfc` | — |
| NES / Famicom Disk System | `.nes`, `.fds` | `.fds` needs the BIOS (Step 3) |
| Arcade (FBNeo) | `.zip` | romset must match the FBNeo set |

### Step 3 — (FDS only) add the BIOS

Famicom Disk System `.fds` games need the **`disksys.rom`** BIOS. Place it at
**`Download/ROMs/system/disksys.rom`**. Without it, `.fds` games won't boot.

### Step 4 — Scan and play

Open Gobe. It scans your folders automatically and builds a **box-art grid** (cover art is
downloaded from the [libretro-thumbnails] project — connect the TV to the internet the first time
so art can load). Filter by **system** or **genre**, pick a game, and press to play.

> **Arcade note:** if a `.zip` is missing a file the FBNeo core expects, Gobe shows an on-screen
> "FBNeo Error" naming exactly which file to add to the `.zip`.

[libretro-thumbnails]: https://github.com/libretro-thumbnails

## Controllers

**Settings → Controllers** lists connected gamepads. Select one to:
- assign it to a **player** (P1–P4) — input is routed to that port,
- **Swap A/B / X/Y** (handy for Nintendo-layout pads),
- **remap any button** by capture ("press the button for A…").

> **Note:** on some Android TV boxes (including the ONN) the single USB-C port is power-only and
> does **not** act as a USB host, so **USB controllers may not be detected** — use **Bluetooth**,
> which is the reliable path for one or more controllers.

## Building from source

```bash
./gradlew :app:assembleDebug      # debug build
./gradlew :app:assembleRelease    # signed release (needs keystore.properties + the keystore)
./gradlew :app:testDebugUnitTest  # unit tests
```

The release build reads signing config from a **gitignored** `keystore.properties` in the repo root
(`storeFile` / `storePassword` / `keyAlias` / `keyPassword`). Without it, the release build is
produced unsigned. The Libretro cores are bundled as `armeabi-v7a` `.so` files under
`app/src/main/jniLibs/`.

## Tech stack

Kotlin · Jetpack Compose for **TV** (`androidx.tv:tv-material3`) · Room · Coil · Kotlin Coroutines ·
[LibretroDroid] 0.14.0 · minSdk 24 / targetSdk 34.

[LibretroDroid]: https://github.com/Swordfish90/LibretroDroid

## Roadmap

See [ROADMAP.md](ROADMAP.md). Next up (v0.3): favorites/recently-added rows, richer metadata and
art matching, manual art override, and sort options.

## Known limitations

- Tested on a single device (ONN Google TV 4K Plus, 32-bit `armeabi-v7a`).
- **USB controllers**: not detected on the ONN (hardware — its USB-C is power-only). Use Bluetooth.
- **Rumble** is not available on this hardware.
- **FDS multi-disk** swap has a known bug — the disk-swap button doesn't appear yet (under
  investigation). Single-disk `.fds` games are unaffected.
- A couple of arcade titles need a specific file inside their `.zip` (FBNeo names it on screen).

## Legal

Gobe is a personal, non-commercial project. It **bundles GPL-licensed Libretro cores** and is
therefore released under the **GPLv3** (see [LICENSE](LICENSE)). It **does not distribute any game
ROMs or BIOS** — you must supply your own, legally-obtained files. Trademarks and game content
belong to their respective owners.

## Credits

Built on [LibretroDroid] and the Libretro ecosystem (cores + [thumbnails] + database).

[thumbnails]: https://github.com/libretro-thumbnails

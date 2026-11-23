# Setup guide

## 1. Install RetroArch

RetroHub supports the official Android builds:

| Package | Build |
|---|---|
| `com.retroarch.aarch64` | 64-bit package |
| `com.retroarch` | Universal package |
| `com.retroarch.ra32` | 32-bit package |

Open RetroArch once and install the cores you need: **Main Menu → Online Updater → Core Downloader**.
On the Google Play edition, cores are installed as Play modules, but RetroHub launches them the same way.

## 2. Install RetroHub TV

Build the APK (see the [README](../README.md#build-from-source)) and install it:

```
adb connect <tv-ip>:<port>
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 3. Grant storage access

RetroHub needs to list ROM files, which Android doesn't treat as media.

| Android version | Access needed |
|---|---|
| 5–10 | Storage permission prompt |
| 11+ | **All files access** |

Open **Settings → Storage → Grant storage access**. Many TVs hide the All files access page. If yours does, run:

```
adb shell appops set --uid com.retrohub.tv MANAGE_EXTERNAL_STORAGE allow
```

Android can reset this when an app is reinstalled. If a scan reports no storage access, grant it again. RetroHub
never removes games because a folder looks empty.

## 4. Give RetroArch access to your games (Play edition)

The Google Play edition has no general storage permission. It can open only the folders you grant it.

1. In RetroArch, open **Load Content** and add your ROM folder (or a parent such as `Download`).
   TVs without a system folder picker need a file manager that provides one.
2. **Start any game from that folder once.** RetroHub learns the grant from RetroArch's history.

**Settings → RetroArch → Game access** lists the folders RetroHub has learned. Builds with normal storage access
skip this step.

## 5. Find your games

Press **Find my games automatically** on Home (also in **Settings → Library** and **Systems**).

RetroHub reads RetroArch's playlists, then scans shared storage. Files are matched in two ways:

- **Unique extensions** such as `.sfc`, `.gba` and `.nes` identify the system directly.
- **Shared extensions** such as `.zip`, `.bin`, `.cue`, `.chd` and `.iso` need a folder named after the system.

Folder names are matched flexibly, so `PS1Games`, `SegaGenesis`, `Sony PlayStation` and `roms/snes` all work.
A recommended layout:

```
ROMs/
├── nes/
├── snes/
├── gba/
├── genesis/
└── ps1/
```

Review the proposed systems and press **Add**. RetroHub then scans them and downloads artwork and details.

To add a system manually, use **Settings → Manage systems → Add system**.

## 6. Play and return

Select a game and press **Play** or **Resume**. To come back to RetroHub:

- **Hold Start for 2 seconds** (change this in **Settings → RetroArch → Return to RetroHub from a game**), or
- Open RetroArch's menu and choose **Close Content**.

Both write the auto-save. The TV's Home button leaves the game **without** updating it, and RetroArch ignores a TV
remote's Back button during gameplay, so a controller is recommended.

## Profiles and saves

Select the avatar at the top right to switch or add profiles. Each profile has its own favourites, play history,
save games and save states, stored in:

```
Android/media/<retroarch-package>/RetroHub/profiles/<profile-id>/
```

To save mid-game, use RetroArch's menu: **Save States → Save State**. Deleting a profile removes its history in
RetroHub, but leaves its save files on storage.

## BIOS files

RetroArch, not RetroHub, loads BIOS files from its **System/BIOS** directory. The Play edition can't read a plain
path outside its own folders, so use:

```
Android/media/<retroarch-package>/RetroArch/system
```

## Lite mode

**Settings → Performance → Lite mode** cycles Auto, On and Off. Auto enables it on low-RAM devices. It reduces image
caches and removes focus zoom, crossfades and backdrop artwork. Restart RetroHub to apply a change.

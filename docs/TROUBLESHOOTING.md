# Troubleshooting

## Black screen when launching a game

Check RetroArch's log during a launch:

```
adb logcat -c
# Start the game from RetroHub, wait 10 seconds
adb logcat -d | grep -E "\[ENV\]|Permission to access|Permission Denial"
```

| Log line | Meaning | Fix |
|---|---|---|
| `Permission to access file ... is denied` | RetroArch can't read that plain path | Update RetroHub; give RetroArch access to the folder |
| `Permission Denial: opening provider` | RetroArch has no grant for that `saf://` folder | Open a game from that folder in RetroArch once |
| `Auto-start game "saf://..."`, no errors | Launch succeeded | Check the core and game in RetroArch directly |

Screenshots taken with `adb` can appear black while a game is running, so check the TV itself.

## "RetroArch can't open this folder yet"

You're using the Play edition and RetroHub hasn't learned a grant covering this folder. In RetroArch, open **Load
Content**, go to that folder and start any game. Then try again. **Settings → RetroArch → Game access** shows the
known folders.

## A core is missing

RetroHub can't see which cores are installed. If RetroArch can't load one:

1. Install it: **RetroArch → Online Updater → Core Downloader**. The game page shows the name.
2. Or choose another core: **Edit system → Core**. Per-game overrides are in the game page's options menu.

## Games aren't found

- Confirm storage access in **Settings → Storage**.
- `.zip`, `.bin`, `.cue`, `.chd` and `.iso` need a folder named after the system, such as `snes` or `PS1Games`.
- Discovery goes 6 folders deep and skips media folders such as `DCIM`, `Movies`, `Music` and `Android`.
- Check the system's ROM folder and extensions, then press **Scan**.

If a scan says it found no games and removed nothing, Android probably withdrew storage access. See below.

## Storage access is lost after reinstalling

```
adb shell appops get com.retrohub.tv MANAGE_EXTERNAL_STORAGE
adb shell appops set --uid com.retrohub.tv MANAGE_EXTERNAL_STORAGE allow
```

## I can't get back to RetroHub from a game

Hold **Start** for 2 seconds on a controller, or open RetroArch's menu (Start + Select by default) and choose
**Close Content**. A TV remote's Back button doesn't work during gameplay; use Home, which doesn't update the auto-save.

## Resume doesn't appear

The auto-save is written only when you leave with the controller shortcut or **Close Content**. Saves are per profile,
so check that the right profile is active.

## Covers or details are missing

- Metadata needs internet and a metadata database set on the system.
- Unusual file names may not match. Try **Settings → Retry unmatched games**.
- Not every game has artwork in libretro-thumbnails.

## Useful adb commands

```
adb connect <tv-ip>:<port>
adb shell dumpsys package com.retrohub.tv | grep versionName
adb shell dumpsys meminfo com.retrohub.tv
adb shell am force-stop com.retroarch.aarch64
```

## Reporting a bug

Please include your TV model, Android version, RetroArch package and version, RetroHub version, the system and file
type, and the log output above. Remove personal paths and never attach ROM or BIOS files.

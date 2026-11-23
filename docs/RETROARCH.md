# RetroArch integration

RetroHub starts games by launching RetroArch's exported activity with intent extras. Findings here were checked
against RetroArch 1.22.2 and its source.

## Launch contract

```
Component: <package>/com.retroarch.browser.retroactivity.RetroActivityFuture
```

| Extra | Value | Required |
|---|---|---|
| `ROM` | Game path: a file path or RetroArch `saf://` path | Yes |
| `LIBRETRO` | Full path to the core | Yes |
| `CONFIGFILE` | Launch config written by RetroHub | Normally |
| `IME` | Current input method | No |
| `DATADIR` | RetroArch's `ApplicationInfo.dataDir` | No |
| `APK` | RetroArch's `ApplicationInfo.sourceDir` | No |
| `SDCARD` | Primary shared storage root | No |
| `EXTERNAL` | Optional external folder | No |

Cores resolve to:

```
<RetroArch dataDir>/cores/<core>_libretro_android.so
```

Priority is: per-game override → system core path → system core name. The logic is in
[`LaunchPlanner.kt`](../app/src/main/java/com/retrohub/tv/launch/LaunchPlanner.kt). Each game page's technical
details show the equivalent `adb shell am start` command.

Before launching, RetroHub closes any RetroArch process left in the background so each game starts cleanly.

## Google Play edition

The Play edition targets a modern SDK and requests **no storage permissions**. A plain path fails:

```
MediaProvider: Permission to access file: /storage/emulated/0/Download/PS1Games/Game.chd is denied
```

It reads external files through Storage Access Framework folder grants, using its own path format
(`libretro-common/vfs/vfs_implementation_saf.c`):

```
saf://<tree URI, with % as %25 and / as %2F>/<path inside the tree>
```

For example:

```
saf://content:%2F%2Fcom.android.externalstorage.documents%2Ftree%2Fprimary%253ADownload/PS1Games/Game.chd
```

A launcher can't list another app's grants, so RetroHub learns them:

1. It detects the edition: no storage permission in RetroArch's manifest.
2. It reads RetroArch's playlists and history from `Android/media/<package>/RetroArch/playlists`.
3. It maps each `saf://` tree to a real folder, and keeps the mapping only if the implied file exists.
4. It converts the game's path using the closest known folder.

If no known folder contains the game, RetroHub shows instructions instead of launching into a black screen. See
[`RetroArchStorage.kt`](../app/src/main/java/com/retrohub/tv/detect/RetroArchStorage.kt).

## Launch config

RetroHub regenerates a small config before each launch. The user's config is included **last**, because with
`#include` first its values override RetroHub's.

```ini
savefile_directory = ".../RetroHub/profiles/1/saves"
savestate_directory = ".../RetroHub/profiles/1/states"
savestate_auto_save = "true"
savestate_auto_load = "true"      # "false" for Start fresh
savestate_thumbnail_enable = "true"
quit_on_close_content = "1"
input_quit_gamepad_combo = "7"    # hold Start
confirm_quit = "false"
#include "/storage/emulated/0/Android/data/<package>/files/retroarch.cfg"
```

The save keys are omitted when per-profile saves are off. These settings apply only to games launched from RetroHub.

The config lives in `Android/media/<package>/RetroHub`, which RetroArch can read and write in every build and
RetroHub can write with All files access.

Exit combo values:

| Value | Combo |
|---|---|
| 0 | Off |
| 3 | L1 + R1 + Start + Select |
| 7 | Hold Start |
| 8 | Hold Select |

## Core selection

Android doesn't let RetroHub list RetroArch's private core folder, so it can't verify that a core is installed.
It chooses in this order:

1. The core RetroArch's playlists most often use for that system
2. The recommendation in [`CoreCatalog.kt`](../app/src/main/java/com/retrohub/tv/data/CoreCatalog.kt)

| System | Recommended core |
|---|---|
| NES | `fceumm` |
| SNES | `snes9x` |
| Nintendo 64 | `mupen64plus_next_gles3` |
| Game Boy / Color | `gambatte` |
| Game Boy Advance | `mgba` |
| Nintendo DS | `melonds` |
| Master System, Genesis, Sega CD, Game Gear | `genesis_plus_gx` |
| Saturn | `yabasanshiro` |
| Dreamcast | `flycast` |
| PlayStation | `pcsx_rearmed` |
| PSP | `ppsspp` |
| PC Engine | `mednafen_pce_fast` |
| Atari 2600 | `stella` |
| Neo Geo Pocket | `mednafen_ngp` |
| WonderSwan | `mednafen_wswan` |
| Arcade | `fbneo` or `mame2003_plus` |

Core availability differs by RetroArch build and CPU. For example, the Play edition's list contains `melondsds`
rather than `melonds` and `mednafen_psx_hw` rather than `mednafen_psx`. If a recommendation isn't available, choose
another core in the system editor.

## Known limits

- RetroHub can't confirm that a core is installed before launch.
- Specific save slots are loaded inside RetroArch; there is no intent extra for them.
- The auto-save is written only when content closes properly, not when leaving with the TV's Home button.
- A TV remote's Back button doesn't reach RetroArch's menu during gameplay.

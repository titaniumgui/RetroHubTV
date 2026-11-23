# Architecture

RetroHub TV is a single-activity Kotlin app using Jetpack Compose for TV, Room and Coil. It has no background
services and no dependency-injection framework.

## Project layout

```
app/src/main/java/com/retrohub/tv/
├── RetroHubApp.kt      App container, bounded Coil ImageLoader, trim-memory handling
├── MainActivity.kt     Single activity, screen routing, status overlay
├── data/               Room entities and DAOs, settings, system presets, core catalog
├── detect/             RetroArch detection, SAF paths, playlist import, game discovery
├── launch/             Pure launch planning and the Android intent launcher
├── metadata/           Streaming RDB reader, matching, artwork and details download
├── perf/               Device memory profile, Lite mode, cache budgets
├── saves/              Per-profile launch config and save-state discovery
├── scan/               File and SAF ROM scanner
├── storage/            Permissions, SAF-to-path mapping, disc-track filtering
└── ui/                 ViewModel, navigation, theme, components and screens
app/src/test/           JVM unit tests for the pure logic
tools/brand.py          Regenerates the logo, launcher icons and TV banner
docs/                   Project documentation
```

## Design principles

- **Pure logic is separate from Android.** Launch planning, path conversion, matching, RDB parsing and playlist parsing
  are plain Kotlin objects with JVM unit tests.
- **The ViewModel coordinates.** `MainViewModel` exposes `StateFlow`s and owns the screen stack, scans and launches.
- **RetroArch stays the emulator.** RetroHub only prepares a config and starts it.
- **Never destroy library data on uncertain input.** A scan that finds nothing doesn't remove existing games.

## Data model

Room database `retrohub.db`, schema version 2, with a manual 1→2 migration.

| Table | Purpose |
|---|---|
| `systems` | ROM folder, extensions, core, BIOS info, artwork folder, metadata database |
| `games` | File location, metadata, artwork paths, CRC, match status |
| `profiles` | Name and avatar colour |
| `game_stats` | Per-profile favourite, last played and play count |

`games.favorite`, `lastPlayed` and `playCount` are legacy v1 columns. Per-profile values live in `game_stats`.
App preferences are stored in `SharedPreferences` through `SettingsStore`.

## Key flows

### Launching a game

```
MainViewModel.play()
  ├─ Re-detect RetroArch if the configured package is gone
  ├─ ProfileStorage.writeConfig()             launch config with saves and exit settings
  ├─ RetroArchStorage.needsFolderGrants()     Play edition?
  │    └─ SafPaths.forPath()                  convert the path to saf://
  ├─ LaunchPlanner.plan()                     resolve core and extras, or a clear error
  ├─ RetroArchLauncher.stopIfRunning()        close a stale background process
  └─ RetroArchLauncher.launch()               start the activity and record play history
```

### Finding games

```
GameDiscovery.run()
  ├─ Read RetroArch playlists                 system, files and core
  ├─ Walk shared storage                      classify by extension and folder name
  ├─ Discovery.merge()                        RetroArch's core wins
  └─ Discover screen                          user reviews, then systems are added and scanned
```

### Metadata

```
MetadataService.run()
  ├─ Compute CRC32 (zip: stored CRC; files over 64 MB skipped)
  ├─ Download or reuse the libretro .rdb (refreshed every 30 days)
  ├─ Stream the RDB twice: match, then merge fields
  └─ Download box art and screenshots, downscale, store as WebP
```

Matching order is CRC, exact name, then name without region and revision tags. Artwork from a user's own folder is
never replaced.

## Low-memory design

| Area | Approach |
|---|---|
| Lists | Lazy rows and grids with a small `GameTile` projection |
| Images | Decoded at display size; memory cache up to 16 MB (6 MB Lite), capped at 1/8 of app heap |
| Disk cache | 64 MB (24 MB Lite); stored artwork is small WebP |
| Scanning | Batches of 200 rows |
| Databases | Streamed, never loaded whole |
| Hero art | Changes only after focus rests; skipped in Lite mode |
| Background work | None; scans and downloads run only while the app is open |
| Leaving for a game | Image memory cache is cleared when the UI is hidden |

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Download game databases and artwork |
| `READ_EXTERNAL_STORAGE` (≤ Android 12) | List ROM folders |
| `WRITE_EXTERNAL_STORAGE` (≤ Android 10) | Write launch configs and save folders |
| `MANAGE_EXTERNAL_STORAGE` | List ROMs and write launch configs on Android 11+ |
| `KILL_BACKGROUND_PROCESSES` | Close a stale RetroArch before launching |

## Third-party components

| Component | License |
|---|---|
| AndroidX, Jetpack Compose, Compose for TV, Room | Apache-2.0 |
| Kotlin and kotlinx.coroutines | Apache-2.0 |
| Coil | Apache-2.0 |
| JUnit (tests only) | EPL-1.0 |
| org.json (tests only) | Public domain |

External data and tools, not included in this repository:

- [RetroArch](https://github.com/libretro/RetroArch) (GPL-3.0), launched as a separate app
- [libretro-database](https://github.com/libretro/libretro-database) and
  [libretro-thumbnails](https://github.com/libretro-thumbnails), downloaded on the user's device
- [Inter](https://github.com/rsms/inter) (SIL OFL 1.1), an input argument for `tools/brand.py`

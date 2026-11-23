# Changelog

All notable changes are listed here. Versions follow `MAJOR.MINOR.PATCH`.

## 1.2.1 - 2026-11-20

### Added
- Return to RetroHub from a game with a controller shortcut (hold Start by default, configurable).
- Close Content in RetroArch now returns to RetroHub.
- A reminder on each game page explaining how to get back.

## 1.2 - 2026-11-20

### Fixed
- Black screen with RetroArch's Google Play edition. RetroHub now launches through RetroArch's folder grants using its
  `saf://` path format.

### Changed
- Profile configs, saves and states moved to `Android/media/<package>/RetroHub`, which every RetroArch build can access.
- Discovery reads RetroArch's playlists and history from its media folder.

### Added
- "Game access" status in Settings and clear instructions when RetroArch can't open a folder.

## 1.1.1 - 2026-11-20

### Changed
- Close any RetroArch process left in the background before launching a game.

## 1.1 - 2026-11-20

### Added
- Automatic core selection per system, with a compatible-core picker.
- Flexible folder recognition (`PS1Games`, `SegaGenesis`, and similar).
- Disc handling: `.bin` tracks and `.m3u` discs are listed as one game.

### Fixed
- The About section in Settings couldn't be reached with the D-pad.
- Page subtitles crowded the action buttons.

## 1.0.0 - 2026-11-20

### Added
- Top-shelf TV interface, game library, details, favourites, recently played and search.
- ROM scanning with path and Storage Access Framework folders.
- Launching through an existing RetroArch installation.
- Profiles with separate history, saves and states; Resume and Start fresh.
- Automatic metadata and artwork from libretro.
- Automatic RetroArch detection and Find my games.
- Lite mode and low-memory design.

# Contributing

Thanks for helping improve RetroHub TV.

## Development setup

1. Install Android Studio, or the Android SDK with platform 36 and JDK 17 or 21.
2. Clone the repository and open it in Android Studio.
3. Run the checks:

   ```
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) first. For anything involving launching or storage, also read
[docs/RETROARCH.md](docs/RETROARCH.md).

## Project rules

- **1–2 GB devices come first.** Avoid background services, unbounded caches, autoplay video and heavy effects.
- **Everything must work with a D-pad.** Check focus entry, exit and restoration on each screen you change.
- **Keep pure logic testable.** Put parsing, matching and path logic in plain Kotlin and add JVM tests.
- **Never lose user data on uncertain input.** Unreadable storage must not look like deleted games.
- **Don't assume storage access.** Check both RetroArch builds with storage permissions and the Play edition.
- **Room schema changes need a migration.**
- **Lite mode must stay functional.**

## Pull requests

- Keep each change focused, and explain what changed and why.
- Include the device, Android version and RetroArch build you tested on.
- Update the docs and [CHANGELOG.md](CHANGELOG.md) for user-visible changes.
- Bump `versionCode` and `versionName` only for releases.

## What not to commit

Never commit ROMs, BIOS files, downloaded box art, signing keys, `local.properties`, or logs with personal paths.

## License

By contributing, you agree that your contributions are licensed under the [GPL-3.0](LICENSE).

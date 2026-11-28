package com.retrohub.tv.launch

import com.retrohub.tv.i18n.tr

/**
 * RetroArch Android launch contract (verified against the RetroArch 32-bit APK in this repo,
 * com.retroarch.ra32 1.22.2, and RetroArch's source):
 *
 *  Component : <package>/com.retroarch.browser.retroactivity.RetroActivityFuture (exported)
 *  Extras    : ROM        absolute path of the content file (plain file I/O, no content:// support)
 *              LIBRETRO   absolute path of the core .so  (required — "Missing extra LIBRETRO")
 *              CONFIGFILE retroarch.cfg path (optional; its presence also tells RetroArch the
 *                         launch came from a front-end, so it skips its own permission prompt)
 *              IME        current input method id (optional)
 *              DATADIR    RetroArch's ApplicationInfo.dataDir (optional)
 *              APK        RetroArch's ApplicationInfo.sourceDir (optional)
 *              SDCARD     primary shared storage root (optional)
 *              EXTERNAL   RetroArch external files dir (optional)
 *  Cores     : RetroArch keeps installed cores in <dataDir>/cores/<name>_libretro_android.so
 *
 * This object is pure Kotlin so it can be unit-tested on the JVM.
 */
object LaunchPlanner {
    const val CORE_SUFFIX = "_libretro_android.so"

    data class Inputs(
        val romPath: String?,
        val romDocumentUri: String?,
        val gameCoreOverride: String,
        val systemCoreName: String,
        val systemCorePath: String,
        val coreDirectory: String,
        val packageName: String,
        val activityName: String,
        val configFile: String,
        val externalDirectory: String,
        val ime: String?,
        val dataDir: String?,
        val apkPath: String?,
        val sdcard: String?,
    )

    sealed interface Result {
        data class Ready(val plan: LaunchPlan) : Result
        data class Blocked(val reason: String) : Result
    }

    data class LaunchPlan(
        val packageName: String,
        val activityName: String,
        val extras: List<Pair<String, String>>,
        val corePath: String,
    ) {
        fun extra(key: String): String? = extras.firstOrNull { it.first == key }?.second
    }

    /** Converts a core name (or file name, or full path) to the .so path RetroArch should load. */
    fun coreFile(nameOrPath: String, coreDirectory: String): String? {
        val v = nameOrPath.trim()
        if (v.isEmpty()) return null
        if (v.startsWith("/")) return v
        val dir = coreDirectory.trim().trimEnd('/')
        if (dir.isEmpty()) return null
        val file = when {
            v.endsWith(".so") -> v
            v.endsWith("_libretro_android") -> "$v.so"
            v.endsWith("_libretro") -> "${v}_android.so"
            else -> "$v$CORE_SUFFIX"
        }
        return "$dir/$file"
    }

    /** Resolution order: per-game override > system core path > system core name + core dir. */
    fun resolveCore(i: Inputs): String? =
        coreFile(i.gameCoreOverride, i.coreDirectory)
            ?: i.systemCorePath.trim().takeIf { it.isNotEmpty() }
            ?: coreFile(i.systemCoreName, i.coreDirectory)

    fun plan(i: Inputs): Result {
        if (i.packageName.isBlank()) return Result.Blocked(tr("Set the RetroArch package name in Settings."))
        if (i.activityName.isBlank()) return Result.Blocked(tr("Set the RetroArch activity in Settings."))
        val rom = i.romPath?.trim().orEmpty()
        if (rom.isEmpty()) {
            return Result.Blocked(
                if (i.romDocumentUri != null) {
                    tr("This game was found through a Storage Access Framework folder that has no " +
                        "filesystem path. RetroArch cannot open content:// URIs, only file paths. " +
                        "Keep ROMs in internal storage and add the folder by path (or pick it from " +
                        "the \"Internal storage\" root in the folder picker).")
                } else {
                    tr("This game has no file path.")
                }
            )
        }
        val core = resolveCore(i) ?: return Result.Blocked(
            when {
                i.gameCoreOverride.isBlank() && i.systemCoreName.isBlank() && i.systemCorePath.isBlank() ->
                    tr("No core configured. Set a core name or core path for this system (or this game).")
                else ->
                    tr("Core directory not set. Open Settings and press \"Detect\" next to Core directory, " +
                        "or type the folder that holds the *{0} files.", CORE_SUFFIX)
            }
        )
        val extras = buildList {
            add("ROM" to rom)
            add("LIBRETRO" to core)
            i.configFile.trim().takeIf { it.isNotEmpty() }?.let { add("CONFIGFILE" to it) }
            i.ime?.takeIf { it.isNotBlank() }?.let { add("IME" to it) }
            i.dataDir?.takeIf { it.isNotBlank() }?.let { add("DATADIR" to it) }
            i.apkPath?.takeIf { it.isNotBlank() }?.let { add("APK" to it) }
            i.sdcard?.takeIf { it.isNotBlank() }?.let { add("SDCARD" to it) }
            i.externalDirectory.trim().takeIf { it.isNotEmpty() }?.let { add("EXTERNAL" to it) }
        }
        return Result.Ready(LaunchPlan(i.packageName.trim(), i.activityName.trim(), extras, core))
    }

    /** Equivalent shell command, shown on the details screen for troubleshooting. */
    fun adbCommand(plan: LaunchPlan): String = buildString {
        append("adb shell am start -S -n ")
        append(plan.packageName).append('/').append(plan.activityName)
        plan.extras.forEach { (k, v) -> append(" -e ").append(k).append(' ').append(shellQuote(v)) }
    }

    private fun shellQuote(v: String): String =
        if (v.all { it.isLetterOrDigit() || it in "/._-:" }) v else "'" + v.replace("'", "'\\''") + "'"
}

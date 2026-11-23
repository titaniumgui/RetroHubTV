package com.retrohub.tv.detect

import android.content.Context
import android.os.Build
import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.launch.RetroArchInfo
import com.retrohub.tv.launch.RetroArchLauncher
import com.retrohub.tv.storage.StorageAccess
import java.io.File

/**
 * Pure rules for finding RetroArch (unit-tested). All three official Android builds share the
 * same launch activity; only the package name differs.
 */
object RetroArchSetup {
    const val PKG_AARCH64 = "com.retroarch.aarch64"
    const val PKG_UNIVERSAL = "com.retroarch"
    const val PKG_RA32 = "com.retroarch.ra32"
    val KNOWN = listOf(PKG_AARCH64, PKG_UNIVERSAL, PKG_RA32)

    /** Preference when several are installed: the one matching the device's CPU first. */
    fun preferenceOrder(supports64Bit: Boolean): List<String> =
        if (supports64Bit) listOf(PKG_AARCH64, PKG_UNIVERSAL, PKG_RA32) else listOf(PKG_RA32, PKG_UNIVERSAL, PKG_AARCH64)

    /** Keeps the configured package while it is installed; otherwise picks the best installed one. */
    fun choosePackage(configured: String, installed: Set<String>, supports64Bit: Boolean): String? = when {
        configured in installed -> configured
        else -> preferenceOrder(supports64Bit).firstOrNull { it in installed } ?: installed.firstOrNull()
    }

    /**
     * RetroArch for Android keeps its main config in its external files folder
     * (observed on 1.22.2: "Saved config to /storage/emulated/0/Android/data/<package>/files/retroarch.cfg").
     */
    fun defaultConfigPath(packageName: String, primaryRoot: String): String =
        "${primaryRoot.trimEnd('/')}/Android/data/$packageName/files/retroarch.cfg"

    /** True when a stored path clearly belongs to a different RetroArch package (left over after switching). */
    fun belongsToOtherPackage(path: String, packageName: String): Boolean =
        KNOWN.any { other -> other != packageName && (path.contains("/$other/") || path.endsWith("/$other")) }
}

/** What detection found, and what it changed. */
data class DetectionResult(
    val info: RetroArchInfo?,
    val installed: List<RetroArchInfo>,
    val settings: AppSettings,
    val changes: List<String>,
)

class RetroArchDetector(private val context: Context, private val launcher: RetroArchLauncher) {

    fun installed(): List<RetroArchInfo> = RetroArchSetup.KNOWN.mapNotNull { launcher.info(it) }

    private val supports64Bit: Boolean get() = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    /**
     * Fills in package, core folder and config path.
     * [force] = the user pressed Detect: replace everything with what was found.
     * Otherwise only missing or stale values (pointing at an uninstalled package) are fixed.
     */
    fun detect(current: AppSettings, force: Boolean, preferPackage: String? = null): DetectionResult {
        val all = installed()
        val installedNames = all.map { it.packageName }.toSet()
        val pkg = (preferPackage?.takeIf { it in installedNames })
            ?: RetroArchSetup.choosePackage(current.retroArchPackage, installedNames, supports64Bit)
            ?: return DetectionResult(null, all, current, emptyList())
        val info = all.first { it.packageName == pkg }
        val changes = ArrayList<String>()
        var s = current

        if (s.retroArchPackage != pkg) {
            s = s.copy(retroArchPackage = pkg)
            changes += "RetroArch: $pkg"
        }
        if (s.retroArchActivity.isBlank()) s = s.copy(retroArchActivity = AppSettings.DEFAULT_ACTIVITY)

        val coreDir = info.dataDir?.let { "$it/cores" }
        if (coreDir != null && (force || s.coreDirectory.isBlank() || RetroArchSetup.belongsToOtherPackage(s.coreDirectory, pkg))) {
            if (s.coreDirectory != coreDir) changes += "Cores: $coreDir"
            s = s.copy(coreDirectory = coreDir)
        }

        val config = findConfig(pkg)
        if (force || s.configFile.isBlank() || RetroArchSetup.belongsToOtherPackage(s.configFile, pkg)) {
            if (s.configFile != config) changes += "Config: $config"
            s = s.copy(configFile = config)
        }
        return DetectionResult(info, all, s, changes)
    }

    /**
     * RetroArch's default config path. On Android 10 and older RetroHub can check it exists and
     * falls back to the classic /RetroArch/retroarch.cfg; on Android 11+ other apps' Android/data
     * folders are hidden, so the default location is used as-is.
     */
    private fun findConfig(pkg: String): String {
        val primary = StorageAccess.primaryRoot()
        val default = RetroArchSetup.defaultConfigPath(pkg, primary)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return default
        val legacy = "$primary/RetroArch/retroarch.cfg"
        return when {
            File(default).isFile -> default
            File(legacy).isFile -> legacy
            else -> default
        }
    }
}

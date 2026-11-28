package com.retrohub.tv.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LiteMode { AUTO, ON, OFF }

/**
 * Launch settings. Package and activity defaults were verified against the RetroArch 32-bit APK
 * (com.retroarch.ra32 1.22.2: exported activity RetroActivityFuture). RetroArch paths start empty:
 * the user fills them in, or presses "Detect", which reads RetroArch's real ApplicationInfo.
 */
data class AppSettings(
    val retroArchPackage: String = DEFAULT_PACKAGE,
    val retroArchActivity: String = DEFAULT_ACTIVITY,
    /** Folder holding *_libretro_android.so files. RetroArch itself uses <its dataDir>/cores/. */
    val coreDirectory: String = "",
    /** The user's own retroarch.cfg. Included by every profile config (or passed directly). */
    val configFile: String = "",
    /** Optional value for the EXTERNAL extra. Blank = extra omitted. */
    val externalDirectory: String = "",
    val sendImeExtra: Boolean = true,
    val clearTaskOnLaunch: Boolean = true,
    val liteMode: LiteMode = LiteMode.AUTO,
    // ---- profiles & saves ----
    val currentProfileId: Long = AppDatabase.DEFAULT_PROFILE_ID,
    val askProfileOnStart: Boolean = true,
    /** Give each profile its own RetroArch save/state folders and auto-save + resume. */
    val profileSaves: Boolean = true,
    /** Where profile folders live. Blank = <shared storage>/RetroHub. */
    val profileRoot: String = "",
    // ---- metadata ----
    val metadataAuto: Boolean = true,
    val metadataSnaps: Boolean = true,
    /** Controller combo that quits RetroArch back to RetroHub (RetroArch's input_quit_gamepad_combo). */
    val exitCombo: Int = 7,
    /** App language code ("en", "ar"). Blank until chosen on first launch. */
    val language: String = "",
) {
    companion object {
        const val DEFAULT_PACKAGE = "com.retroarch.ra32"
        const val DEFAULT_ACTIVITY = "com.retroarch.browser.retroactivity.RetroActivityFuture"
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = state.asStateFlow()
    val current: AppSettings get() = state.value

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            retroArchPackage = prefs.getString("raPackage", null) ?: d.retroArchPackage,
            retroArchActivity = prefs.getString("raActivity", null) ?: d.retroArchActivity,
            coreDirectory = prefs.getString("coreDir", null) ?: "",
            configFile = prefs.getString("configFile", null) ?: "",
            externalDirectory = prefs.getString("externalDir", null) ?: "",
            sendImeExtra = prefs.getBoolean("sendIme", d.sendImeExtra),
            clearTaskOnLaunch = prefs.getBoolean("clearTask", d.clearTaskOnLaunch),
            liteMode = runCatching { LiteMode.valueOf(prefs.getString("lite", null) ?: "AUTO") }
                .getOrDefault(LiteMode.AUTO),
            currentProfileId = prefs.getLong("profile", d.currentProfileId),
            askProfileOnStart = prefs.getBoolean("askProfile", d.askProfileOnStart),
            profileSaves = prefs.getBoolean("profileSaves", d.profileSaves),
            profileRoot = prefs.getString("profileRoot", null) ?: "",
            metadataAuto = prefs.getBoolean("metaAuto", d.metadataAuto),
            metadataSnaps = prefs.getBoolean("metaSnaps", d.metadataSnaps),
            exitCombo = prefs.getInt("exitCombo", d.exitCombo),
            language = prefs.getString("lang", null) ?: "",
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(state.value)
        prefs.edit()
            .putString("raPackage", s.retroArchPackage.trim())
            .putString("raActivity", s.retroArchActivity.trim())
            .putString("coreDir", s.coreDirectory.trim())
            .putString("configFile", s.configFile.trim())
            .putString("externalDir", s.externalDirectory.trim())
            .putBoolean("sendIme", s.sendImeExtra)
            .putBoolean("clearTask", s.clearTaskOnLaunch)
            .putString("lite", s.liteMode.name)
            .putLong("profile", s.currentProfileId)
            .putBoolean("askProfile", s.askProfileOnStart)
            .putBoolean("profileSaves", s.profileSaves)
            .putString("profileRoot", s.profileRoot.trim())
            .putBoolean("metaAuto", s.metadataAuto)
            .putBoolean("metaSnaps", s.metadataSnaps)
            .putInt("exitCombo", s.exitCombo)
            .putString("lang", s.language)
            .apply()
        state.value = read()
    }
}

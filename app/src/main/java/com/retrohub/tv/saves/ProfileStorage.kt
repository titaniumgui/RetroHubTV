package com.retrohub.tv.saves

import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.storage.StorageAccess
import java.io.File

/** Locates profile folders in shared storage, where RetroArch can read and write them. */
object ProfileStorage {
    /**
     * Default: RetroArch's own media folder (Android/media/<package>/RetroHub). Every RetroArch
     * build can read and write there, including the Google Play edition, which has no storage
     * permissions. RetroHub writes there with All-files access.
     */
    fun root(settings: AppSettings): File =
        File(settings.profileRoot.ifBlank { StorageAccess.primaryRoot() + "/Android/media/${settings.retroArchPackage}/RetroHub" })

    fun profileDir(settings: AppSettings, profileId: Long) = File(root(settings), "profiles/$profileId")
    fun statesDir(settings: AppSettings, profileId: Long) = File(profileDir(settings, profileId), "states")
    fun savesDir(settings: AppSettings, profileId: Long) = File(profileDir(settings, profileId), "saves")

    /**
     * Writes the launch config and returns its path. With per-profile saves it lives in the profile
     * folder and points RetroArch's saves/states there; without, it only adds the exit-to-RetroHub
     * settings on top of the user's own config. Needs shared-storage write access.
     */
    fun writeConfig(settings: AppSettings, profileId: Long, autoLoad: Boolean): Result<String> = runCatching {
        val include = settings.configFile.ifBlank { null }
        if (!settings.profileSaves) {
            val dir = root(settings)
            if (!dir.isDirectory && !dir.mkdirs()) error("Cannot create ${dir.path}")
            val cfg = File(dir, "launch.cfg")
            cfg.writeText(ProfileConfig.build(null, null, autoLoad, include, settings.exitCombo))
            return@runCatching cfg.absolutePath
        }
        val dir = profileDir(settings, profileId)
        val saves = savesDir(settings, profileId)
        val states = statesDir(settings, profileId)
        listOf(dir, saves, states).forEach { if (!it.isDirectory && !it.mkdirs()) error("Cannot create ${it.path}") }
        val cfg = File(dir, "retroarch.cfg")
        cfg.writeText(ProfileConfig.build(saves.absolutePath, states.absolutePath, autoLoad, include, settings.exitCombo))
        cfg.absolutePath
    }
}

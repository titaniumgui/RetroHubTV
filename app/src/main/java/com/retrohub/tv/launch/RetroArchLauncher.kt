package com.retrohub.tv.launch

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.data.GameEntity
import com.retrohub.tv.data.SystemEntity
import com.retrohub.tv.storage.StorageAccess

data class RetroArchInfo(
    val packageName: String,
    val versionName: String?,
    val dataDir: String?,
    val apkPath: String?,
)

class RetroArchLauncher(private val context: Context) {

    /** Reads the installed RetroArch's real ApplicationInfo. Null when it is not installed/visible. */
    fun info(packageName: String): RetroArchInfo? {
        val pm = context.packageManager
        return try {
            @Suppress("DEPRECATION")
            val pkg = pm.getPackageInfo(packageName, 0)
            val app = pkg.applicationInfo
            RetroArchInfo(
                packageName = packageName,
                versionName = pkg.versionName,
                dataDir = app?.dataDir,
                apkPath = app?.sourceDir,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** RetroArch's own core folder (RetroActivityCommon.getCorePath(): dataDir + "/cores/"). */
    fun detectCoreDirectory(packageName: String): String? =
        info(packageName)?.dataDir?.let { "$it/cores" }

    /** [configFile] = the CONFIGFILE to pass (a profile config, or the user's own retroarch.cfg). */
    fun inputs(game: GameEntity, system: SystemEntity, s: AppSettings, configFile: String = s.configFile): LaunchPlanner.Inputs {
        val ra = info(s.retroArchPackage)
        val ime = if (s.sendImeExtra) {
            runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) }.getOrNull()
        } else null
        return LaunchPlanner.Inputs(
            romPath = game.path,
            romDocumentUri = game.documentUri,
            gameCoreOverride = game.coreOverride,
            systemCoreName = system.coreName,
            systemCorePath = system.corePath,
            coreDirectory = s.coreDirectory,
            packageName = s.retroArchPackage,
            activityName = s.retroArchActivity,
            configFile = configFile,
            externalDirectory = s.externalDirectory,
            ime = ime,
            dataDir = ra?.dataDir,
            apkPath = ra?.apkPath,
            sdcard = StorageAccess.primaryRoot(),
        )
    }

    fun launch(plan: LaunchPlanner.LaunchPlan, clearTask: Boolean): String? {
        if (info(plan.packageName) == null) {
            return "RetroArch (${plan.packageName}) is not installed or not visible to RetroHub."
        }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(plan.packageName, plan.activityName)
            plan.extras.forEach { (k, v) -> putExtra(k, v) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Replace any RetroArch task left in the background so the new ROM/core extras are read on a fresh start.
            if (clearTask) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return try {
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "RetroArch activity ${plan.activityName} was not found in ${plan.packageName}."
        } catch (e: SecurityException) {
            "Android blocked the launch: ${e.message}"
        }
    }

    /**
     * Closes a RetroArch process left in the background (for example after playing from RetroArch
     * itself or pressing Home mid-game). RetroArch for Android hangs on a black screen when new
     * content is started into a process that is still running, so every launch starts it fresh.
     * Only works while RetroArch is in the background, which it is when RetroHub is on screen.
     * Returns true when a running RetroArch was found and asked to close.
     */
    fun stopIfRunning(packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        // Other apps' processes aren't listed on modern Android, so just ask; it's a no-op when not running.
        return runCatching { am.killBackgroundProcesses(packageName); true }.getOrDefault(false)
    }

    /** Opens RetroArch's own menu (to grant its storage permission, install cores, set BIOS dir). */
    fun openRetroArch(packageName: String): String? {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return "RetroArch ($packageName) is not installed."
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (_: ActivityNotFoundException) {
            "Could not open RetroArch."
        }
    }
}

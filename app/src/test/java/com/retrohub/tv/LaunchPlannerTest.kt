package com.retrohub.tv

import com.retrohub.tv.data.AppSettings
import com.retrohub.tv.launch.LaunchPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchPlannerTest {
    private val coreDir = "/data/user/0/com.retroarch.ra32/cores"

    private fun inputs(
        rom: String? = "/storage/emulated/0/ROMs/snes/game.sfc",
        uri: String? = null,
        override: String = "",
        coreName: String = "snes9x",
        corePath: String = "",
        dir: String = coreDir,
        config: String = "",
    ) = LaunchPlanner.Inputs(
        romPath = rom, romDocumentUri = uri, gameCoreOverride = override,
        systemCoreName = coreName, systemCorePath = corePath, coreDirectory = dir,
        packageName = AppSettings.DEFAULT_PACKAGE, activityName = AppSettings.DEFAULT_ACTIVITY,
        configFile = config, externalDirectory = "", ime = "com.example/.Ime",
        dataDir = "/data/user/0/com.retroarch.ra32", apkPath = "/data/app/ra/base.apk", sdcard = "/storage/emulated/0",
    )

    @Test fun coreNameExpandsToRetroArchFileName() {
        assertEquals("$coreDir/snes9x_libretro_android.so", LaunchPlanner.coreFile("snes9x", coreDir))
        assertEquals("$coreDir/snes9x_libretro_android.so", LaunchPlanner.coreFile("snes9x_libretro", coreDir))
        assertEquals("$coreDir/snes9x_libretro_android.so", LaunchPlanner.coreFile("snes9x_libretro_android.so", "$coreDir/"))
        assertEquals("/sdcard/cores/x.so", LaunchPlanner.coreFile("/sdcard/cores/x.so", ""))
        assertNull(LaunchPlanner.coreFile("snes9x", ""))
    }

    @Test fun overrideBeatsSystemPathBeatsName() {
        assertEquals("$coreDir/bsnes_libretro_android.so", LaunchPlanner.resolveCore(inputs(override = "bsnes", corePath = "/x/y.so")))
        assertEquals("/x/y.so", LaunchPlanner.resolveCore(inputs(corePath = "/x/y.so")))
        assertEquals("$coreDir/snes9x_libretro_android.so", LaunchPlanner.resolveCore(inputs()))
    }

    @Test fun readyPlanContainsRequiredExtras() {
        val r = LaunchPlanner.plan(inputs(config = "/storage/emulated/0/RetroArch/retroarch.cfg"))
        assertTrue(r is LaunchPlanner.Result.Ready)
        val plan = (r as LaunchPlanner.Result.Ready).plan
        assertEquals("/storage/emulated/0/ROMs/snes/game.sfc", plan.extra("ROM"))
        assertEquals("$coreDir/snes9x_libretro_android.so", plan.extra("LIBRETRO"))
        assertEquals("/storage/emulated/0/RetroArch/retroarch.cfg", plan.extra("CONFIGFILE"))
        assertNull(plan.extra("EXTERNAL"))
        assertTrue(LaunchPlanner.adbCommand(plan).startsWith("adb shell am start -S -n com.retroarch.ra32/"))
    }

    @Test fun safOnlyGameIsBlocked() {
        val r = LaunchPlanner.plan(inputs(rom = null, uri = "content://x/doc"))
        assertTrue(r is LaunchPlanner.Result.Blocked)
        assertTrue((r as LaunchPlanner.Result.Blocked).reason.contains("content://"))
    }

    @Test fun missingCoreDirectoryIsBlocked() {
        val r = LaunchPlanner.plan(inputs(dir = ""))
        assertTrue(r is LaunchPlanner.Result.Blocked)
    }

    @Test fun missingCoreIsBlocked() {
        assertTrue(LaunchPlanner.plan(inputs(coreName = "")) is LaunchPlanner.Result.Blocked)
    }
}

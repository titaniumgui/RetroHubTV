package com.retrohub.tv

import com.retrohub.tv.data.CoreCatalog
import com.retrohub.tv.detect.Discovery
import com.retrohub.tv.storage.StoragePaths
import com.retrohub.tv.detect.Playlists
import com.retrohub.tv.detect.RetroArchSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionTest {
    private val root = "/storage/emulated/0"

    // ---------------- RetroArch package ----------------

    @Test fun keepsConfiguredPackageWhileInstalled() {
        assertEquals("com.retroarch.ra32", RetroArchSetup.choosePackage("com.retroarch.ra32", setOf("com.retroarch.ra32", "com.retroarch.aarch64"), true))
    }

    @Test fun switchesToInstalledBuildWhenConfiguredOneIsMissing() {
        // The reported case: configured ra32, but the TV has the 64-bit build.
        assertEquals("com.retroarch.aarch64", RetroArchSetup.choosePackage("com.retroarch.ra32", setOf("com.retroarch.aarch64"), true))
        assertEquals("com.retroarch.ra32", RetroArchSetup.choosePackage("x", setOf("com.retroarch.ra32", "com.retroarch.aarch64"), false))
        assertNull(RetroArchSetup.choosePackage("com.retroarch.ra32", emptySet(), true))
    }

    @Test fun configAndStalePaths() {
        assertEquals(
            "/storage/emulated/0/Android/data/com.retroarch.aarch64/files/retroarch.cfg",
            RetroArchSetup.defaultConfigPath("com.retroarch.aarch64", root),
        )
        assertTrue(RetroArchSetup.belongsToOtherPackage("/data/user/0/com.retroarch.ra32/cores", "com.retroarch.aarch64"))
        assertFalse(RetroArchSetup.belongsToOtherPackage("/data/user/0/com.retroarch.aarch64/cores", "com.retroarch.aarch64"))
        // "com.retroarch" must not match inside "com.retroarch.aarch64".
        assertFalse(RetroArchSetup.belongsToOtherPackage("/data/user/0/com.retroarch.aarch64/cores", "com.retroarch.aarch64"))
        assertTrue(RetroArchSetup.belongsToOtherPackage("/data/user/0/com.retroarch/cores", "com.retroarch.aarch64"))
    }

    // ---------------- RetroArch playlists ----------------

    @Test fun parsesJsonPlaylist() {
        val json = """
            {"version":"1.5","default_core_path":"/data/user/0/com.retroarch.aarch64/cores/snes9x_libretro_android.so",
             "items":[
              {"path":"/storage/emulated/0/ROMs/snes/Zelda.sfc","label":"Zelda","core_path":"DETECT","core_name":"DETECT","crc32":"1|crc","db_name":"Nintendo - Super Nintendo Entertainment System.lpl"},
              {"path":"/storage/emulated/0/ROMs/snes/Mario.zip#Mario.sfc","label":"Mario","core_path":"/data/user/0/com.retroarch.aarch64/cores/bsnes_libretro_android.so","db_name":""}
             ]}
        """.trimIndent()
        val items = Playlists.parse(json)
        assertEquals(2, items.size)
        assertEquals("/data/user/0/com.retroarch.aarch64/cores/snes9x_libretro_android.so", items[0].corePath) // DETECT -> default core
        assertEquals("Nintendo - Super Nintendo Entertainment System", Playlists.dbName(items[0].dbName!!))
        assertNull(items[1].dbName)
        assertEquals("/storage/emulated/0/ROMs/snes/Mario.zip", Discovery.fileOnDisk(items[1].path))
    }

    @Test fun parsesLegacyPlaylist() {
        val legacy = "/sd/a.nes\nA\n/x/cores/fceumm_libretro_android.so\nFCEUmm\n0|crc\nNintendo - Nintendo Entertainment System.lpl\n"
        val items = Playlists.parse(legacy)
        assertEquals(1, items.size)
        assertEquals("fceumm", Discovery.coreNameFromPath(items[0].corePath))
        assertTrue(Playlists.isSpecial("content_history.lpl"))
    }

    // ---------------- classification and folders ----------------

    @Test fun classifiesByExtensionAndFolder() {
        assertEquals("Nintendo - Super Nintendo Entertainment System", Discovery.classify("$root/Games/Zelda.sfc")?.metadataDb)
        assertEquals("Nintendo - Game Boy Advance", Discovery.classify("$root/x/y/Metroid.gba")?.metadataDb)
        // Shared extensions need a folder hint, and must be valid for that system.
        assertNull(Discovery.classify("$root/Download/random.zip"))
        assertEquals("Sony - PlayStation", Discovery.classify("$root/ROMs/PSX/Game (Disc 1).chd")?.metadataDb)
        assertEquals("Sega - Mega Drive - Genesis", Discovery.classify("$root/roms/Mega Drive/Sonic.zip")?.metadataDb)
        assertNull(Discovery.classify("$root/ROMs/snes/notes.txt"))
    }

    @Test fun picksSensibleRomFolder() {
        assertEquals(
            "$root/ROMs/snes",
            Discovery.romFolderFor(listOf("$root/ROMs/snes/a.sfc", "$root/ROMs/snes/RPG/b.sfc"), root),
        )
        // Files spread across the whole storage: use the folder holding the most of them.
        assertEquals(
            "$root/Games",
            Discovery.romFolderFor(listOf("$root/Games/a.gba", "$root/Games/b.gba", "$root/Download/c.gba"), root),
        )
    }

    @Test fun retroArchCoreWinsWhenMerging() {
        val ra = Discovery.propose("ra", Discovery.presetForDb("Nintendo - Game Boy Advance"), "Nintendo - Game Boy Advance",
            listOf("$root/ROMs/gba/a.gba"), "mgba", root, true, emptySet())
        val scan = Discovery.propose("scan", Discovery.presetForDb("Nintendo - Game Boy Advance"), "Nintendo - Game Boy Advance",
            listOf("$root/ROMs/gba/a.gba", "$root/ROMs/gba/b.gba"), null, root, false, setOf("$root/ROMs/gba"))
        val merged = Discovery.merge(listOf(ra), listOf(scan))
        assertEquals(1, merged.size)
        assertEquals("mgba", merged[0].coreName)
        assertEquals(2, merged[0].gameCount)
        assertTrue(merged[0].alreadyAdded)
    }

    // ---------------- v1.1: folder names, disc games, cores ----------------

    @Test fun recognisesRealWorldFolderNames() {
        // The reported case: a PS1 folder called "PS1Games" holding cue/bin sets.
        assertEquals("Sony - PlayStation", Discovery.classify("$root/Download/PS1Games/Crash (USA).cue")?.metadataDb)
        assertEquals("Sony - PlayStation", Discovery.classify("$root/Download/PS1Games/Crash (USA).bin")?.metadataDb)
        assertEquals("Sega - Mega Drive - Genesis", Discovery.classify("$root/Download/SegaGenesis/Sonic.zip")?.metadataDb)
        assertEquals("Atari - 2600", Discovery.classify("$root/Download/Atari2600/Pitfall.bin")?.metadataDb)
        assertEquals("Sony - PlayStation", Discovery.classify("$root/Sony PlayStation/Game.chd")?.metadataDb)
        assertEquals("Nintendo - Game Boy", Discovery.classify("$root/GameBoy/Tetris.zip")?.metadataDb) // "gameboy" kept whole
        assertTrue("ps1" in Discovery.folderKeys("PS1Games"))
        assertTrue("genesis" in Discovery.folderKeys("SegaGenesis"))
    }

    @Test fun discGamesAreListedOnce() {
        val names = listOf("Crash (USA).cue", "Crash (USA).bin", "FF7 (Disc 1).cue", "FF7 (Disc 1).bin", "FF7 (Disc 2).cue", "FF7.m3u", "Spyro.chd")
        val m3u = mapOf("FF7.m3u" to listOf("FF7 (Disc 1).cue", "FF7 (Disc 2).cue"))
        val visible = StoragePaths.visibleGameFiles(names) { m3u[it].orEmpty() }
        assertEquals(listOf("Crash (USA).cue", "FF7.m3u", "Spyro.chd"), visible)
        // Cartridge .bin files (no cue sheet in the folder) are games, not tracks.
        assertEquals(listOf("Pitfall.bin", "Adventure.bin"), StoragePaths.visibleGameFiles(listOf("Pitfall.bin", "Adventure.bin")) { emptyList() })
    }

    @Test fun coresAreChosenAutomatically() {
        assertEquals("genesis_plus_gx", CoreCatalog.choose("Sega - Mega Drive - Genesis", null))
        assertEquals("pcsx_rearmed", CoreCatalog.choose("Sony - PlayStation", null))
        assertEquals("stella", CoreCatalog.choose("Atari - 2600", null))
        assertEquals("fceumm", CoreCatalog.choose("Nintendo - Nintendo Entertainment System", null))
        // RetroArch's own choice (from its playlists) wins.
        assertEquals("quicknes", CoreCatalog.choose("Nintendo - Nintendo Entertainment System", "quicknes"))
        assertNull(CoreCatalog.choose("Some - Unknown System", null))
        val p = Discovery.propose("scan", Discovery.presetForDb("Sony - PlayStation"), "Sony - PlayStation",
            listOf("$root/Download/PS1Games/a.cue"), null, root, false, emptySet())
        assertEquals("pcsx_rearmed", p.coreName)
        assertFalse(p.coreFromRetroArch)
    }
}

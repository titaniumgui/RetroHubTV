package com.retrohub.tv

import com.retrohub.tv.metadata.MatchRequest
import com.retrohub.tv.metadata.MetadataMatcher
import com.retrohub.tv.metadata.RdbEntry
import com.retrohub.tv.metadata.RdbReader
import com.retrohub.tv.metadata.Thumbnails
import com.retrohub.tv.saves.ProfileConfig
import com.retrohub.tv.saves.SaveStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SavesAndMetadataTest {

    // ---------------- profile config ----------------

    @Test fun includeComesLastSoProfileKeysWin() {
        val cfg = ProfileConfig.build("/s/p/1/saves", "/s/p/1/states", autoLoad = true, includeConfig = "/s/base.cfg")
        val lines = cfg.lines().filter { it.isNotBlank() && !it.startsWith("# ") }
        assertEquals("#include \"/s/base.cfg\"", lines.last())
        assertTrue("savestate_directory = \"/s/p/1/states\"" in lines)
        assertTrue("savestate_auto_save = \"true\"" in lines)
        assertTrue("savestate_auto_load = \"true\"" in lines)
    }

    @Test fun launchConfigReturnsToRetroHub() {
        val cfg = ProfileConfig.build("/a", "/b", autoLoad = true, includeConfig = "/base.cfg", exitCombo = ProfileConfig.EXIT_HOLD_START)
        val lines = cfg.lines()
        assertTrue("quit_on_close_content = \"1\"" in lines)
        assertTrue("input_quit_gamepad_combo = \"7\"" in lines)
        assertTrue("confirm_quit = \"false\"" in lines)
        assertEquals("#include \"/base.cfg\"", lines.last { it.isNotBlank() })
        // Without per-profile saves: only the exit settings, RetroArch's own save folders stay.
        val shared = ProfileConfig.build(null, null, autoLoad = true, includeConfig = "/base.cfg", exitCombo = ProfileConfig.EXIT_OFF)
        assertTrue(shared.lines().none { it.startsWith("savestate_directory") })
        assertTrue("input_quit_gamepad_combo = \"0\"" in shared.lines())
        assertTrue(shared.lines().none { it.startsWith("confirm_quit") })
    }

    @Test fun startFreshDisablesAutoLoadAndNoIncludeWhenBlank() {
        val cfg = ProfileConfig.build("/a", "/b", autoLoad = false, includeConfig = null)
        assertTrue("savestate_auto_load = \"false\"" in cfg.lines())
        assertTrue(cfg.lines().none { it.startsWith("#include") })
    }

    // ---------------- save state names ----------------

    @Test fun saveSlotsAreParsed() {
        val base = "Blue_Screen_Test (Homebrew)"
        assertEquals(0, SaveStates.slotOf("$base.state", base))
        assertEquals(3, SaveStates.slotOf("$base.state3", base))
        assertEquals(-1, SaveStates.slotOf("$base.state.auto", base))
        assertNull(SaveStates.slotOf("$base.state.png", base))
        assertNull(SaveStates.slotOf("Other.state", base))
    }

    // ---------------- RDB reader ----------------

    private fun str(out: ByteArrayOutputStream, s: String) {
        val b = s.toByteArray()
        require(b.size < 32)
        out.write(0xA0 or b.size); out.write(b)
    }

    private fun rdb(vararg entries: Map<String, Any>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("RARCHDB".toByteArray()); out.write(0); out.write(ByteArray(8))
        for (e in entries) {
            out.write(0x80 or e.size)
            for ((k, v) in e) {
                str(out, k)
                when (v) {
                    is String -> str(out, v)
                    is Int -> { out.write(0xCD); out.write(v shr 8); out.write(v and 0xFF) }
                    is ByteArray -> { out.write(0xC4); out.write(v.size); out.write(v) }
                    else -> error("unsupported")
                }
            }
        }
        out.write(0xC0)
        return out.toByteArray()
    }

    private val smbCrc = byteArrayOf(0x39, 0x3a, 0x43, 0x2f)

    @Test fun readerExtractsFieldsAndSkipsOthers() {
        val bytes = rdb(
            mapOf("name" to "Super Mario Bros. (World)", "md5" to "abc", "crc" to smbCrc, "releaseyear" to 1985, "genre" to "Platform"),
        )
        val got = ArrayList<RdbEntry>()
        RdbReader(ByteArrayInputStream(bytes)).forEach { got += it }
        assertEquals(1, got.size)
        assertEquals("Super Mario Bros. (World)", got[0].name)
        assertEquals("393a432f", got[0].crc)
        assertEquals(1985, got[0].releaseYear)
        assertEquals("Platform", got[0].genre)
    }

    @Test fun matcherPrefersCrcThenMergesFieldsAcrossEntries() {
        val bytes = rdb(
            mapOf("name" to "Super Mario Bros. (World)", "crc" to byteArrayOf(0xd4.toByte(), 0x45, 0xf6.toByte(), 0x98.toByte())),
            mapOf("name" to "Super Mario Bros. (World)", "crc" to smbCrc, "developer" to "Nintendo R&D4", "users" to 2),
            mapOf("crc" to smbCrc, "releaseyear" to 1985),
            mapOf("name" to "Tetris (USA)", "genre" to "Puzzle"),
            mapOf("name" to "Tetris (Japan)", "genre" to "Puzzle"),
        )
        val m = MetadataMatcher(
            listOf(
                MatchRequest(1, "smb_renamed.nes", "393a432f"),     // by CRC
                MatchRequest(2, "Tetris.nes", null),                // loose name -> prefers USA
            )
        )
        RdbReader(ByteArrayInputStream(bytes)).forEach(m::pass1)
        val resolved = m.resolved()
        assertEquals("Super Mario Bros. (World)", resolved[1])
        assertEquals("Tetris (USA)", resolved[2])
        m.preparePass2(resolved)
        RdbReader(ByteArrayInputStream(bytes)).forEach(m::pass2)
        val smb = m.metadataFor("Super Mario Bros. (World)")
        assertEquals("Nintendo R&D4", smb.developer)
        assertEquals(2, smb.players)
        assertEquals(1985, smb.releaseYear)
    }

    // ---------------- thumbnails ----------------

    @Test fun thumbnailUrlsFollowLibretroNaming() {
        assertEquals("Nintendo_-_Nintendo_Entertainment_System", Thumbnails.repo("Nintendo - Nintendo Entertainment System"))
        assertEquals("Mario _ Wario", Thumbnails.sanitize("Mario & Wario"))
        assertEquals(
            "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Nintendo_Entertainment_System/master/Named_Boxarts/Super%20Mario%20Bros.%20%28World%29.png",
            Thumbnails.url("Nintendo - Nintendo Entertainment System", "Named_Boxarts", "Super Mario Bros. (World)"),
        )
    }
}

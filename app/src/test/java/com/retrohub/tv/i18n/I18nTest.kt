package com.retrohub.tv.i18n

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class I18nTest {

    @After fun resetLanguage() = I18n.apply(AppLanguage.ENGLISH)

    /** English strings built at runtime (enum labels, Lite mode names, exit shortcuts). */
    private val dynamicKeys = listOf(
        "Auto", "On", "Off",
        "Hold Start (2 seconds)", "Hold Select (2 seconds)", "Off (use Close Content only)", "Hold Start", "Hold Select",
        "Name", "Shown on the dashboard.", "ROM folder (path)", "File extensions", "Core path (optional)", "BIOS folder",
        "Required BIOS files", "Artwork folder (optional)", "Metadata database",
        "RetroArch package", "RetroArch activity", "Core directory", "Your RetroArch config (retroarch.cfg)",
        "External directory (EXTERNAL)", "Profile folder", "Language",
    )

    private val literal = Regex(""""((?:[^"\\]|\\.)*)"""")
    private val trCall = Regex("""(?<![\w.])tr\(\s*("(?:[^"\\]|\\.)*"(?:\s*\+\s*"(?:[^"\\]|\\.)*")*)""")
    private val countCall = Regex("""(?<![\w.])trCount\([^,"]+,\s*"((?:[^"\\]|\\.)*)",\s*"((?:[^"\\]|\\.)*)"""")

    private fun unescape(s: String) = s.replace("\\\"", "\"").replace("\\$", "$").replace("\\\\", "\\")

    private fun sources(): List<File> =
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList().also { assertTrue(it.isNotEmpty()) }

    @Test fun everyStringInTheCodeHasAnArabicTranslation() {
        val missing = mutableListOf<String>()
        for (file in sources()) {
            val text = file.readText()
            trCall.findAll(text).forEach { m ->
                val key = literal.findAll(m.groupValues[1]).joinToString("") { unescape(it.groupValues[1]) }
                if (key !in ArabicCatalog.strings) missing += "${file.name}: $key"
            }
            countCall.findAll(text).forEach { m ->
                val other = unescape(m.groupValues[2])
                if (other !in ArabicCatalog.plurals) missing += "${file.name} (plural): $other"
            }
        }
        dynamicKeys.filterNot { it in ArabicCatalog.strings }.forEach { missing += "dynamic: $it" }
        assertTrue("Missing Arabic translations:\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    @Test fun translationsKeepTheirPlaceholders() {
        val placeholder = Regex("""\{\d}""")
        ArabicCatalog.strings.forEach { (en, ar) ->
            assertEquals("Placeholders differ for: $en", placeholder.findAll(en).map { it.value }.toSet(), placeholder.findAll(ar).map { it.value }.toSet())
        }
        ArabicCatalog.plurals.forEach { (en, forms) ->
            assertEquals("Arabic needs 6 plural forms: $en", 6, forms.size)
        }
    }

    @Test fun arabicPluralCategories() {
        assertEquals(listOf(0, 1, 2, 3, 3, 4, 4, 5, 5, 3, 4), listOf(0, 1, 2, 3, 10, 11, 99, 100, 102, 103, 111).map(I18n::arabicPluralIndex))
    }

    @Test fun englishIsUnchangedAndFillsArguments() {
        I18n.apply(AppLanguage.ENGLISH)
        assertEquals("Hello, Sam", tr("Hello, {0}", "Sam"))
        assertEquals("1 game", trCount(1, "{0} game", "{0} games"))
        assertEquals("3 games", trCount(3, "{0} game", "{0} games"))
        assertEquals("Not in any catalog", tr("Not in any catalog"))
    }

    @Test fun arabicTranslatesPluralsAndFallsBackToEnglish() {
        I18n.apply(AppLanguage.ARABIC)
        assertEquals("مرحبًا، Sam", tr("Hello, {0}", "Sam"))
        assertEquals("لعبتان", trCount(2, "{0} game", "{0} games"))
        assertEquals("5 ألعاب", trCount(5, "{0} game", "{0} games"))
        assertEquals("12 لعبة", trCount(12, "{0} game", "{0} games"))
        assertEquals("Not in any catalog", tr("Not in any catalog"))
    }

    @Test fun arabicSentenceStartingWithLatinGetsRightToLeftMark() {
        I18n.apply(AppLanguage.ARABIC)
        assertTrue(tr("Could not open RetroArch.").startsWith("ت"))
        assertTrue(tr("RetroArch ({0}) is not installed.", "x").startsWith("‏"))
    }

    @Test fun relativeTimeUsesAppLanguage() {
        val now = 10_000_000_000L
        assertEquals("3 hours ago", relativeTime(now - 3 * 3_600_000L, now))
        I18n.apply(AppLanguage.ARABIC)
        assertEquals("منذ 3 ساعات", relativeTime(now - 3 * 3_600_000L, now))
        assertEquals("أمس", relativeTime(now - 30 * 3_600_000L, now))
    }
}

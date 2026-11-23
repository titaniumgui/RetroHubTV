package com.retrohub.tv

import com.retrohub.tv.detect.SafPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses the exact strings from RetroArch's history on a TCL Android 14 TV (Play edition, aarch64). */
class SafPathsTest {
    private val root = "/storage/emulated/0"
    private val tvSaf =
        "saf://content:%2F%2Fza.kilowatch.ultimatefilemanager.documents%2Ftree%2Floc%253Aprimary%252FDownload/PS1Games/Pepsiman (Japan).chd"
    private val tree = "content://za.kilowatch.ultimatefilemanager.documents/tree/loc%3Aprimary%2FDownload"

    @Test fun parsesAndRebuildsRetroArchsFormat() {
        val (t, rel) = SafPaths.parse(tvSaf)!!
        assertEquals(tree, t)
        assertEquals("PS1Games/Pepsiman (Japan).chd", rel)
        assertEquals(tvSaf, SafPaths.build(t, rel))
    }

    @Test fun mapsFileManagerAndSystemTreesToFolders() {
        assertEquals("$root/Download", SafPaths.guessRoot(tree, root))
        assertEquals("$root/Download/PS1Games",
            SafPaths.guessRoot("content://com.android.externalstorage.documents/tree/primary%3ADownload%2FPS1Games", root))
        assertEquals(root, SafPaths.guessRoot("content://com.android.externalstorage.documents/tree/primary%3A", root))
        assertNull(SafPaths.guessRoot("content://com.google.android.apps.docs.storage/tree/acc%3D1%3Bdoc%3Dabc", root))
    }

    @Test fun launchesAnyGameInsideAKnownFolder() {
        val roots = SafPaths.learnRoots(listOf(tvSaf), root) { it == "$root/Download/PS1Games/Pepsiman (Japan).chd" }
        assertEquals(mapOf("$root/Download" to tree), roots)
        // A different game in the same granted folder, never played in RetroArch:
        assertEquals(
            "saf://content:%2F%2Fza.kilowatch.ultimatefilemanager.documents%2Ftree%2Floc%253Aprimary%252FDownload/PS1Games/Crash Team Racing.chd",
            SafPaths.forPath("$root/Download/PS1Games/Crash Team Racing.chd", roots),
        )
        assertTrue(SafPaths.forPath("$root/Download/NES/Mario.nes", roots)!!.endsWith("/NES/Mario.nes"))
        assertNull(SafPaths.forPath("$root/Games/Other.chd", roots))
        assertEquals("$root/Download/PS1Games/Pepsiman (Japan).chd", SafPaths.toRealPath(tvSaf, root))
    }

    @Test fun ignoresMappingsThatDoNotPointAtRealFiles() {
        assertTrue(SafPaths.learnRoots(listOf(tvSaf), root) { false }.isEmpty())
    }
}

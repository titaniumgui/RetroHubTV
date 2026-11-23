package com.retrohub.tv.storage

/**
 * Pure helpers (unit-tested) for turning Storage Access Framework document IDs into real
 * filesystem paths. RetroArch's ROM extra is copied into its content path and opened with
 * ordinary file I/O, so a content:// URI cannot be handed to it. Only these providers expose
 * IDs that map to a path:
 *  - com.android.externalstorage.documents: "primary:ROMs/snes" or "1234-ABCD:ROMs/snes"
 *  - com.android.providers.downloads.documents: "raw:/storage/emulated/0/Download/x.zip"
 * Anything else (cloud drives, media provider, vendor file managers) has no usable path.
 */
object StoragePaths {
    const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    const val DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents"

    fun isContentUri(value: String): Boolean = value.trim().startsWith("content://")

    /**
     * @param primaryRoot value of Environment.getExternalStorageDirectory() on the device,
     *                    passed in rather than hard-coded.
     */
    fun documentIdToPath(authority: String?, documentId: String, primaryRoot: String): String? {
        if (authority == DOWNLOADS_AUTHORITY && documentId.startsWith("raw:")) {
            return documentId.removePrefix("raw:").takeIf { it.startsWith("/") }
        }
        if (authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val colon = documentId.indexOf(':')
        if (colon <= 0) return null
        val volume = documentId.substring(0, colon)
        val relative = documentId.substring(colon + 1).trim('/')
        val root = if (volume.equals("primary", ignoreCase = true)) {
            primaryRoot.trimEnd('/')
        } else {
            "/storage/$volume"
        }
        return if (relative.isEmpty()) root else "$root/$relative"
    }

    /** "sfc, .SMC ;zip" -> {"sfc","smc","zip"}; empty set means "accept nothing". */
    fun parseExtensions(raw: String): Set<String> =
        raw.split(',', ';', ' ', '\n')
            .map { it.trim().removePrefix(".").lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun extensionOf(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot <= 0 || dot == fileName.length - 1) "" else fileName.substring(dot + 1).lowercase()
    }

    fun baseName(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot <= 0) fileName else fileName.substring(0, dot)
    }

    /** "Super_Mario_World (USA).sfc" -> "Super Mario World (USA)" */
    fun titleFromFileName(fileName: String): String =
        baseName(fileName).replace('_', ' ').replace(Regex("\\s+"), " ").trim().ifEmpty { fileName }

    private val DISC_SHEETS = setOf("cue", "gdi", "ccd", "m3u")
    private val TRACK_FILES = setOf("bin", "img")

    /**
     * Hides the parts of disc games so each game is listed once (pure, unit-tested):
     *  - .bin/.img tracks when the folder has a .cue/.gdi/.ccd/.m3u that describes them;
     *  - discs (.cue/.chd/…) that an .m3u playlist in the same folder already lists.
     * [m3uEntries] returns the file names an .m3u lists (empty if unreadable).
     */
    fun visibleGameFiles(names: List<String>, m3uEntries: (String) -> List<String>): List<String> {
        val exts = names.map { extensionOf(it) }
        val hasSheet = exts.any { it in DISC_SHEETS }
        val inPlaylists = names.filter { extensionOf(it) == "m3u" }
            .flatMap { m3u -> m3uEntries(m3u).map { e -> e.trim().substringAfterLast('/').substringAfterLast('\\').lowercase() } }
            .toSet()
        return names.filter { n ->
            val e = extensionOf(n)
            when {
                hasSheet && e in TRACK_FILES -> false
                e != "m3u" && n.lowercase() in inPlaylists -> false
                else -> true
            }
        }
    }
}

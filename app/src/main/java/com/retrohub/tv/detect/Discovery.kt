package com.retrohub.tv.detect

import com.retrohub.tv.data.CoreCatalog
import com.retrohub.tv.data.SystemPreset
import com.retrohub.tv.data.SystemPresets
import com.retrohub.tv.storage.StoragePaths
import org.json.JSONObject

/** One entry of a RetroArch playlist (.lpl). */
data class PlaylistItem(
    val path: String,
    val label: String?,
    val corePath: String?,
    val dbName: String?,
)

/** A system RetroHub proposes to add after discovery. */
data class Proposal(
    val key: String,
    val name: String,
    val metadataDb: String,
    val romFolder: String,
    val extensions: Set<String>,
    /** Chosen automatically: RetroArch's own core for this system when known, else the recommended one. */
    val coreName: String,
    val gameCount: Int,
    /** Found in RetroArch's own playlists (so the core is the one RetroArch already uses). */
    val fromRetroArch: Boolean,
    val alreadyAdded: Boolean = false,
    /** True when [coreName] came from RetroArch's playlists rather than RetroHub's recommendation. */
    val coreFromRetroArch: Boolean = false,
)

/** RetroArch playlist parsing: the JSON format (1.7.6+) and the legacy six-lines-per-entry format. */
object Playlists {
    fun parse(text: String): List<PlaylistItem> {
        val trimmed = text.trimStart()
        return if (trimmed.startsWith("{")) parseJson(trimmed) else parseLegacy(text)
    }

    private fun parseJson(text: String): List<PlaylistItem> {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return emptyList()
        val defaultCore = root.optString("default_core_path").takeUsable()
        val items = root.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i ->
            val o = items.optJSONObject(i) ?: return@mapNotNull null
            val path = o.optString("path").trim()
            if (path.isEmpty()) return@mapNotNull null
            PlaylistItem(
                path = path,
                label = o.optString("label").takeUsable(),
                corePath = o.optString("core_path").takeUsable() ?: defaultCore,
                dbName = o.optString("db_name").takeUsable(),
            )
        }
    }

    private fun parseLegacy(text: String): List<PlaylistItem> =
        text.lines().chunked(6).mapNotNull { e ->
            val path = e.getOrNull(0)?.trim().orEmpty()
            if (path.isEmpty() || e.size < 6) return@mapNotNull null
            PlaylistItem(path, e[1].takeUsable(), e[2].takeUsable(), e[5].takeUsable())
        }

    private fun String?.takeUsable(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("DETECT", ignoreCase = true) && it != "null" }

    /** "Nintendo - Game Boy.lpl" -> "Nintendo - Game Boy". */
    fun dbName(fileOrDb: String): String = fileOrDb.substringAfterLast('/').removeSuffix(".lpl")

    /** RetroArch's own mixed lists (history, favourites, …) are not one system. */
    fun isSpecial(fileName: String): Boolean = fileName.startsWith("content_") || fileName.startsWith("builtin_")
}

object Discovery {
    private val AMBIGUOUS = setOf("zip", "7z", "bin", "cue", "chd", "m3u", "iso", "cso", "pbp", "img", "ccd")

    private val systems: List<SystemPreset> = SystemPresets.all.filter { it.metadataDb.isNotEmpty() }

    /** Extensions that belong to exactly one system, e.g. "sfc" -> Super Nintendo. */
    private val uniqueExtension: Map<String, SystemPreset> = run {
        val owners = HashMap<String, MutableList<SystemPreset>>()
        systems.forEach { p ->
            StoragePaths.parseExtensions(p.extensions).forEach { owners.getOrPut(it) { ArrayList() }.add(p) }
        }
        owners.filter { (ext, list) -> list.size == 1 && ext !in AMBIGUOUS }.mapValues { it.value.first() }
    }

    fun presetForDb(db: String): SystemPreset? = systems.firstOrNull { it.metadataDb.equals(db, ignoreCase = true) }

    private val GENERIC = listOf("collection", "library", "romset", "games", "game", "roms", "rom", "isos", "iso", "files", "set", "dumps")
    private val BRANDS = listOf("nintendo", "sega", "sony", "nec", "snk", "bandai")

    /**
     * Ways a folder name can spell a system: as written, without generic words ("PS1Games" -> "ps1",
     * "Games_GBA" -> "gba"), and without a brand prefix ("SegaGenesis" -> "genesis").
     */
    fun folderKeys(folderName: String): List<String> {
        val raw = folderName.lowercase().filter { it.isLetterOrDigit() }
        if (raw.isEmpty()) return emptyList()
        var stripped = raw
        repeat(2) {
            for (g in GENERIC) {
                if (stripped.length > g.length && stripped.endsWith(g)) stripped = stripped.removeSuffix(g)
                if (stripped.length > g.length && stripped.startsWith(g)) stripped = stripped.removePrefix(g)
            }
        }
        fun noBrand(k: String) = BRANDS.firstOrNull { k.length > it.length && k.startsWith(it) }?.let { k.removePrefix(it) } ?: k
        return listOf(raw, stripped, noBrand(raw), noBrand(stripped)).distinct()
    }

    /** Folder hint from any folder in the path: ".../roms/SNES/Zelda.zip" -> Super Nintendo. */
    fun presetFromFolders(path: String): SystemPreset? {
        val folders = path.split('/').dropLast(1)
        for (folder in folders.asReversed()) {
            val keys = folderKeys(folder)
            systems.firstOrNull { p -> keys.any { it in p.folderHints } }?.let { return it }
        }
        return null
    }

    /**
     * Which system a file belongs to, or null if unsure. A unique extension decides on its own;
     * shared ones (zip, cue, chd, …) need a matching folder name and must be valid for that system.
     */
    fun classify(path: String): SystemPreset? {
        val ext = StoragePaths.extensionOf(path.substringAfterLast('/'))
        if (ext.isEmpty()) return null
        uniqueExtension[ext]?.let { return it }
        val hinted = presetFromFolders(path) ?: return null
        return hinted.takeIf { ext in StoragePaths.parseExtensions(it.extensions) }
    }

    /** "/data/user/0/com.retroarch.aarch64/cores/snes9x_libretro_android.so" -> "snes9x". */
    fun coreNameFromPath(corePath: String?): String? {
        val file = corePath?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: return null
        return when {
            file.endsWith("_libretro_android.so") -> file.removeSuffix("_libretro_android.so")
            file.endsWith("_libretro.so") -> file.removeSuffix("_libretro.so")
            file.endsWith(".so") -> file.removeSuffix(".so")
            else -> null
        }?.takeIf { it.isNotEmpty() }
    }

    /** RetroArch writes archive members as "game.zip#game.nes"; the file on disk is the archive. */
    fun fileOnDisk(path: String): String = path.substringBefore('#')

    /**
     * Folder to register for a set of files: their deepest common folder, unless that would be the
     * whole storage (then the folder holding the most files).
     */
    fun romFolderFor(files: List<String>, storageRoot: String): String {
        val parents = files.map { it.substringBeforeLast('/') }
        if (parents.isEmpty()) return storageRoot
        var common = parents.first()
        for (p in parents.drop(1)) {
            while (common.isNotEmpty() && p != common && !p.startsWith("$common/")) common = common.substringBeforeLast('/', "")
        }
        val root = storageRoot.trimEnd('/')
        return if (common.length > root.length && common.startsWith(root)) common
        else parents.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
    }

    /** Shared helper for both sources: one proposal per system. */
    fun propose(
        key: String,
        preset: SystemPreset?,
        db: String,
        files: List<String>,
        coreName: String?,
        storageRoot: String,
        fromRetroArch: Boolean,
        existingFolders: Set<String>,
    ): Proposal {
        val folder = romFolderFor(files, storageRoot)
        val exts = files.map { StoragePaths.extensionOf(it.substringAfterLast('/')) }.filter { it.isNotEmpty() }.toSet() +
            (preset?.let { StoragePaths.parseExtensions(it.extensions) } ?: emptySet())
        return Proposal(
            key = key,
            name = preset?.name ?: db.substringAfter(" - ", db),
            metadataDb = preset?.metadataDb ?: db,
            romFolder = folder,
            extensions = exts,
            coreName = CoreCatalog.choose(preset?.metadataDb ?: db, coreName).orEmpty(),
            coreFromRetroArch = coreName != null,
            gameCount = files.size,
            fromRetroArch = fromRetroArch,
            alreadyAdded = folder.trimEnd('/') in existingFolders,
        )
    }

    /** RetroArch's playlists win (they know the core); a storage scan fills in the rest. */
    fun merge(fromPlaylists: List<Proposal>, fromScan: List<Proposal>): List<Proposal> {
        val byDb = LinkedHashMap<String, Proposal>()
        fromPlaylists.forEach { byDb[it.metadataDb] = it }
        for (scan in fromScan) {
            val p = byDb[scan.metadataDb]
            byDb[scan.metadataDb] = if (p == null) scan else p.copy(
                // Keep RetroArch's core; take the scan's folder when it holds more games.
                romFolder = if (scan.gameCount > p.gameCount) scan.romFolder else p.romFolder,
                extensions = p.extensions + scan.extensions,
                gameCount = maxOf(p.gameCount, scan.gameCount),
                coreName = if (p.coreFromRetroArch || scan.coreName.isBlank()) p.coreName else scan.coreName,
                alreadyAdded = p.alreadyAdded || scan.alreadyAdded,
            )
        }
        return byDb.values.sortedWith(compareBy<Proposal> { it.alreadyAdded }.thenByDescending { it.gameCount })
    }
}

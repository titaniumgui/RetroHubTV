package com.retrohub.tv.detect

import com.retrohub.tv.data.SystemEntity
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.storage.StoragePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Finds games automatically, in two passes:
 *  1. RetroArch's own playlists (.lpl) — what RetroArch already scanned, with the core it uses.
 *  2. A walk of shared storage for ROM files, classified by extension and folder name.
 * Needs storage access (All-files access on Android 11+). Runs in the foreground only.
 */
class GameDiscovery(private val storage: RetroArchStorage) {

    data class Result(val proposals: List<Proposal>, val playlistsFound: Int, val filesScanned: Int)

    suspend fun run(
        retroArchPackage: String,
        configFile: String,
        existing: List<SystemEntity>,
        onProgress: (String) -> Unit,
    ): Result = withContext(Dispatchers.IO) {
        val root = StorageAccess.primaryRoot()
        val existingFolders = existing.mapNotNull { it.romFolder.trimEnd('/').ifEmpty { null } }.toSet()

        onProgress("Reading RetroArch playlists…")
        val (fromPlaylists, playlistCount) = readPlaylists(root, retroArchPackage, configFile, existingFolders)

        val raCores = retroArchCores(retroArchPackage, configFile)
        onProgress("Looking for games on storage…")
        val (fromScan, scanned) = scanStorage(root, existingFolders, raCores, onProgress)

        Result(Discovery.merge(fromPlaylists, fromScan), playlistCount, scanned)
    }

    // ---------------- RetroArch playlists ----------------

    /** Core RetroArch uses per system (database name -> core id), read from its playlists. */
    suspend fun retroArchCores(retroArchPackage: String, configFile: String): Map<String, String> = withContext(Dispatchers.IO) {
        val root = StorageAccess.primaryRoot()
        val (_, cores, _) = collectPlaylists(root, retroArchPackage, configFile, checkFiles = false)
        cores.mapValues { (_, counts) -> counts.maxByOrNull { it.value }!!.key }
    }

    private fun playlistFolders(root: String, pkg: String, configFile: String): List<File> {
        val dirs = linkedSetOf(
            "$root/RetroArch/playlists",
            "$root/Android/data/$pkg/files/playlists", // readable on Android 10 and older only
        )
        // If RetroArch's config is readable, honour its playlist_directory setting.
        runCatching {
            File(configFile).takeIf { it.isFile }?.useLines { lines ->
                lines.firstOrNull { it.trimStart().startsWith("playlist_directory") }
                    ?.substringAfter('=')?.trim()?.trim('"')
                    ?.takeIf { it.isNotEmpty() && it != "default" }
                    ?.let { dirs.add(it) }
            }
        }
        return dirs.map(::File).filter { it.isDirectory }
    }

    private suspend fun readPlaylists(
        root: String,
        pkg: String,
        configFile: String,
        existingFolders: Set<String>,
    ): Pair<List<Proposal>, Int> {
        val (filesByDb, coresByDb, count) = collectPlaylists(root, pkg, configFile, checkFiles = true)
        val proposals = filesByDb.map { (db, files) ->
            Discovery.propose(
                key = "ra:$db",
                preset = Discovery.presetForDb(db),
                db = db,
                files = files.toList(),
                coreName = coresByDb[db]?.maxByOrNull { it.value }?.key,
                storageRoot = root,
                fromRetroArch = true,
                existingFolders = existingFolders,
            )
        }
        return proposals to count
    }

    private suspend fun collectPlaylists(
        root: String,
        pkg: String,
        configFile: String,
        checkFiles: Boolean,
    ): Triple<Map<String, Set<String>>, Map<String, Map<String, Int>>, Int> {
        val filesByDb = LinkedHashMap<String, MutableSet<String>>()
        val coresByDb = HashMap<String, MutableMap<String, Int>>()
        var count = 0
        // RetroArch's media folder (Play edition), the classic /RetroArch folder, and playlist_directory.
        val lists = (storage.playlistFiles(pkg) +
            playlistFolders(root, pkg, configFile).flatMap { d -> d.listFiles { f -> f.isFile && f.name.endsWith(".lpl") }.orEmpty().toList() })
            .distinctBy { it.absolutePath }
        run {
            for (lpl in lists) {
                coroutineContext.ensureActive()
                if (lpl.length() > 32L * 1024 * 1024) continue
                val items = runCatching { Playlists.parse(lpl.readText()) }.getOrDefault(emptyList())
                if (items.isEmpty()) continue
                count++
                val listDb = if (Playlists.isSpecial(lpl.name)) null else Playlists.dbName(lpl.name)
                for (item in items) {
                    val db = item.dbName?.let(Playlists::dbName) ?: listDb ?: continue
                    Discovery.coreNameFromPath(item.corePath)?.let { core ->
                        val m = coresByDb.getOrPut(db) { HashMap() }
                        m[core] = (m[core] ?: 0) + 1
                    }
                    // Play-edition entries are saf:// paths; translate them to the real file.
                    val file = Discovery.fileOnDisk(
                        if (SafPaths.isSaf(item.path)) SafPaths.toRealPath(item.path, root) ?: continue else item.path
                    )
                    if (checkFiles && !File(file).isFile) continue
                    filesByDb.getOrPut(db) { linkedSetOf() }.add(file)
                }
            }
        }
        return Triple(filesByDb, coresByDb, count)
    }

    // ---------------- storage walk ----------------

    private suspend fun scanStorage(
        root: String,
        existingFolders: Set<String>,
        raCores: Map<String, String>,
        onProgress: (String) -> Unit,
    ): Pair<List<Proposal>, Int> {
        val byDb = LinkedHashMap<String, MutableList<String>>()
        var visited = 0
        var lastReport = 0
        val stack = ArrayDeque<Pair<File, Int>>().apply { add(File(root) to 0) }
        while (stack.isNotEmpty() && visited < MAX_ENTRIES) {
            coroutineContext.ensureActive()
            val (dir, depth) = stack.removeLast()
            val children = dir.listFiles() ?: continue
            val found = ArrayList<Pair<File, String>>()
            for (f in children) {
                visited++
                val name = f.name
                if (name.startsWith('.')) continue
                if (f.isDirectory) {
                    if (depth < MAX_DEPTH && !skipFolder(f, depth, root)) stack.add(f to depth + 1)
                } else {
                    val preset = Discovery.classify(f.path) ?: continue
                    found.add(f to preset.metadataDb)
                }
            }
            // Count each disc game once (tracks behind .cue files, discs behind .m3u playlists).
            val visible = StoragePaths.visibleGameFiles(found.map { it.first.name }) { m3u ->
                runCatching { File(dir, m3u).takeIf { it.length() < 65536 }?.readLines().orEmpty().filter { !it.startsWith("#") } }.getOrDefault(emptyList())
            }.toHashSet()
            for ((f, db) in found) if (f.name in visible) byDb.getOrPut(db) { ArrayList() }.add(f.path)
            if (visited - lastReport >= 2000) {
                lastReport = visited
                onProgress("Looking for games… ${byDb.values.sumOf { it.size }} found")
            }
        }
        val proposals = byDb.map { (db, files) ->
            Discovery.propose(
                key = "scan:$db",
                preset = Discovery.presetForDb(db),
                db = db,
                files = files,
                coreName = raCores[db],
                storageRoot = root,
                fromRetroArch = false,
                existingFolders = existingFolders,
            )
        }
        return proposals to visited
    }

    /** Media, system and RetroArch data folders never hold ROMs worth listing. */
    private fun skipFolder(f: File, depth: Int, root: String): Boolean {
        val n = f.name.lowercase()
        if (depth == 0 && n in TOP_LEVEL_SKIP) return true
        if (f.parentFile?.path == "$root/RetroArch" && n in RETROARCH_SKIP) return true
        if (f.path == "$root/RetroHub") return true
        return false
    }

    companion object {
        private const val MAX_DEPTH = 6
        private const val MAX_ENTRIES = 200_000
        private val TOP_LEVEL_SKIP = setOf(
            "android", "dcim", "pictures", "movies", "music", "alarms", "notifications", "ringtones",
            "podcasts", "audiobooks", "recordings", "lost.dir",
        )
        private val RETROARCH_SKIP = setOf(
            "assets", "autoconfig", "cheats", "config", "cores", "database", "info", "logs", "overlays",
            "playlists", "saves", "screenshots", "shaders", "states", "system", "temp", "thumbnails", "filters",
        )
    }
}

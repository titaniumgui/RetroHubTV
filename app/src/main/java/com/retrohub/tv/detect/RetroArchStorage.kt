package com.retrohub.tv.detect

import android.content.Context
import android.content.pm.PackageManager
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.storage.StoragePaths
import java.io.File
import java.net.URLDecoder

/**
 * RetroArch's own "saf://" path format (libretro-common/vfs/vfs_implementation_saf.c):
 *
 *     saf://<tree URI with '%' -> %25 and '/' -> %2F>/<path inside the tree>
 *
 * Its VFS opens such paths through the folder grants RetroArch holds (Storage Access Framework),
 * which is the only way RetroArch's Google Play edition can read files outside its own folders.
 * Verified on a TCL Android 14 TV with com.retroarch.aarch64 1.22.2: a plain /storage path is
 * denied, while the saf:// form of the same game launches. Pure functions, unit-tested.
 */
object SafPaths {
    const val PREFIX = "saf://"

    fun isSaf(path: String): Boolean = path.startsWith(PREFIX) && path.length > PREFIX.length

    fun encodeTree(tree: String): String = tree.replace("%", "%25").replace("/", "%2F")

    private fun decodeTree(encoded: String): String = buildString {
        var i = 0
        while (i < encoded.length) {
            val c = encoded[i]
            if (c == '%' && i + 2 < encoded.length) {
                val hex = encoded.substring(i + 1, i + 3)
                val v = hex.toIntOrNull(16)
                if (v != null) { append(v.toChar()); i += 3; continue }
            }
            append(c); i++
        }
    }

    fun build(tree: String, relativePath: String): String = PREFIX + encodeTree(tree) + "/" + relativePath.trimStart('/')

    /** "saf://<enc tree>/<rel>" -> (tree URI, relative path), or null. */
    fun parse(saf: String): Pair<String, String>? {
        if (!isSaf(saf)) return null
        val rest = saf.substring(PREFIX.length)
        val slash = rest.indexOf('/')
        if (slash <= 0) return null
        return decodeTree(rest.substring(0, slash)) to rest.substring(slash + 1)
    }

    /**
     * Best guess of the real folder a tree URI points to. Works for Android's own provider
     * ("primary:Download") and for file-manager providers that embed the volume the same way
     * (e.g. Ultimate File Manager's "loc:primary/Download"). Null when unknown.
     */
    fun guessRoot(tree: String, primaryRoot: String): String? {
        val authority = tree.substringAfter("content://").substringBefore('/')
        val docId = runCatching { URLDecoder.decode(tree.substringAfter("/tree/", ""), "UTF-8") }.getOrNull() ?: return null
        if (docId.isEmpty()) return null
        StoragePaths.documentIdToPath(authority, docId, primaryRoot)?.let { return it }
        val m = Regex("(?:^|[:/])primary(?:[:/](.*))?$").find(docId) ?: return null
        val rel = m.groupValues[1].trim('/')
        return if (rel.isEmpty()) primaryRoot.trimEnd('/') else "${primaryRoot.trimEnd('/')}/$rel"
    }

    /**
     * Learns real-folder -> tree mappings from saf:// paths RetroArch has used (history,
     * playlists). A mapping is kept only when the file it implies really exists.
     */
    fun learnRoots(safPaths: Collection<String>, primaryRoot: String, exists: (String) -> Boolean): Map<String, String> {
        val out = HashMap<String, String>()
        for (p in safPaths) {
            val (tree, rel) = parse(p) ?: continue
            val root = guessRoot(tree, primaryRoot) ?: continue
            if (root !in out && exists("$root/$rel")) out[root] = tree
        }
        return out
    }

    /** saf:// path RetroArch can open for [realPath], using the closest folder it has access to. */
    fun forPath(realPath: String, roots: Map<String, String>): String? {
        val best = roots.keys.filter { realPath.startsWith("$it/") }.maxByOrNull { it.length } ?: return null
        return build(roots.getValue(best), realPath.removePrefix("$best/"))
    }

    /** Real path for a saf:// path, when its tree maps to a known folder. */
    fun toRealPath(saf: String, primaryRoot: String): String? {
        val (tree, rel) = parse(saf) ?: return null
        return guessRoot(tree, primaryRoot)?.let { "$it/$rel" }
    }
}

/** How a RetroArch install reaches files, and the folders it has been given. */
class RetroArchStorage(private val context: Context) {

    /**
     * True for builds without storage permissions (Google Play edition, targetSdk 30+): they can
     * only open files through folder grants, so games must be passed as saf:// paths.
     */
    fun needsFolderGrants(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val requested = context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty()
        STORAGE_PERMISSIONS.none { it in requested }
    }.getOrDefault(false)

    /** RetroArch's media folder: shared storage that RetroArch can always read and write. */
    fun mediaDir(packageName: String): File = File(StorageAccess.primaryRoot() + "/Android/media/$packageName")

    /** Every RetroArch playlist file RetroHub can read (Android/data is hidden on Android 11+). */
    fun playlistFiles(packageName: String): List<File> {
        val root = StorageAccess.primaryRoot()
        val dirs = listOf(
            File(mediaDir(packageName), "RetroArch/playlists"),
            File("$root/RetroArch/playlists"),
            File("$root/Android/data/$packageName/files/playlists"),
        )
        return dirs.filter { it.isDirectory }.flatMap { d ->
            (d.listFiles().orEmpty().toList() + d.listFiles { f -> f.isDirectory }.orEmpty().flatMap { it.listFiles().orEmpty().toList() })
                .filter { it.isFile && it.name.endsWith(".lpl") && it.length() < 32L * 1024 * 1024 }
        }
    }

    /** Folders RetroArch can open (real path -> tree URI), learned from its playlists and history. */
    fun knownFolders(packageName: String): Map<String, String> {
        val safPaths = playlistFiles(packageName).flatMap { f ->
            runCatching { Playlists.parse(f.readText()) }.getOrDefault(emptyList()).map { it.path }
        }.filter { SafPaths.isSaf(it) }
        return SafPaths.learnRoots(safPaths, StorageAccess.primaryRoot()) { File(Discovery.fileOnDisk(it)).isFile }
    }

    companion object {
        private val STORAGE_PERMISSIONS = setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
        )
    }
}

package com.retrohub.tv.scan

import com.retrohub.tv.i18n.tr
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import com.retrohub.tv.data.AppDatabase
import com.retrohub.tv.data.GameEntity
import com.retrohub.tv.data.SystemEntity
import com.retrohub.tv.storage.StorageAccess
import com.retrohub.tv.storage.StoragePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

data class ScanResult(val systemName: String, val found: Int, val removed: Int, val error: String? = null)

/** One file found on disk or through SAF. */
private data class Found(val name: String, val path: String?, val documentUri: String?, val size: Long)

/**
 * Foreground-only scanner (runs in the ViewModel scope, no services). Streams files in batches
 * of [BATCH] so memory stays flat no matter how big the collection is. Existing rows keep their
 * favourite / play history: new files are inserted, seen files are re-stamped, and rows whose
 * stamp did not change are removed at the end.
 */
class RomScanner(private val context: Context, private val db: AppDatabase) {

    suspend fun scan(system: SystemEntity, onProgress: (Int) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val extensions = StoragePaths.parseExtensions(system.extensions)
        if (system.romFolder.isBlank()) return@withContext ScanResult(system.name, 0, 0, tr("No ROM folder set"))
        if (extensions.isEmpty()) return@withContext ScanResult(system.name, 0, 0, tr("No file extensions set"))

        val stamp = System.currentTimeMillis()
        val art = loadArtwork(system.artworkFolder)
        val batch = ArrayList<Found>(BATCH)
        var total = 0

        suspend fun flush() {
            if (batch.isEmpty()) return
            val rows = batch.map { f ->
                val location = f.path ?: f.documentUri!!
                GameEntity(
                    systemId = system.id,
                    title = StoragePaths.titleFromFileName(f.name),
                    fileName = f.name,
                    path = f.path,
                    documentUri = f.documentUri,
                    location = location,
                    sizeBytes = f.size,
                    coverPath = art[StoragePaths.baseName(f.name).lowercase()],
                    scanStamp = stamp,
                )
            }
            db.withTransaction {
                db.games().insertIgnore(rows)
                rows.chunked(SQL_CHUNK).forEach { chunk ->
                    db.games().touch(system.id, chunk.map { it.location }, stamp)
                }
                if (art.isNotEmpty()) rows.forEach { db.games().setCover(system.id, it.location, it.coverPath) }
            }
            total += rows.size
            batch.clear()
            onProgress(total)
        }

        val error = try {
            val emit: suspend (Found) -> Unit = { f ->
                batch.add(f)
                if (batch.size >= BATCH) flush()
            }
            if (StoragePaths.isContentUri(system.romFolder)) {
                walkTree(Uri.parse(system.romFolder.trim()), extensions, system.recursive, emit)
            } else {
                walkFiles(File(system.romFolder.trim()), extensions, system.recursive, emit)
            }
            flush()
            null
        } catch (e: SecurityException) {
            tr("Permission denied: {0}", e.message ?: tr("grant storage access or pick the folder again"))
        } catch (e: ScanException) {
            e.message
        }

        if (error != null) return@withContext ScanResult(system.name, total, 0, error)
        // Safety net: an empty result usually means unreadable or unmounted storage (Android shows a
        // folder without permission as empty, not as an error). Never wipe a library because of that.
        if (total == 0 && db.games().countBySystem(system.id) > 0) {
            return@withContext ScanResult(
                system.name, 0, 0,
                tr("found no games in {0}. Nothing was removed — check that the storage is connected and readable.", system.romFolder),
            )
        }
        val removed = db.games().deleteStale(system.id, stamp)
        db.systems().setLastScan(system.id, stamp)
        ScanResult(system.name, total, removed)
    }

    private suspend fun walkFiles(root: File, ext: Set<String>, recursive: Boolean, emit: suspend (Found) -> Unit) {
        if (!root.isDirectory) throw ScanException(tr("Folder not found or not readable: {0}", root.path))
        // Without access, Android lists non-media files as missing instead of failing, so check first.
        if (!StorageAccess.hasBroadFileAccess(context)) {
            throw ScanException(tr("no storage access, so {0} can't be read. Grant access in Settings → Storage.", root.path))
        }
        val stack = ArrayDeque<Pair<File, Int>>().apply { add(root to 0) }
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val (dir, depth) = stack.removeLast()
            val children = dir.listFiles() ?: continue
            val games = ArrayList<File>()
            for (f in children) {
                if (f.name.startsWith('.')) continue
                if (f.isDirectory) {
                    if (recursive && depth < MAX_DEPTH) stack.add(f to depth + 1)
                } else if (StoragePaths.extensionOf(f.name) in ext) {
                    games.add(f)
                }
            }
            // One entry per disc game: hide .bin tracks behind their .cue, and discs listed in an .m3u.
            val visible = StoragePaths.visibleGameFiles(games.map { it.name }) { m3u -> readM3u(File(dir, m3u)) }.toHashSet()
            for (f in games) if (f.name in visible) emit(Found(f.name, f.absolutePath, null, f.length()))
        }
    }

    private suspend fun walkTree(tree: Uri, ext: Set<String>, recursive: Boolean, emit: suspend (Found) -> Unit) {
        if (!StorageAccess.hasTreePermission(context, tree.toString())) {
            throw ScanException(tr("Folder permission missing. Pick the folder again."))
        }
        val authority = tree.authority
        val primary = StorageAccess.primaryRoot()
        val stack = ArrayDeque<Pair<String, Int>>().apply { add(DocumentsContract.getTreeDocumentId(tree) to 0) }
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val (docId, depth) = stack.removeLast()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
            val pending = ArrayList<Found>()
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    if (name.startsWith('.')) continue
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (recursive && depth < MAX_DEPTH) stack.add(id to depth + 1)
                    } else if (StoragePaths.extensionOf(name) in ext) {
                        val size = if (c.isNull(3)) 0L else c.getLong(3)
                        pending.add(
                            Found(
                                name = name,
                                path = StoragePaths.documentIdToPath(authority, id, primary),
                                documentUri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                                size = size,
                            )
                        )
                    }
                }
            }
            // Emit after the cursor is closed so DB writes never overlap an open provider cursor.
            val visible = StoragePaths.visibleGameFiles(pending.map { it.name }) { m3u ->
                pending.firstOrNull { it.name == m3u }?.path?.let { readM3u(File(it)) }.orEmpty()
            }.toHashSet()
            pending.forEach { if (it.name in visible) emit(it) }
        }
    }

    /** base-name (lowercase) -> image path or content URI. Only the top level of the folder is read. */
    private fun loadArtwork(folder: String): Map<String, String> {
        val value = folder.trim()
        if (value.isEmpty()) return emptyMap()
        val out = HashMap<String, String>()
        fun accept(name: String, location: String) {
            if (StoragePaths.extensionOf(name) in IMAGE_EXT) {
                val key = StoragePaths.baseName(name).lowercase()
                if (key !in out) out[key] = location
            }
        }
        runCatching {
            if (StoragePaths.isContentUri(value)) {
                val tree = Uri.parse(value)
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                context.contentResolver.query(
                    children,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1) ?: continue
                        accept(name, DocumentsContract.buildDocumentUriUsingTree(tree, id).toString())
                    }
                }
            } else {
                File(value).listFiles()?.forEach { if (it.isFile) accept(it.name, it.absolutePath) }
            }
        }
        return out
    }

    private fun readM3u(f: File): List<String> =
        runCatching { if (f.length() < 64 * 1024) f.readLines().filter { it.isNotBlank() && !it.startsWith("#") } else emptyList() }
            .getOrDefault(emptyList())

    private class ScanException(message: String) : Exception(message)

    companion object {
        private const val BATCH = 200
        private const val SQL_CHUNK = 500 // below SQLite's 999 bound-variable limit on old devices
        private const val MAX_DEPTH = 6
        private val IMAGE_EXT = setOf("png", "jpg", "jpeg", "webp")
    }
}

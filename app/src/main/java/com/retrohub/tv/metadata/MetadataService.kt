package com.retrohub.tv.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.retrohub.tv.data.AppDatabase
import com.retrohub.tv.data.GameEntity
import com.retrohub.tv.data.MetadataStatus
import com.retrohub.tv.storage.StoragePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.CRC32
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

data class MetadataResult(val matched: Int, val notFound: Int, val errors: List<String>)

/**
 * Downloads metadata for installed games from libretro (no account needed):
 *  - details from libretro-database .rdb files (cached in app storage, refreshed every 30 days);
 *  - box art and screenshots from the libretro-thumbnails GitHub repositories.
 * Runs only while the app is open (no services). Images are downscaled and stored as WebP so a
 * 1 GB device never decodes full-size artwork.
 */
class MetadataService(private val context: Context, private val db: AppDatabase) {
    private val rdbDir = File(context.filesDir, "rdb")
    private val artDir = File(context.filesDir, "art")
    private val coverDir = File(artDir, "covers")
    private val snapDir = File(artDir, "snaps")

    fun isOurArtwork(path: String?): Boolean = path != null && path.startsWith(artDir.absolutePath)

    fun artworkBytes(): Long = artDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearArtwork() { artDir.deleteRecursively() }

    suspend fun run(
        systemIds: List<Long>?,
        retryNotFound: Boolean,
        downloadSnaps: Boolean,
        onProgress: (String) -> Unit,
    ): MetadataResult = withContext(Dispatchers.IO) {
        if (retryNotFound) db.games().resetNotFound()
        val systems = db.systems().all().filter { (systemIds == null || it.id in systemIds) && it.metadataDb.isNotBlank() }
        var matched = 0
        var notFound = 0
        val errors = ArrayList<String>()
        for (system in systems) {
            coroutineContext.ensureActive()
            val games = db.games().forMetadata(system.id, listOf(MetadataStatus.PENDING))
            if (games.isEmpty()) continue
            try {
                onProgress("${system.name}: reading checksums")
                val requests = games.map { g ->
                    coroutineContext.ensureActive()
                    val crc = g.crc ?: crcOf(g)?.also { db.games().setCrc(g.id, it) }
                    MatchRequest(g.id, g.fileName, crc)
                }
                onProgress("${system.name}: updating game database")
                val rdb = ensureDatabase(system.metadataDb)

                val matcher = MetadataMatcher(requests)
                openRdb(rdb).use { RdbReader(it).forEach(matcher::pass1) }
                val resolved = matcher.resolved()
                matcher.preparePass2(resolved)
                openRdb(rdb).use { RdbReader(it).forEach(matcher::pass2) }

                games.forEachIndexed { i, g ->
                    coroutineContext.ensureActive()
                    onProgress("${system.name}: artwork ${i + 1}/${games.size}")
                    val name = resolved[g.id]
                    if (name == null) {
                        db.games().setMetadataStatus(g.id, MetadataStatus.NOT_FOUND)
                        notFound++
                        return@forEachIndexed
                    }
                    val m = matcher.metadataFor(name)
                    db.games().setMetadata(
                        g.id, m.dbName, m.genre, m.developer, m.publisher, m.releaseYear, m.players, m.region,
                        "libretro", MetadataStatus.MATCHED,
                    )
                    matched++
                    // Never replace artwork the user supplied through an artwork folder.
                    if (g.coverPath == null || isOurArtwork(g.coverPath)) {
                        val out = File(coverDir, "${g.id}.webp")
                        val ok = fetchImage(Thumbnails.url(system.metadataDb, "Named_Boxarts", name), out, 300, 420) ||
                            fetchImage(Thumbnails.url(system.metadataDb, "Named_Titles", name), out, 300, 420)
                        if (ok) db.games().setCoverById(g.id, out.absolutePath)
                    }
                    if (downloadSnaps && (g.screenshotPath == null || isOurArtwork(g.screenshotPath))) {
                        val out = File(snapDir, "${g.id}.webp")
                        if (fetchImage(Thumbnails.url(system.metadataDb, "Named_Snaps", name), out, 640, 480)) {
                            db.games().setSnap(g.id, out.absolutePath)
                        }
                    }
                }
            } catch (e: IOException) {
                errors += "${system.name}: ${e.message ?: "network error"}"
            } catch (e: IllegalStateException) {
                errors += "${system.name}: ${e.message}"
            } catch (e: IllegalArgumentException) {
                errors += "${system.name}: ${e.message}"
            }
        }
        MetadataResult(matched, notFound, errors)
    }

    // ---------------- checksums ----------------

    /** CRC32 of the ROM. Zips use the stored CRC of their largest entry; files over 64 MB are skipped. */
    private fun crcOf(g: GameEntity): String? = runCatching {
        val path = g.path
        if (path != null) {
            val f = File(path)
            if (!f.isFile) return null
            if (StoragePaths.extensionOf(f.name) == "zip") {
                ZipFile(f).use { zip ->
                    val entry = zip.entries().asSequence().filter { !it.isDirectory }.maxByOrNull { it.size }
                    return entry?.crc?.takeIf { it >= 0 }?.let { "%08x".format(it) }
                }
            }
            if (f.length() > MAX_CRC_BYTES) return null
            FileInputStream(f).use { crcStream(it) }
        } else {
            val uri = g.documentUri ?: return null
            if (g.sizeBytes > MAX_CRC_BYTES) return null
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { crcStream(it) }
        }
    }.getOrNull()

    private fun crcStream(input: InputStream): String {
        val crc = CRC32()
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            crc.update(buf, 0, n)
        }
        return "%08x".format(crc.value)
    }

    // ---------------- database cache ----------------

    private fun ensureDatabase(name: String): File {
        rdbDir.mkdirs()
        val file = File(rdbDir, Thumbnails.sanitize(name) + ".rdb")
        val fresh = file.isFile && System.currentTimeMillis() - file.lastModified() < DB_MAX_AGE_MS
        if (fresh) return file
        try {
            val tmp = File(rdbDir, file.name + ".part")
            download(Thumbnails.rdbUrl(name), tmp)
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        } catch (e: IOException) {
            if (!file.isFile) throw e // keep using an older copy when offline
        }
        return file
    }

    private fun openRdb(f: File): InputStream = BufferedInputStream(FileInputStream(f), 64 * 1024)

    // ---------------- network ----------------

    private fun connect(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RetroHubTV/1.0")
        }

    private fun download(url: String, dest: File) {
        val c = connect(url)
        try {
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode} for database")
            c.inputStream.use { input -> dest.outputStream().use { input.copyTo(it, 64 * 1024) } }
        } finally {
            c.disconnect()
        }
    }

    /** Returns null for 404. */
    private fun getBytes(url: String, maxBytes: Int): ByteArray? {
        val c = connect(url)
        try {
            if (c.responseCode == 404) return null
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val len = c.contentLength
            if (len > maxBytes) return null
            return c.inputStream.use { it.readBytes() }.takeIf { it.size <= maxBytes }
        } finally {
            c.disconnect()
        }
    }

    // ---------------- images ----------------

    private fun fetchImage(url: String, out: File, maxW: Int, maxH: Int): Boolean = try {
        var bytes = getBytes(url, MAX_IMAGE_BYTES)
        // Some thumbnail entries are git symlinks: raw.githubusercontent serves the target path as text.
        if (bytes != null && !isPng(bytes) && bytes.size < 512) {
            val target = String(bytes, Charsets.UTF_8).trim()
            bytes = if (target.endsWith(".png")) {
                val resolved = URL(URL(url), target.split('/').joinToString("/") { seg ->
                    if (seg == ".." || seg == ".") seg else Thumbnails.encode(seg)
                })
                getBytes(resolved.toString(), MAX_IMAGE_BYTES)
            } else null
        }
        bytes != null && saveScaled(bytes, out, maxW, maxH)
    } catch (_: IOException) {
        false
    }

    private fun isPng(b: ByteArray) = b.size > 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte()

    private fun saveScaled(bytes: ByteArray, out: File, maxW: Int, maxH: Int): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxW && bounds.outHeight / (sample * 2) >= maxH) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return false
        val scale = minOf(1f, maxW.toFloat() / decoded.width, maxH.toFloat() / decoded.height)
        val bmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
                .also { if (it !== decoded) decoded.recycle() }
        } else decoded
        out.parentFile?.mkdirs()
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        val ok = out.outputStream().use { bmp.compress(format, 82, it) }
        bmp.recycle()
        return ok
    }

    companion object {
        private const val MAX_CRC_BYTES = 64L * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 6 * 1024 * 1024
        private const val DB_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }
}

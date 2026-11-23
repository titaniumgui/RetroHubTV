package com.retrohub.tv.metadata

/** A game that needs metadata. */
data class MatchRequest(val gameId: Long, val fileName: String, val crc: String?)

/** Merged metadata for one database title. */
data class GameMetadata(
    val dbName: String,
    val genre: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
    val players: Int? = null,
    val region: String? = null,
) {
    fun merge(e: RdbEntry) = copy(
        genre = genre ?: e.genre,
        developer = developer ?: e.developer,
        publisher = publisher ?: e.publisher,
        releaseYear = releaseYear ?: e.releaseYear,
        players = players ?: e.users,
        region = region ?: e.region,
    )
}

/**
 * Two-pass matching over a streamed database (pure, unit-tested):
 *  pass 1: CRC match first, then exact file name, then the name without "(...)"/"[...]" tags
 *          (preferring World/USA/Europe releases);
 *  pass 2: merge every entry that shares a matched name or CRC, because libretro splits one title's
 *          fields across several entries (e.g. headered + headerless dumps, metadata-only entries).
 */
class MetadataMatcher(requests: List<MatchRequest>) {
    private val byCrc = HashMap<String, MutableList<Long>>()
    private val byExact = HashMap<String, MutableList<Long>>()
    private val byLoose = HashMap<String, MutableList<Long>>()

    private val crcHit = HashMap<Long, String>()      // gameId -> dbName (strongest)
    private val exactHit = HashMap<Long, String>()
    private val looseHit = HashMap<Long, Pair<Int, String>>() // gameId -> (rank, dbName)

    init {
        for (r in requests) {
            r.crc?.lowercase()?.let { byCrc.getOrPut(it) { ArrayList() }.add(r.gameId) }
            val base = r.fileName.substringBeforeLast('.', r.fileName)
            byExact.getOrPut(normalize(base)) { ArrayList() }.add(r.gameId)
            byLoose.getOrPut(stripTags(base)) { ArrayList() }.add(r.gameId)
        }
    }

    fun pass1(e: RdbEntry) {
        val name = e.name ?: return
        e.crc?.let { crc -> byCrc[crc]?.forEach { crcHit[it] = name } }
        byExact[normalize(name)]?.forEach { exactHit.putIfAbsentCompat(it, name) }
        byLoose[stripTags(name)]?.forEach { id ->
            val rank = regionRank(name)
            val cur = looseHit[id]
            if (cur == null || rank < cur.first) looseHit[id] = rank to name
        }
    }

    /** gameId -> database name, best match per game. Call after pass 1. */
    fun resolved(): Map<Long, String> {
        val out = HashMap<Long, String>()
        looseHit.forEach { (id, v) -> out[id] = v.second }
        exactHit.forEach { (id, n) -> out[id] = n }
        crcHit.forEach { (id, n) -> out[id] = n }
        return out
    }

    private var wantedNames: Set<String> = emptySet()
    private var crcToName: Map<String, String> = emptyMap()
    private val merged = HashMap<String, GameMetadata>()

    fun preparePass2(resolved: Map<Long, String>) {
        wantedNames = resolved.values.toSet()
        crcToName = byCrc.entries.mapNotNull { (crc, ids) -> resolved[ids.first()]?.let { crc to it } }.toMap()
    }

    fun pass2(e: RdbEntry) {
        val target = e.name?.takeIf { it in wantedNames } ?: e.crc?.let { crcToName[it] } ?: return
        merged[target] = (merged[target] ?: GameMetadata(target)).merge(e)
    }

    fun metadataFor(dbName: String): GameMetadata = merged[dbName] ?: GameMetadata(dbName)

    companion object {
        fun normalize(s: String): String =
            s.replace('_', ' ').lowercase().replace(Regex("\\s+"), " ").trim()

        /** "Super Mario Bros. (World) [!]" -> "super mario bros." */
        fun stripTags(s: String): String =
            normalize(s.replace(Regex("\\s*[(\\[][^)\\]]*[)\\]]"), ""))

        private val unwanted = listOf("Virtual Console", "Beta", "Proto", "Demo", "Sample", "Hack", "Pirate", "Aftermarket", "Unl", "Switch Online", "Collection")

        /** Lower is better: preferred region, then the plainest release (fewest tags, no re-releases/betas). */
        fun regionRank(name: String): Int {
            val region = when {
                "(World)" in name -> 0
                "(USA" in name -> 1
                "(Europe" in name -> 2
                "(Japan" in name -> 4
                else -> 3
            }
            val tags = Regex("[(\\[]").findAll(name).count()
            val penalty = unwanted.count { it in name }
            return penalty * 100 + region * 10 + tags
        }

        private fun <K, V> HashMap<K, V>.putIfAbsentCompat(k: K, v: V) { if (!containsKey(k)) put(k, v) }
    }
}

/** libretro-thumbnails naming (as RetroArch does it). */
object Thumbnails {
    private const val BASE = "https://raw.githubusercontent.com/libretro-thumbnails/"

    /** Characters RetroArch replaces with '_' in thumbnail file names. */
    fun sanitize(name: String): String = name.map { if (it in "&*/:`<>?\\|\"") '_' else it }.joinToString("")

    fun repo(db: String): String = db.replace(' ', '_')

    fun url(db: String, type: String, dbName: String): String =
        BASE + encode(repo(db)) + "/master/" + type + "/" + encode(sanitize(dbName)) + ".png"

    /** Percent-encodes everything except RFC 3986 unreserved characters. */
    fun encode(s: String): String = buildString {
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if ((c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) || (c in '0'.code..'9'.code) || c.toChar() in "-._~") {
                append(c.toChar())
            } else {
                append('%').append("%02X".format(c))
            }
        }
    }

    fun rdbUrl(db: String): String =
        "https://raw.githubusercontent.com/libretro/libretro-database/master/rdb/" + encode(db) + ".rdb"
}

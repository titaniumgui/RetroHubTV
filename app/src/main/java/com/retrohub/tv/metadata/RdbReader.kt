package com.retrohub.tv.metadata

import java.io.EOFException
import java.io.InputStream

/** The fields RetroHub uses from a libretro-database entry. */
data class RdbEntry(
    val name: String?,
    val crc: String?,
    val genre: String?,
    val developer: String?,
    val publisher: String?,
    val releaseYear: Int?,
    val users: Int?,
    val region: String?,
)

/**
 * Streaming reader for libretro .rdb files: "RARCHDB\0" + 8-byte offset, then one MessagePack map
 * per entry until a nil (0xC0) marks the metadata block. Only interesting keys are materialised;
 * everything else is skipped, so memory use is constant regardless of database size.
 */
class RdbReader(private val input: InputStream) {

    fun forEach(block: (RdbEntry) -> Unit) {
        val magic = ByteArray(8).also { readFully(it) }
        require(String(magic, Charsets.US_ASCII).startsWith("RARCHDB")) { "Not a libretro database" }
        skipBytes(8) // metadata offset
        while (true) {
            val t = input.read()
            if (t == -1 || t == 0xC0) return
            val size = mapSize(t) ?: throw IllegalStateException("Unexpected type 0x${t.toString(16)}")
            var name: String? = null; var crc: String? = null; var genre: String? = null
            var developer: String? = null; var publisher: String? = null; var year: Int? = null
            var users: Int? = null; var region: String? = null
            repeat(size) {
                val key = readKey()
                when (key) {
                    "name" -> name = readString()
                    "crc" -> crc = readBinHex()
                    "genre" -> genre = readString()
                    "developer" -> developer = readString()
                    "publisher" -> publisher = readString()
                    "releaseyear" -> year = readInt()
                    "users" -> users = readInt()
                    "region" -> region = readString()
                    else -> skipValue()
                }
            }
            block(RdbEntry(name, crc, genre, developer, publisher, year, users, region))
        }
    }

    // ---------------- MessagePack primitives ----------------

    private fun mapSize(t: Int): Int? = when (t) {
        in 0x80..0x8F -> t and 0x0F
        0xDE -> readUInt(2).toInt()
        0xDF -> readUInt(4).toInt()
        else -> null
    }

    private fun readKey(): String? = readString()

    private fun readString(): String? {
        val t = read()
        val len = strLen(t) ?: run { skipValueOfType(t); return null }
        val bytes = ByteArray(len).also { readFully(it) }
        return String(bytes, Charsets.UTF_8)
    }

    private fun strLen(t: Int): Int? = when (t) {
        in 0xA0..0xBF -> t and 0x1F
        0xD9 -> readUInt(1).toInt()
        0xDA -> readUInt(2).toInt()
        0xDB -> readUInt(4).toInt()
        else -> null
    }

    private fun readBinHex(): String? {
        val t = read()
        val len = when (t) {
            0xC4 -> readUInt(1).toInt()
            0xC5 -> readUInt(2).toInt()
            0xC6 -> readUInt(4).toInt()
            else -> { skipValueOfType(t); return null }
        }
        val bytes = ByteArray(len).also { readFully(it) }
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private fun readInt(): Int? {
        val t = read()
        return when (t) {
            in 0x00..0x7F -> t
            0xCC -> readUInt(1).toInt()
            0xCD -> readUInt(2).toInt()
            0xCE -> readUInt(4).toInt()
            0xCF -> readUInt(8).toInt()
            0xD0 -> readUInt(1).toByte().toInt()
            0xD1 -> readUInt(2).toShort().toInt()
            0xD2 -> readUInt(4).toInt()
            0xD3 -> readUInt(8).toInt()
            else -> { skipValueOfType(t); null }
        }
    }

    private fun skipValue() = skipValueOfType(read())

    private fun skipValueOfType(t: Int) {
        when (t) {
            in 0x00..0x7F, in 0xE0..0xFF, 0xC0, 0xC2, 0xC3 -> Unit
            in 0x80..0x8F -> repeat((t and 0x0F) * 2) { skipValue() }
            in 0x90..0x9F -> repeat(t and 0x0F) { skipValue() }
            in 0xA0..0xBF -> skipBytes((t and 0x1F).toLong())
            0xC4, 0xD9 -> skipBytes(readUInt(1))
            0xC5, 0xDA -> skipBytes(readUInt(2))
            0xC6, 0xDB -> skipBytes(readUInt(4))
            0xCC, 0xD0 -> skipBytes(1)
            0xCD, 0xD1 -> skipBytes(2)
            0xCE, 0xD2, 0xCA -> skipBytes(4)
            0xCF, 0xD3, 0xCB -> skipBytes(8)
            0xDC -> repeat(readUInt(2).toInt()) { skipValue() }
            0xDD -> repeat(readUInt(4).toInt()) { skipValue() }
            0xDE -> repeat(readUInt(2).toInt() * 2) { skipValue() }
            0xDF -> repeat(readUInt(4).toInt() * 2) { skipValue() }
            else -> throw IllegalStateException("Unsupported MessagePack type 0x${t.toString(16)}")
        }
    }

    private fun read(): Int = input.read().also { if (it == -1) throw EOFException() }

    private fun readUInt(bytes: Int): Long {
        var v = 0L
        repeat(bytes) { v = (v shl 8) or read().toLong() }
        return v
    }

    private fun readFully(buf: ByteArray) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) throw EOFException()
            off += n
        }
    }

    private fun skipBytes(n: Long) {
        var left = n
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) { read(); left-- } else left -= skipped
        }
    }
}

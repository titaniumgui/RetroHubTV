package com.retrohub.tv.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined platform. Nothing here is guessed: ROM folder, core name/path and BIOS
 * location all come from the user (presets only pre-fill a display name, file extensions and
 * the libretro database name used for metadata).
 */
@Entity(tableName = "systems")
data class SystemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Filesystem path (e.g. /storage/emulated/0/ROMs/snes) or a SAF tree URI (content://...). */
    val romFolder: String = "",
    /** Comma separated, without dots: "sfc,smc,zip". */
    val extensions: String = "",
    /** RetroArch core base name, e.g. "snes9x" -> <coreDir>/snes9x_libretro_android.so */
    val coreName: String = "",
    /** Optional full core path; wins over [coreName] when set. */
    val corePath: String = "",
    /** Folder RetroArch's System/BIOS directory points to. Used for verification only. */
    val biosFolder: String = "",
    /** Comma separated BIOS file names that must exist in [biosFolder]. */
    val biosFiles: String = "",
    /** Optional folder with cover images named like the ROM (<rom base name>.png/.jpg/.webp). */
    val artworkFolder: String = "",
    val recursive: Boolean = true,
    val sortOrder: Int = 0,
    val lastScan: Long = 0,
    /** libretro-database name, e.g. "Nintendo - Super Nintendo Entertainment System". Blank = no metadata. */
    @ColumnInfo(defaultValue = "") val metadataDb: String = "",
)

object MetadataStatus {
    const val PENDING = 0
    const val MATCHED = 1
    const val NOT_FOUND = 2
}

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = SystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["systemId", "location"], unique = true),
        Index(value = ["lastPlayed"]),
        Index(value = ["favorite"]),
    ],
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemId: Long,
    val title: String,
    val fileName: String,
    /** Real filesystem path. This is what RetroArch receives in the ROM extra. */
    val path: String?,
    /** SAF document URI when the game was found through a picked folder. */
    val documentUri: String?,
    /** Stable unique key: path when known, otherwise the document URI. */
    val location: String,
    val sizeBytes: Long = 0,
    // Legacy v1 columns. Per-profile favourites/history live in game_stats since schema v2.
    val favorite: Boolean = false,
    val lastPlayed: Long? = null,
    val playCount: Int = 0,
    /** Per-game core: a core name ("mgba") or a full .so path. Blank = use the system's core. */
    val coreOverride: String = "",
    // ---- Metadata / artwork ----
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val releaseYear: Int? = null,
    val genre: String? = null,
    val players: Int? = null,
    val coverPath: String? = null,
    val screenshotPath: String? = null,
    val metadataSource: String? = null,
    val scanStamp: Long = 0,
    /** CRC32 of the ROM (hex, lower case), used to match libretro-database entries. */
    val crc: String? = null,
    /** Canonical name from the metadata database (No-Intro / Redump naming). */
    val dbName: String? = null,
    val region: String? = null,
    @ColumnInfo(defaultValue = "0") val metadataStatus: Int = MetadataStatus.PENDING,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Favourites and play history for one profile. */
@Entity(
    tableName = "game_stats",
    primaryKeys = ["profileId", "gameId"],
    foreignKeys = [
        ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GameEntity::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["profileId", "lastPlayed"]),
        Index(value = ["profileId", "favorite"]),
    ],
)
data class GameStatsEntity(
    val profileId: Long,
    val gameId: Long,
    val favorite: Boolean = false,
    val lastPlayed: Long? = null,
    val playCount: Int = 0,
)

/** Lightweight projection for rows and grids: avoids loading full rows for every tile. */
data class GameTile(
    val id: Long,
    val systemId: Long,
    val title: String,
    val coverPath: String?,
    val screenshotPath: String?,
    val genre: String?,
    val releaseYear: Int?,
    val favorite: Boolean,
    val lastPlayed: Long?,
)

data class SystemWithCount(
    @Embedded val system: SystemEntity,
    @ColumnInfo(name = "gameCount") val gameCount: Int,
)

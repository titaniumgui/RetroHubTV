package com.retrohub.tv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {
    @Query(
        "SELECT s.*, (SELECT COUNT(*) FROM games g WHERE g.systemId = s.id) AS gameCount " +
            "FROM systems s ORDER BY s.sortOrder, s.name COLLATE NOCASE"
    )
    fun observeWithCounts(): Flow<List<SystemWithCount>>

    @Query("SELECT * FROM systems ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun all(): List<SystemEntity>

    @Query("SELECT * FROM systems WHERE id = :id")
    suspend fun byId(id: Long): SystemEntity?

    @Query("SELECT * FROM systems WHERE id = :id")
    fun observe(id: Long): Flow<SystemEntity?>

    @Insert
    suspend fun insert(system: SystemEntity): Long

    @Update
    suspend fun update(system: SystemEntity)

    @Delete
    suspend fun delete(system: SystemEntity)

    @Query("UPDATE systems SET lastScan = :time WHERE id = :id")
    suspend fun setLastScan(id: Long, time: Long)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt, id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt, id")
    suspend fun all(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun byId(id: Long): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)
}

/*
 * Tile queries join the current profile's stats. Columns are listed explicitly so Room maps
 * them onto the small GameTile projection.
 */
private const val TILE_COLUMNS =
    "g.id AS id, g.systemId AS systemId, g.title AS title, g.coverPath AS coverPath, " +
        "g.screenshotPath AS screenshotPath, g.genre AS genre, g.releaseYear AS releaseYear, " +
        "COALESCE(s.favorite, 0) AS favorite, s.lastPlayed AS lastPlayed"
private const val TILE_FROM = "FROM games g LEFT JOIN game_stats s ON s.gameId = g.id AND s.profileId = :profileId"

@Dao
interface GameDao {
    @Query("SELECT $TILE_COLUMNS $TILE_FROM WHERE s.lastPlayed IS NOT NULL ORDER BY s.lastPlayed DESC LIMIT :limit")
    fun observeRecent(profileId: Long, limit: Int): Flow<List<GameTile>>

    @Query("SELECT $TILE_COLUMNS $TILE_FROM WHERE s.favorite = 1 ORDER BY g.title COLLATE NOCASE LIMIT :limit")
    fun observeFavorites(profileId: Long, limit: Int): Flow<List<GameTile>>

    @Query("SELECT $TILE_COLUMNS $TILE_FROM WHERE g.systemId = :systemId ORDER BY g.title COLLATE NOCASE")
    fun observeBySystem(profileId: Long, systemId: Long): Flow<List<GameTile>>

    @Query("SELECT $TILE_COLUMNS $TILE_FROM WHERE g.systemId = :systemId AND s.favorite = 1 ORDER BY g.title COLLATE NOCASE")
    fun observeFavoritesBySystem(profileId: Long, systemId: Long): Flow<List<GameTile>>

    @Query("SELECT $TILE_COLUMNS $TILE_FROM ORDER BY g.title COLLATE NOCASE")
    fun observeAll(profileId: Long): Flow<List<GameTile>>

    @Query("SELECT $TILE_COLUMNS $TILE_FROM WHERE s.favorite = 1 ORDER BY g.title COLLATE NOCASE")
    fun observeAllFavorites(profileId: Long): Flow<List<GameTile>>

    /** [pattern] is a LIKE pattern with '\' as escape character. */
    @Query(
        "SELECT $TILE_COLUMNS $TILE_FROM WHERE g.title LIKE :pattern ESCAPE '\\' OR g.dbName LIKE :pattern ESCAPE '\\' " +
            "ORDER BY CASE WHEN g.title LIKE :prefix ESCAPE '\\' THEN 0 ELSE 1 END, g.title COLLATE NOCASE LIMIT :limit"
    )
    fun search(profileId: Long, pattern: String, prefix: String, limit: Int): Flow<List<GameTile>>

    @Query("SELECT COUNT(*) FROM games")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE systemId = :systemId")
    suspend fun countBySystem(systemId: Long): Int

    @Query("SELECT * FROM games WHERE id = :id")
    fun observe(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun byId(id: Long): GameEntity?

    // ---------------- per-profile stats ----------------
    @Query("SELECT * FROM game_stats WHERE profileId = :profileId AND gameId = :gameId")
    fun observeStats(profileId: Long, gameId: Long): Flow<GameStatsEntity?>

    @Query("INSERT OR IGNORE INTO game_stats (profileId, gameId, favorite, playCount) VALUES (:profileId, :gameId, 0, 0)")
    suspend fun ensureStats(profileId: Long, gameId: Long)

    @Query("UPDATE game_stats SET favorite = :favorite WHERE profileId = :profileId AND gameId = :gameId")
    suspend fun setFavorite(profileId: Long, gameId: Long, favorite: Boolean)

    @Query("UPDATE game_stats SET lastPlayed = :time, playCount = playCount + 1 WHERE profileId = :profileId AND gameId = :gameId")
    suspend fun markPlayed(profileId: Long, gameId: Long, time: Long)

    @Query("UPDATE game_stats SET lastPlayed = NULL WHERE profileId = :profileId")
    suspend fun clearRecent(profileId: Long)

    // ---------------- scanning ----------------
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(games: List<GameEntity>): List<Long>

    @Query("UPDATE games SET scanStamp = :stamp WHERE systemId = :systemId AND location IN (:locations)")
    suspend fun touch(systemId: Long, locations: List<String>, stamp: Long)

    @Query("UPDATE games SET coverPath = :cover WHERE systemId = :systemId AND location = :location")
    suspend fun setCover(systemId: Long, location: String, cover: String?)

    @Query("DELETE FROM games WHERE systemId = :systemId AND scanStamp != :stamp")
    suspend fun deleteStale(systemId: Long, stamp: Long): Int

    @Query("UPDATE games SET coreOverride = :core WHERE id = :id")
    suspend fun setCoreOverride(id: Long, core: String)

    // ---------------- metadata ----------------
    @Query("SELECT * FROM games WHERE systemId = :systemId AND metadataStatus IN (:statuses)")
    suspend fun forMetadata(systemId: Long, statuses: List<Int>): List<GameEntity>

    @Query("UPDATE games SET crc = :crc WHERE id = :id")
    suspend fun setCrc(id: Long, crc: String?)

    @Query(
        "UPDATE games SET dbName = :dbName, genre = :genre, developer = :developer, publisher = :publisher, " +
            "releaseYear = :year, players = :players, region = :region, metadataSource = :source, " +
            "metadataStatus = :status WHERE id = :id"
    )
    suspend fun setMetadata(
        id: Long, dbName: String?, genre: String?, developer: String?, publisher: String?,
        year: Int?, players: Int?, region: String?, source: String?, status: Int,
    )

    @Query("UPDATE games SET metadataStatus = :status WHERE id = :id")
    suspend fun setMetadataStatus(id: Long, status: Int)

    @Query("UPDATE games SET coverPath = :cover WHERE id = :id")
    suspend fun setCoverById(id: Long, cover: String?)

    @Query("UPDATE games SET screenshotPath = :snap WHERE id = :id")
    suspend fun setSnap(id: Long, snap: String?)

    @Query("UPDATE games SET metadataStatus = 0 WHERE metadataStatus = 2")
    suspend fun resetNotFound()

    /** Forgets downloaded images (paths under [prefix]) and marks every game for a fresh metadata pass. */
    @Query(
        "UPDATE games SET " +
            "coverPath = CASE WHEN coverPath LIKE :prefix THEN NULL ELSE coverPath END, " +
            "screenshotPath = CASE WHEN screenshotPath LIKE :prefix THEN NULL ELSE screenshotPath END, " +
            "metadataStatus = 0"
    )
    suspend fun clearDownloadedArtwork(prefix: String)
}

package com.retrohub.tv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SystemEntity::class, GameEntity::class, ProfileEntity::class, GameStatsEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun systems(): SystemDao
    abstract fun games(): GameDao
    abstract fun profiles(): ProfileDao

    companion object {
        const val DEFAULT_PROFILE_ID = 1L

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "retrohub.db")
                // WAL keeps UI reads responsive while a scan writes.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        insertDefaultProfile(db)
                    }
                })
                .build()

        private fun insertDefaultProfile(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO profiles (id, name, colorIndex, createdAt) VALUES (?, 'Player 1', 0, ?)",
                arrayOf<Any>(DEFAULT_PROFILE_ID, System.currentTimeMillis()),
            )
        }

        /** v1 → v2: profiles, per-profile stats (existing history moves to "Player 1"), metadata columns. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE systems ADD COLUMN metadataDb TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE games ADD COLUMN crc TEXT")
                db.execSQL("ALTER TABLE games ADD COLUMN dbName TEXT")
                db.execSQL("ALTER TABLE games ADD COLUMN region TEXT")
                db.execSQL("ALTER TABLE games ADD COLUMN metadataStatus INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, colorIndex INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS game_stats (profileId INTEGER NOT NULL, gameId INTEGER NOT NULL, " +
                        "favorite INTEGER NOT NULL, lastPlayed INTEGER, playCount INTEGER NOT NULL, " +
                        "PRIMARY KEY(profileId, gameId), " +
                        "FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(gameId) REFERENCES games(id) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_stats_gameId ON game_stats (gameId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_stats_profileId_lastPlayed ON game_stats (profileId, lastPlayed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_stats_profileId_favorite ON game_stats (profileId, favorite)")
                insertDefaultProfile(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO game_stats (profileId, gameId, favorite, lastPlayed, playCount) " +
                        "SELECT $DEFAULT_PROFILE_ID, id, favorite, lastPlayed, playCount FROM games " +
                        "WHERE favorite = 1 OR lastPlayed IS NOT NULL OR playCount > 0"
                )
            }
        }
    }
}

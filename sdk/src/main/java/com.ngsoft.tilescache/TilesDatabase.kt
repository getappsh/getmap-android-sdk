package com.ngsoft.tilescache

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ngsoft.tilescache.converters.TimeStampConverter
import com.ngsoft.tilescache.dao.MapDAO
import com.ngsoft.tilescache.dao.TilesDAO
import com.ngsoft.tilescache.models.MapPkg
import com.ngsoft.tilescache.models.TilePkg

@Database(
    version = 11,
    entities = [TilePkg::class, MapPkg::class],
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
    ]
    , exportSchema = true
)
@TypeConverters(TimeStampConverter::class)
abstract class TilesDatabase : RoomDatabase() {

    companion object {
        @Volatile private var INSTANCE: TilesDatabase? = null
        fun getInstance(ctx: Context) : TilesDatabase {
            synchronized(this){
                var instance = INSTANCE
                
                if (instance == null){
                    instance =  Room.databaseBuilder(ctx, TilesDatabase::class.java, "tiles-DB")
                        //no migration support currently. 4 migration see:
                        //https://developer.android.com/training/data-storage/room/migrating-db-versions
                        .addMigrations(MIGRATION_4_5, MIGRATION_8_9, MIGRATION_10_11)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE MapPkg SET flowState = 'IMPORT_DELIVERY' WHERE flowState = 'IMPORT_DELIVERY_STATUS'")

            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE MapPkg RENAME COLUMN statusMessage TO statusMsg");
                database.execSQL("ALTER TABLE MapPkg RENAME COLUMN errorContent TO statusDescr");

            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE MapPkg_temp (
                pId TEXT NOT NULL,
                bBox TEXT NOT NULL,
                footprint TEXT,
                flowState TEXT NOT NULL,
                reqId TEXT,
                JDID INTEGER,
                MDID INTEGER,
                state TEXT NOT NULL,
                statusMsg TEXT NOT NULL,
                fileName TEXT,
                jsonName TEXT,
                url TEXT,
                path TEXT,
                downloadProgress INTEGER NOT NULL,
                statusDescr TEXT,
                cancelDownload INTEGER NOT NULL,
                downloadStart INTEGER,
                downloadStop INTEGER,
                downloadDone INTEGER,
                isUpdated INTEGER NOT NULL DEFAULT 1,
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                validationAttempt INTEGER NOT NULL,
                connectionAttempt INTEGER NOT NULL DEFAULT 0,
                mapAttempt INTEGER NOT NULL DEFAULT 0,
                mapDone INTEGER NOT NULL DEFAULT 0,
                jsonAttempt INTEGER NOT NULL DEFAULT 0,
                jsonDone INTEGER NOT NULL DEFAULT 0,
                reqDate INTEGER DEFAULT (strftime('%s', 'now')) NOT NULL
            )
        """.trimIndent())

                // Step 2: Copy data from the original table to the temporary table
                database.execSQL("""
            INSERT INTO MapPkg_temp (
                pId, bBox, footprint, flowState, reqId, JDID, MDID, state, statusMsg,
                fileName, jsonName, url, path, downloadProgress, statusDescr,
                cancelDownload, downloadStart, downloadStop, downloadDone,
                isUpdated, id, validationAttempt, connectionAttempt, mapAttempt,
                mapDone, jsonAttempt, jsonDone, reqDate
            )
            SELECT
                pId, bBox, footprint, flowState, reqId, JDID, MDID, state, statusMsg,
                fileName, jsonName, url, path, downloadProgress, statusDescr,
                cancelDownload, downloadStart, downloadStop, downloadDone,
                isUpdated, id, validationAttempt, connectionAttempt, mapAttempt,
                mapDone, jsonAttempt, jsonDone,
                COALESCE(downloadStart, downloadStop, downloadDone, strftime('%s', 'now'))
            FROM MapPkg
        """.trimIndent())
                // Step 3: Drop the original table
                database.execSQL("DROP TABLE MapPkg")

                // Step 4: Rename the temporary table to the original table name
                database.execSQL("ALTER TABLE MapPkg_temp RENAME TO MapPkg")
            }
        }
    }

    abstract fun tilesDao(): TilesDAO

    abstract fun mapDap(): MapDAO

}
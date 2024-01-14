package com.ngsoft.tilescache

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ngsoft.tilescache.convertes.TimeStampConverter
import com.ngsoft.tilescache.dao.MapDAO
import com.ngsoft.tilescache.dao.TilesDAO
import com.ngsoft.tilescache.models.MapPkg
import com.ngsoft.tilescache.models.TilePkg

@Database(
    version = 7,
    entities = [TilePkg::class, MapPkg::class],
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7)
    ]
    //, exportSchema = false
)
@TypeConverters(TimeStampConverter::class)
abstract class TilesDatabase : RoomDatabase() {

    companion object {
        fun connect(ctx: Context) : TilesDatabase {
            return Room.databaseBuilder(ctx, TilesDatabase::class.java, "tiles-DB")
                //no migration support currently. 4 migration see:
                //https://developer.android.com/training/data-storage/room/migrating-db-versions
                .addMigrations(MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE MapPkg SET flowState = 'IMPORT_DELIVERY' WHERE flowState = 'IMPORT_DELIVERY_STATUS'")

            }
        }
    }

    abstract fun tilesDao(): TilesDAO

    abstract fun mapDap(): MapDAO

}
package com.ngsoft.tilescache

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ngsoft.tilescache.convertes.TimeStampConverter
import com.ngsoft.tilescache.dao.MapDAO
import com.ngsoft.tilescache.dao.TilesDAO
import com.ngsoft.tilescache.models.MapPkg
import com.ngsoft.tilescache.models.TilePkg

@Database(
    version = 4,
    entities = [TilePkg::class, MapPkg::class],
    autoMigrations = [
        AutoMigration(from = 3, to = 4)
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
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    abstract fun tilesDao(): TilesDAO

    abstract fun mapDap(): MapDAO

}
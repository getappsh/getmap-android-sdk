package com.ngsoft.tilescache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TilePkg::class], version = 1
    //, exportSchema = false
)
abstract class TilesDatabase : RoomDatabase() {

    companion object {

        //@JvmStatic
        fun connect(ctx: Context) : TilesDatabase {
            return Room.databaseBuilder(ctx, TilesDatabase::class.java, "tiles-DB")

                //no migration support currently. 4 migration see:
                //https://developer.android.com/training/data-storage/room/migrating-db-versions
                .fallbackToDestructiveMigration()

                .build()
        }
    }

    abstract fun tilesDao(): TilesDAO

}
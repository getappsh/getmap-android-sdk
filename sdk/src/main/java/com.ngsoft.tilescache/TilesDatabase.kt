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
    version = 10,
    entities = [TilePkg::class, MapPkg::class],
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 9, to = 10),
    ]
    //, exportSchema = false
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
                        .addMigrations(MIGRATION_4_5, MIGRATION_8_9)
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
    }

    abstract fun tilesDao(): TilesDAO

    abstract fun mapDap(): MapDAO

}
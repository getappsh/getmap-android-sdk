package com.ngsoft.tilescache

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ngsoft.tilescache.models.TilePkg
import java.time.LocalDateTime

internal class TilesCache(ctx: Context)  {
    private val _tag = "TilesCache"
    private val db: TilesDatabase
    private val dao: TilesDAO
    init {
        Log.d(_tag,"TilesCache init...")
        db = TilesDatabase.connect(ctx)
        dao = db.tilesDao()
    }

    fun purge(){
        //val tiles = dao.getAll()
        //tiles.forEach{ t -> println(t) }

        dao.nukeTable()
        //reset auto-increments
        db.runInTransaction { db.query(SimpleSQLiteQuery("DELETE FROM sqlite_sequence")) }
    }

    fun registerTilePkg(tilePkg: TilePkg) {
        dao.insert(tilePkg)
    }

    fun isTileInCache(prodName: String, x: Int, y: Int, zoom: Int, updDate: LocalDateTime) : Boolean {
        val tilePackages = dao.getByTile(x, y, zoom)
        tilePackages.forEach{ tilePkg ->
            if (tilePkg.prodName == prodName && isTilePkgUpdateDateValid(tilePkg, updDate))
                return true
        }
        return false
    }

    private fun isTilePkgUpdateDateValid(tilePkg: TilePkg, updDate: LocalDateTime): Boolean {
        return tilePkg.dateUpdated >= updDate
        //return tilePkg.dateUpdated.toEpochSecond(ZoneOffset.UTC) >= updDate.toEpochSecond(ZoneOffset.UTC)
    }

}
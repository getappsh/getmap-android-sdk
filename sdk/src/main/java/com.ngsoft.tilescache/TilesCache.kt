package com.ngsoft.tilescache

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ngsoft.tilescache.dao.TilesDAO
import com.ngsoft.tilescache.models.BBox
import com.ngsoft.tilescache.models.Tile
import com.ngsoft.tilescache.models.TilePkg
import com.ngsoft.tilescache.models.TilePkgUpdate
import java.time.LocalDateTime

internal class TilesCache(ctx: Context)  {
    private val _tag = "TilesCache"
    private val db: TilesDatabase
    private val dao: TilesDAO
    init {
        Log.d(_tag,"TilesCache init...")
        db = TilesDatabase.getInstance(ctx)
        dao = db.tilesDao()
    }

    fun purge(){
        //val tiles = dao.getAll()
        //tiles.forEach{ t -> println(t) }

        dao.nukeTable()
        //reset auto-increments
        db.runInTransaction { db.query(SimpleSQLiteQuery("DELETE FROM sqlite_sequence")) }
    }

    fun registerTilePkg(prodName: String, fileName: String, tile: Tile, bBox: BBox, updDate: LocalDateTime) {
        val found = getTileInCache(prodName, tile.x, tile.y, tile.zoom)
        if(found != null){
            dao.update(TilePkgUpdate(id = found.id, fileName = fileName, dateUpdated = updDate, dateCached = LocalDateTime.now()))
            return
        }

        dao.insert(TilePkg(prodName = prodName, fileName = fileName, tile = tile, bBox = bBox,
            dateCreated = LocalDateTime.now(), dateUpdated = updDate, dateCached = LocalDateTime.now()))
    }

    fun getTileInCache(prodName: String, x: Int, y: Int, zoom: Int) : TilePkg? {
        val tilePackages = dao.getByTile(x, y, zoom)
        return tilePackages.find{ it.prodName == prodName}
    }

    fun isTileInCache(prodName: String, x: Int, y: Int, zoom: Int, updDate: LocalDateTime) : Boolean {
        val tilePackages = dao.getByTile(x, y, zoom)
        val found = tilePackages.find{ it.prodName == prodName }
        if(found != null)
            return isTilePkgUpdateDateValid(found, updDate)

        return false
    }

    private fun isTilePkgUpdateDateValid(tilePkg: TilePkg, updDate: LocalDateTime): Boolean {
        return tilePkg.dateUpdated >= updDate
        //return tilePkg.dateUpdated.toEpochSecond(ZoneOffset.UTC) >= updDate.toEpochSecond(ZoneOffset.UTC)
    }

}
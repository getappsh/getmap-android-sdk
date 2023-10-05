package com.ngsoft.tilescache

import android.content.Context
import com.ngsoft.tilescache.models.TilePkg

class TilesCache(ctx: Context)  {

    private val dao: TilesDAO
    init {
        println("TilesCache init...")
        val db = TilesDatabase.connect(ctx)
        dao = db.tilesDao()
    }

    fun registerTilePkg(tilePkg: TilePkg) {
        dao.insert(tilePkg)
    }

}
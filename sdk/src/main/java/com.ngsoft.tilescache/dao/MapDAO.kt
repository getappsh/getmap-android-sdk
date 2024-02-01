package com.ngsoft.tilescache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import com.ngsoft.tilescache.models.MapPkg

@Dao
interface MapDAO {
    @Query("SELECT * FROM MapPkg")
    fun getAll(): List<MapPkg>

    @Query("SELECT * FROM MapPkg WHERE id = :id")
    fun getById(id: Int): MapPkg?

    @Insert
    fun insert(map: MapPkg): Long
    @Update
    fun update(mapPkg: MapPkg)

    @Query("SELECT * FROM MapPkg WHERE reqId = :reqId")
    fun getByReqId(reqId: String): MapPkg?

    @Query("UPDATE MapPkg SET isUpdated = :isUpdate WHERE reqId = :reqId")
    fun setUpdatedByReqId(reqId: String, isUpdate: Boolean)

    @Transaction
    fun updateAndReturn(mapPkg: MapPkg): MapPkg? {
        update(mapPkg)
        return getById(mapPkg.id)
    }

    @Query("UPDATE MapPkg set mapDone = COALESCE(:mapDone, mapDone), jsonDone = COALESCE(:jsonDone, jsonDone), fileName = COALESCE(:fileName, fileName), jsonName = COALESCE(:jsonName, jsonName) where id = :id")
    fun updateFileDone(id: String, mapDone: Boolean?=null, fileName: String?=null, jsonDone: Boolean?=null, jsonName: String?=null)
    @Query("DELETE FROM MapPkg WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE fileName = :name)")
    fun doesMapFileExist(name: String): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE jsonName = :name)")
    fun doesJsonFileExist(name: String): Boolean

    @Query("DELETE FROM MapPkg")
    fun nukeTable()
}
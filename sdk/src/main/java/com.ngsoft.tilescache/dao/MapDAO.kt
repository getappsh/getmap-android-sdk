package com.ngsoft.tilescache.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.tilescache.models.DeliveryFlowState

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

    @Query("DELETE FROM MapPkg WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE fileName = :name)")
    fun doesMapFileExist(name: String): Boolean

    @Query("SELECT EXISTS (SELECT 1 FROM MapPkg WHERE jsonName = :name)")
    fun doesJsonFileExist(name: String): Boolean

    @Query("DELETE FROM MapPkg")
    fun nukeTable()

    @Query("UPDATE MapPkg SET " +
            "reqId = COALESCE(:reqId, reqId), " +
            "JDID = COALESCE(:JDID, JDID), " +
            "MDID = COALESCE(:MDID, MDID), " +
            "fileName = COALESCE(:fileName, fileName), " +
            "jsonName = COALESCE(:jsonName, jsonName), " +
            "url = COALESCE(:url, url), " +
            "flowState = COALESCE(:flowState, flowState), " +
            "statusMsg = COALESCE(:statusMsg, statusMsg), " +
            "downloadProgress = COALESCE(:downloadProgress, downloadProgress), " +
            "statusDescr = COALESCE(:statusDescr, statusDescr), " +
            "cancelDownload = CASE WHEN :state = 'CANCEL' AND cancelDownload = 1 THEN 0 ELSE COALESCE(:cancelDownload, cancelDownload) END, "+
            "footprint = COALESCE(:footprint, footprint), "+
            "isUpdated = COALESCE(:isUpdated, isUpdated), "+
            "validationAttempt = COALESCE(:validationAttempt, validationAttempt), " +
            "connectionAttempt = COALESCE(:connectionAttempt, connectionAttempt), " +
            "mapAttempt = COALESCE(:mapAttempt, mapAttempt), " +
            "mapDone = COALESCE(:mapDone, mapDone), " +
            "jsonAttempt = COALESCE(:jsonAttempt, jsonAttempt), " +
            "jsonDone = COALESCE(:jsonDone, jsonDone), " +
            "downloadStop = CASE " +
            "WHEN :state = 'CANCEL' AND cancelDownload = 1 THEN strftime('%s', 'now')  " +
            "WHEN (:state = 'CANCEL' OR :state = 'PAUSE' OR :state = 'ERROR') AND state NOT IN ('CANCEL', 'PAUSE', 'ERROR') THEN strftime('%s', 'now')  " +
            "ELSE downloadStop " +
            "END, " +
            "state = COALESCE(:state, state), " +
            "downloadStart = CASE " +
            "WHEN :state = 'CONTINUE' THEN strftime('%s', 'now')  " +
            "WHEN :state IN ('START', 'CONTINUE', 'DOWNLOAD') AND downloadStart IS NULL THEN strftime('%s', 'now')  " +
            "ELSE downloadStart " +
            "END, " +
            "downloadDone = CASE " +
            "WHEN :state = 'DONE' THEN strftime('%s', 'now')  " +
            "ELSE downloadDone " +
            "END " +
            "WHERE id = :id")
    fun updateMapFields(
        id: String,
        reqId: String?=null,
        JDID: Long?=null,
        MDID: Long?=null,
        state: MapDeliveryState?=null,
        flowState: DeliveryFlowState?=null,
        statusMsg: String?=null,
        fileName: String?=null,
        jsonName: String?=null,
        url: String?=null,
        downloadProgress: Int?=null,
        statusDescr: String?=null,
        validationAttempt: Int?=null,
        connectionAttempt: Int?=null,
        mapAttempt: Int?=null,
        mapDone: Boolean?=null,
        jsonAttempt: Int?=null,
        jsonDone: Boolean?=null,
        footprint: String?=null,
        isUpdated: Boolean?=null,
        cancelDownload: Boolean?=null
    )
}
package com.ngsoft.getapp.sdk

class GetMapServiceImpl private constructor() : GetMapService {
    fun init(configuration: Configuration?, statusCode: Status?): Boolean {
        // Implementation of init method can be added here.
        return false
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = CreateMapImportStatus()
        status.importRequestId = inputImportRequestId
        status.statusCode = Status()
        status.statusCode!!.statusCode = StatusCode.SUCCESS
        status.state = MapImportState.START
        return status
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        if(inputProperties == null)
            throw Exception("invalid inputProperties")

        val status = CreateMapImportStatus()
        status.statusCode = Status()
        status.statusCode!!.statusCode = StatusCode.SUCCESS
        status.state = MapImportState.IN_PROGRESS
        return status
    }

    override fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.state = MapDeliveryState.CONTINUE
        return status
    }

    override fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.state = MapDeliveryState.START
        return status
    }

    override fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.state = MapDeliveryState.PAUSE
        return status
    }

    override fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus? {
        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")

        val status = MapImportDeliveryStatus()
        status.state = MapDeliveryState.CANCEL
        return status
    }

    override fun setMapImportDeploy(
        inputImportRequestId: String?,
        inputState: MapDeployState?
    ): MapDeployState? {

        if(inputImportRequestId.isNullOrEmpty())
            throw Exception("invalid inputImportRequestId")


        return MapDeployState.DONE
    }

    companion object {
        var instance: GetMapServiceImpl? = null
            get() {
                if (field == null) {
                    field = GetMapServiceImpl()
                }
                return field
            }
            private set
    }
}
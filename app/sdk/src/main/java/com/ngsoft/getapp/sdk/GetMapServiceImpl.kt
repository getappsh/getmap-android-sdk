package com.ngsoft.getapp.sdk

class GetMapServiceImpl private constructor() : GetMapService {
    fun init(configuration: Configuration?, statusCode: Status?): Boolean {
        // Implementation of init method can be added here.
        return false
    }

    override fun getCreateMapImportStatus(inputImportRequestId: String?): CreateMapImportStatus? {
        // Implementation of getCreateMapImportStatus method can be added here.
        return null
    }

    override fun createMapImport(inputProperties: MapProperties?): CreateMapImportStatus? {
        // Implementation of createMapImport method can be added here.
        return null
    }

    override fun getMapImportDeliveryStatus(inputImportRequestId: String?): MapImportDeliveryStatus? {
        // Implementation of getMapImportDeliveryStatus method can be added here.
        return null
    }

    override fun setMapImportDeliveryStart(inputImportRequestId: String?): MapImportDeliveryStatus? {
        // Implementation of setMapImportDeliveryStart method can be added here.
        return null
    }

    override fun setMapImportDeliveryPause(inputImportRequestId: String?): MapImportDeliveryStatus? {
        // Implementation of setMapImportDeliveryPause method can be added here.
        return null
    }

    override fun setMapImportDeliveryCancel(inputImportRequestId: String?): MapImportDeliveryStatus? {
        // Implementation of setMapImportDeliveryCancel method can be added here.
        return null
    }

    override fun setMapImportDeploy(
        inputImportRequestId: String?,
        inputState: MapDeployState?
    ): MapDeployState? {
        // Implementation of setMapImportDeploy method can be added here.
        return null
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
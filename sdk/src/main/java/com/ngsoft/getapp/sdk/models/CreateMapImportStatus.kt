package com.ngsoft.getapp.sdk.models

class CreateMapImportStatus {
    var importRequestId: String? = null
    var state: MapImportState? = null
    var statusCode: Status? = null //TODO add packageUrl, filename and createDate?
}
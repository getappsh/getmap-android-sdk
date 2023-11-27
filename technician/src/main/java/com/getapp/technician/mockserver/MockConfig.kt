package com.getapp.technician.mockserver

import GetApp.Client.models.CreateImportResDto

data class MockConfig (
    var discoveryPath: String? = null,
    var mapPath: String? = null,
    var jsonPath: String? = null,
    var importCreateStatus: CreateImportResDto.Status =  CreateImportResDto.Status.inProgress,
    var discoveryTimeOut: Int = 0,
    var failedValidation: Boolean = false,
    var fastDownload: Boolean = true
)

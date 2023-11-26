package com.getapp.technician.mockserver

data class MockConfig (
    var discoveryTimeOut: Int = 0,
    var failedValidation: Boolean = false,
    var fastDownload: Boolean = true
)

package com.ngsoft.getapp.sdk

/**
 * Configuration
 *
 * @property baseUrl GetApp service address
 * @property user
 * @property password
 * @property storagePath for tiles
 * @property zoomLevel matrix grid zoom level
 * @property deliveryTimeout in minutes
 * @property downloadTimeout in minutes
 * @constructor Create empty Configuration
 */
data class Configuration (
    val baseUrl: String,
    val user: String,
    val password: String,
    val storagePath: String,
    val zoomLevel: Int,
    val deliveryTimeout: Int,
    val downloadTimeout: Int,
    val imei: String?,
    val downloadRetry: Int = 2,
    )
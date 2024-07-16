package com.example.getmap.matomo.provider

import java.util.Date

data class Report(
    val id: Long,
    val type: VariantReport,
    val path: String? = null,
    val title: String? = null,
    val category: String? = null,
    val action: String? = null,
    val name: String? = null,
    val value: Float? = null,
    val dimId: Int? = null,
    val dimValue: String? = null,
    val createdAt: Date? = null
)

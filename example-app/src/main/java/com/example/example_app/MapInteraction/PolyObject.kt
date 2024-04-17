package com.example.example_app.MapInteraction

import java.time.OffsetDateTime
data class Product(
    val date: OffsetDateTime?,
    val intersection: Double,
    val start: String,
    val end: String
)
data class PolyObject(
    val area: Double,
    val strPolygon: String,
    val inCollision: Boolean,
    val product: Product?
)

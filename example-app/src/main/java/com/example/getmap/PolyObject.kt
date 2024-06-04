package com.example.getmap

import com.arcgismaps.geometry.Geometry
import java.math.BigDecimal
import java.time.OffsetDateTime

class PolyObject(val date: OffsetDateTime?, val intersection: Double, val start: String, val end: String, val resolution: BigDecimal, val geometry: Geometry)

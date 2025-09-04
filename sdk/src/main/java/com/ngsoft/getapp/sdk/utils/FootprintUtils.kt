package com.ngsoft.getapp.sdk.utils

import org.json.JSONObject

internal object FootprintUtils {
    @Throws(Exception::class)
    fun toString(footprint: JSONObject): String {
        val type = footprint.getString("type")
        val coordinatesArray = footprint.getJSONArray("coordinates")
        val coordinatesString = StringBuilder()

        when (type) {
            "Polygon" -> {
                val ringArray = coordinatesArray.getJSONArray(0)
                for (i in 0 until ringArray.length()) {
                    val pointArray = ringArray.getJSONArray(i)
                    val longitude = pointArray.getDouble(0)
                    val latitude = pointArray.getDouble(1)
                    coordinatesString.append("$longitude,$latitude,")
                }
            }
            "MultiPolygon" -> {
                for (i in 0 until coordinatesArray.length()) {
                    val polygonArray = coordinatesArray.getJSONArray(i)
                    val ringArray = polygonArray.getJSONArray(0)
                    for (j in 0 until ringArray.length()) {
                        val pointArray = ringArray.getJSONArray(j)
                        val longitude = pointArray.getDouble(0)
                        val latitude = pointArray.getDouble(1)
                        coordinatesString.append("$longitude,$latitude,")
                    }
                }
            }
            else -> throw Exception("Unsupported footprint geometry type: $type")
        }

        if (coordinatesString.isNotEmpty()) {
            coordinatesString.setLength(coordinatesString.length - 1)
        }

        return coordinatesString.toString()
    }

    fun toList(footprint: String): List<Double> = footprint.split(",").map { it.toDouble() }
}

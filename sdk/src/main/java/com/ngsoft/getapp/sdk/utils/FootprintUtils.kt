package com.ngsoft.getapp.sdk.utils

import android.util.Log
import org.json.JSONObject

internal object FootprintUtils {

    fun toString(footprint: JSONObject): String {
        return try {
            val type = footprint.getString("type")
            val coordinatesArray = footprint.getJSONArray("coordinates")
            val coordinatesString = StringBuilder()

            if (type == "Polygon") {
                val ringArray = coordinatesArray.getJSONArray(0)
                for (i in 0 until ringArray.length()) {
                    val pointArray = ringArray.getJSONArray(i)
                    val longitude = pointArray.getDouble(0)
                    val latitude = pointArray.getDouble(1)
                    coordinatesString.append("$longitude,$latitude,")
                }
            } else if (type == "MultiPolygon") {
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
            } else {
                Log.e("FootprintUtils", "Unknown geometry type: $type")
                return ""
            }

            if (coordinatesString.isNotEmpty()) {
                coordinatesString.deleteCharAt(coordinatesString.length - 1)
            }

            return coordinatesString.toString()

        } catch (e: Exception) {
            Log.e("FootprintUtils", "Error parsing footprint: ${e.message}", e)
            ""
        }
    }


    fun toList(footprint: String): List<Double> = footprint.split(",").map { it.toDouble() }

}
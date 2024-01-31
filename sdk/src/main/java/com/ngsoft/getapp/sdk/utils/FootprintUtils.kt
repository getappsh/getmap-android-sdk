package com.ngsoft.getapp.sdk.utils

import org.json.JSONObject

internal object FootprintUtils {

    fun toString(footprint: JSONObject): String{
        val coordinatesArray = footprint
            .getJSONArray("coordinates")
            .getJSONArray(0) // Assuming there's only one set of coordinates in the example

        val coordinatesString = StringBuilder()

        for (i in 0 until coordinatesArray.length()) {
            val innerArray = coordinatesArray.getJSONArray(i)
            for (j in 0 until innerArray.length()) {
                val pointArray = innerArray.getJSONArray(j)
                val longitude = pointArray.getDouble(0)
                val latitude = pointArray.getDouble(1)

                coordinatesString.append("$longitude,$latitude,")
            }
        }

        // Remove the trailing comma
        coordinatesString.deleteCharAt(coordinatesString.length - 1)
        return coordinatesString.toString()
    }

    fun toList(footprint: String): List<Double> = footprint.split(",").map { it.toDouble() }

}
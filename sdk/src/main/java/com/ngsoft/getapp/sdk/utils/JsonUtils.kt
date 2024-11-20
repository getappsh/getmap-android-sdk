package com.ngsoft.getapp.sdk.utils

import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Paths

internal object JsonUtils {


    fun writeJson(jsonPath: String, jsonObject: JSONObject) {
        Files.write(Paths.get(jsonPath), jsonObject.toString().toByteArray())
    }
    fun readJson(jsonPath: String): JSONObject{
        val file = Files.readAllBytes(Paths.get(jsonPath))
        return JSONObject(String(file))
    }

    fun getStringOrThrow(key: String, jsonPath: String): String{
        val jsonObject = readJson(jsonPath);
        return jsonObject.getString(key)
    }

    fun JSONObject.getStringOrNull(key: String): String?{
        return if(this.has(key) && !this.isNull(key)) this.getString(key) else null

    }
}
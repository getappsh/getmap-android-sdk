package com.ngsoft.getapp.sdk.utils

import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Paths

object JsonUtils {

    fun readJson(jsonPath: String): JSONObject{
        val file = Files.readAllBytes(Paths.get(jsonPath))
        return JSONObject(String(file))
    }

    fun getStringOrThrow(key: String, jsonPath: String): String{
        val jsonObject = readJson(jsonPath);
        return jsonObject.getString(key)
    }
}
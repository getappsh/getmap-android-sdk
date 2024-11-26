package com.ngsoft.getapp.sdk.exceptions

class MapAlreadyExistsException(val id: String, message: String) : Exception("Map with ID '$id' already exists.")
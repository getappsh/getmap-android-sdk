package com.ngsoft.getapp.sdk.exceptions

class MapAlreadyExistsException(val id: String) : Exception("Map with ID '$id' already exists.")
package com.example.getmap

import gov.nasa.worldwind.geom.Position

data class FourScreenPoints(
    val leftTop: Position,
    val rightBottom: Position,
    val rightTop: Position,
    val leftBottom: Position
)
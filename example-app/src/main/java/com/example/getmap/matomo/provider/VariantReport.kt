package com.example.getmap.matomo.provider

sealed class VariantReport {
    class Event(val value: String) : VariantReport()
    class Screen(val value: String) : VariantReport()

    override fun toString(): String {
        return when (this) {
            is Event -> "Event:$value"
            is Screen -> "Screen:$value"
        }
    }

    companion object {
        fun fromString(value: String): VariantReport {
            val parts = value.split(":")
            return when (parts[0]) {
                "Event" -> Event(parts[1])
                "Screen" -> Screen(parts[1])
                else -> throw IllegalArgumentException("Unknown VariantReport type")
            }
        }
    }
}

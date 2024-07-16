package com.example.getmap.matomo.provider

import java.util.Date

data class Report(
    val id: Long,
    val type: VariantReportEnum,
    val path: String? = null,
    val title: String? = null,
    val category: String? = null,
    val action: String? = null,
    val name: String? = null,
    val value: Float? = null,
    val dimId: Int? = null,
    val dimValue: String? = null,
    val createdAt: Date? = null
)


enum class VariantReportEnum {
    Event,
    Screen;

    companion object {
        fun fromString(typeString: String): VariantReportEnum {
            return when (typeString) {
                "Event" -> Event
                "Screen" -> Screen
                else -> throw IllegalArgumentException("Unknown type_enum value: $typeString")
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            Event -> "Event"
            Screen -> "Screen"
        }
    }
}

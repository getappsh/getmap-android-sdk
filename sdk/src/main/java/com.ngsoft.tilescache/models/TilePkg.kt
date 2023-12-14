package com.ngsoft.tilescache.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ngsoft.tilescache.convertes.TimeStampConverter
import java.time.LocalDateTime


@Entity
data class TilePkg(
    //@ColumnInfo(name = "prod_name")
    val prodName: String,
    val fileName: String,

    @Embedded
    val tile: Tile,

    @Embedded
    val bBox: BBox,

    @TypeConverters(TimeStampConverter::class)
    val dateCreated: LocalDateTime,

    @TypeConverters(TimeStampConverter::class)
    val dateUpdated: LocalDateTime,

    @TypeConverters(TimeStampConverter::class)
    val dateCached: LocalDateTime

){
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

data class TilePkgUpdate (
    val id: Int,
    val fileName: String,
    val dateUpdated: LocalDateTime,
    val dateCached: LocalDateTime
)
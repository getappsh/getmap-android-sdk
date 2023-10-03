package com.ngsoft.tilescache.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ngsoft.tilescache.TimeStampConverter
import java.time.LocalDateTime

@Entity
data class TilePkg(
    //@ColumnInfo(name = "prod_name")
    val prodName: String?,

    val fileName: String?,

    @Embedded
    val tile: Tile?,

    @Embedded
    val bBox: BBox?,

    @TypeConverters(TimeStampConverter::class)
    var dateCreated: LocalDateTime?,

    @TypeConverters(TimeStampConverter::class)
    var dateCached: LocalDateTime?

){
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

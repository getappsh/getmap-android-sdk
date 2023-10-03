package com.ngsoft.tilescache

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ngsoft.tilescache.BBox
import java.time.LocalDate

@Entity
data class TilePkg(
    //@ColumnInfo(name = "prod_name")
    val prodName: String?,

    val fileName: String?,

    @Embedded
    val bBox: BBox?,

    val zoom: Int?

    //, var dateCreated: LocalDate? = LocalDate.now()

){
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}

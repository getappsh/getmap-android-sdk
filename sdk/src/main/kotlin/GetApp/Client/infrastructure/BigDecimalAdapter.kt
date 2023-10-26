package GetApp.Client.infrastructure

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.math.BigDecimal

class BigDecimalAdapter {
    @ToJson
    //fun toJson(value: BigDecimal): String {
    fun toJson(value: BigDecimal): Double {
        //return value.toPlainString()
        return value.toDouble()
    }

    @FromJson
    //fun fromJson(value: String): BigDecimal {
    fun fromJson(value: Double): BigDecimal {
        return BigDecimal(value)
    }
}

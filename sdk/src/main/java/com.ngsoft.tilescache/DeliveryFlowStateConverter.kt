import androidx.room.TypeConverter
import com.ngsoft.tilescache.models.DeliveryFlowState

internal class DeliveryFlowStateConverter {

    @TypeConverter
    fun fromDeliveryFlowState(value: String) = enumValueOf<DeliveryFlowState>(value)
    @TypeConverter
    fun toDeliveryFlowState(value: DeliveryFlowState) = value.name
}

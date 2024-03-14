import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class MapDataMetaData {
    var id: String = ""
    var type: String = ""
    var classification: String = ""
    var productName: String = ""
    var description: String = ""
    var srsId: String = ""
    var producerName: String = ""
    var creationDate: String = ""
    var ingestionDate: String = ""
    var updateDate: String = ""
    var sourceDateStart: String = ""
    var sourceDateEnd: String = ""
    var minHorizontalAccuracyCE90: Double = -1.0
    var sensors: Array<String> = arrayOf()
    var region: Array<String> = arrayOf()
    var productId: String = ""
    var productVersion: String = ""
    var productType: String = ""
    var maxResolutionDeg: Double = -1.0
    var maxResolutionMeter: Double = -1.0
    lateinit var footprint: JvmType.Object
    lateinit var bbox: Array<Long>
    var productBoundingBox: String = ""
    var displayPath: String = ""
    var transparency: String = ""
    var tileOutputFormat: String = ""
    var sha256: String = ""
    var includedInBests: Array<String> = arrayOf()

}
import com.example.completecontentprovider.VariantReport

data class Report(
    val id: Long,
    val type: VariantReport,
    val path: String?,
    val title: String?,
    val category: String?,
    val action: String?,
    val name: String?,
    val value: Float?,
    val dimId: Int?,
    val dimValue: String?
)

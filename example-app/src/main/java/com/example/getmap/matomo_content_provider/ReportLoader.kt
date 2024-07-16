import android.content.Context
import androidx.loader.content.CursorLoader
import com.example.completecontentprovider.ReportProvider

class ReportLoader(context: Context) : CursorLoader(
    context,
    ReportProvider.CONTENT_URI,
    null,
    null,
    null,
    null
)

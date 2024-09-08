package com.example.getmap


import MapDataMetaData
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RotateDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.getmap.matomo.MatomoTracker
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.time.Duration


class DownloadListAdapter(
    private val onButtonClick: (Int, String, Any?) -> Unit,
    private val manager: MapServiceManager,
    private val context: Context,
) :
    RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {


    private var notifValidation: Toast? = null
    var availableUpdate: Boolean = false
    private var tracker: Tracker = MatomoTracker.getTracker(context)

    //Create and define the signal listener
    interface SignalListener {
        fun onSignalSpace()
        fun onSignalDownload()
    }


    //    @RequiresApi(Build.VERSION_CODES.R)
//    TODO Way is it needed?
    private val pathAvailable = "" /* manager.service.config.storagePath */

    private val listeners = mutableListOf<SignalListener>()

    fun addListener(listener: SignalListener) {
        listeners.add(listener)
    }

    fun triggerSpaceSignal() {
        for (listener in listeners) {
            listener.onSignalSpace()
        }
    }

    // Méthode pour déclencher le signal 2 avec des données
    fun triggerDownloadSignal() {
        for (listener in listeners) {
            listener.onSignalDownload()
        }
    }
    var region = ""

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textFileName: TextView = itemView.findViewById(R.id.textFileName)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val dates: TextView = itemView.findViewById(R.id.dates)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val percentage: TextView = itemView.findViewById(R.id.Percentages)
        val btnCancelResume: Button = itemView.findViewById(R.id.btnCancelResume)
        val btnDelete: AppCompatImageButton = itemView.findViewById(R.id.btnDelete)
        val btnQRCode: Button = itemView.findViewById(R.id.btnQRCode)
        val updated: ImageView = itemView.findViewById(R.id.updated_signal)
        val demandDate: TextView = itemView.findViewById(R.id.demand_date)
        val size: TextView = itemView.findViewById(R.id.size)
        val product: TextView = itemView.findViewById(R.id.product)
        val sizeLayout: LinearLayout = itemView.findViewById(R.id.size_layout)
        val separator: TextView = itemView.findViewById(R.id.lign_separator)
    }


    private val diffUtil = object : DiffUtil.ItemCallback<MapData>() {
        override fun areItemsTheSame(oldItem: MapData, newItem: MapData): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: MapData, newItem: MapData): Boolean {
            return oldItem.toString() == newItem.toString()
        }

    }
    private val asyncListDiffer = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_download, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val downloadData = asyncListDiffer.currentList[position]
        Log.i(
            "vsdnhilofherszofhezofezhioflezhfiollzefhzuofhezuofhezjofgdszuikzerf",
            "onBindViewHolder: ${downloadData.jsonName}"
        )

        val directory = File(
            //path for olar
//            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + downloadData.jsonName
//            "sdcard/Documents" +
//                    File.separator
            downloadData.path ?: ""
        )
        var geo = File(downloadData.path, downloadData.fileName ?: "")

        //Take the 3 letters that identify bbox
        val jsonFile = downloadData.getJson()
        if (jsonFile != null) {

            val endName = downloadData.fileName?.substringAfterLast('_')?.substringBefore('Z') + "Z"
            val jsonText = Gson().fromJson(jsonFile.toString(), MapDataMetaData::class.java)
            region = jsonText.region[0]
            holder.size.text = occupiedSpace(geo)
            holder.product.text =
                "תוצר: ${jsonText.id.subSequence(jsonText.id.length - 4, jsonText.id.length)}"
            deliveryDate(manager, downloadData, holder)
            holder.textFileName.text = "${region} ${endName}"
            val startDate = jsonText.sourceDateStart.substringBefore('T')
            val endDate = jsonText.sourceDateEnd.substringBefore('T')
            var startDateFormatted = formatDate(startDate)
            var endDateFormatted = formatDate(endDate)
            val tsoulam = "צולם: "
            if (endDateFormatted == startDateFormatted) {
                holder.dates.text = "${tsoulam}${endDateFormatted}"
            } else {
                holder.dates.text = "${tsoulam}${endDateFormatted} - ${startDateFormatted}"
            }

        } else {
            val sdf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
            val stopDate = downloadData.downloadStop
            val startDate = downloadData.reqDate
            if (downloadData.statusMsg == "בהורדה" || downloadData.statusMsg == "בקשה בהפקה" || downloadData.statusMsg == "בקשה נשלחה") {
                val a = sdf.format(startDate)
                holder.demandDate.text = "תאריך בקשה: ${a}"
            }
            if (stopDate != null && downloadData.statusMsg == "בוטל" || downloadData.statusMsg == "ההורדה נכשלה") {
                val a = sdf.format(stopDate)
                holder.demandDate.text = "תאריך עצירה: ${a}"
            }
        }

        holder.textStatus.text = downloadData.statusMsg
        holder.progressBar.progress = downloadData.progress

        holder.percentage.text = downloadData.progress.toString() + "%"

        holder.btnCancelResume.visibility = View.VISIBLE
        holder.btnCancelResume.isEnabled = true

        when (downloadData.deliveryState) {
            START -> {
                val localDateTime: LocalDateTime = LocalDateTime.now()
                val oneSecondBeforeLocalDateTime: LocalDateTime =
                    localDateTime.minus(Duration.ofSeconds(1))
                if (downloadData.downloadStart!!.toLocalDateTime()
                        .isAfter(oneSecondBeforeLocalDateTime)
                ) {
                    TrackHelper.track().dimension(
                        manager.service.config.matomoSiteId.toInt(),
                        downloadData.footprint
                    ).event("מיפוי ענן", "ניהול בקשות").name(" הורדת בול")
                        .with(tracker)
                }
                holder.sizeLayout.visibility = View.GONE
//                deliveryDate(manager, downloadData, holder)
                holder.btnDelete.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.textFileName.visibility = View.INVISIBLE
                holder.dates.visibility = View.INVISIBLE
                holder.btnCancelResume.visibility = View.INVISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
                holder.size.visibility = View.INVISIBLE
                holder.product.visibility = View.INVISIBLE
                holder.separator.visibility = View.INVISIBLE
                updateProgressBarColor(holder.progressBar, R.color.green, R.color.loadEmpty)
            }

            DONE -> {
                val localDateTime: LocalDateTime = LocalDateTime.now()
                val oneSecondBeforeLocalDateTime: LocalDateTime =
                    localDateTime.minus(Duration.ofSeconds(1))
                val name = region + "-" + downloadData.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                val coordinates = downloadData.footprint
                if (downloadData.downloadDone!!.toLocalDateTime()
                        .isAfter(oneSecondBeforeLocalDateTime)
                ) {
                    TrackHelper.track()
                        .dimension(manager.service.config.matomoDimensionId.toInt(), name)
                        .dimension(manager.service.config.matomoDimensionId.toInt(), coordinates)
                        .event("מיפוי ענן", "ניהול בקשות").name("בול הורד בהצלחה")

                        .with(tracker)
                }
                holder.sizeLayout.visibility = View.VISIBLE
                holder.percentage.visibility = View.GONE
                holder.textStatus.visibility = View.GONE
                holder.textFileName.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.dates.visibility = View.VISIBLE
                holder.btnCancelResume.visibility = View.GONE
                holder.progressBar.progress = 0
                triggerSpaceSignal()
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.VISIBLE
                holder.size.visibility = View.VISIBLE
                holder.product.visibility = View.VISIBLE
                holder.separator.visibility = View.VISIBLE
                updateProgressBarColor(holder.progressBar, R.color.loadEmpty, R.color.loadEmpty)

            }

            ERROR -> {
                val name = region + "-" + downloadData.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                TrackHelper.track().dimension(manager.service.config.matomoDimensionId.toInt(), name).event("מיפוי ענן", "ניהול שגיאות").name("ההורדה נכשלה").with(tracker)
                holder.textFileName.text = "ההורדה נכשלה"
                holder.dates.visibility = View.GONE
                holder.btnDelete.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.dates.text = LocalDate.now().toString()
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
                holder.sizeLayout.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
                updateProgressBarColor(holder.progressBar, R.color.red, R.color.light_red)
            }

            CANCEL -> {
                holder.dates.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
                holder.textStatus.text = "בוטל: ההורדה תמשיך מנקודת העצירה"
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
                holder.textFileName.text = "ההורדה בוטלה"
                holder.size.visibility = View.INVISIBLE
                holder.product.visibility = View.INVISIBLE
                holder.separator.visibility = View.INVISIBLE
                holder.dates.visibility = View.INVISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.sizeLayout.visibility = View.GONE
                updateProgressBarColor(holder.progressBar, R.color.blue, R.color.light_blue)
            }

            PAUSE -> {
                holder.textFileName.text = "ההורדה נעצרה"
                holder.textStatus.text = "נעצר: ההורדה תמשיך מנקודת העצירה"
                holder.btnDelete.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.textStatus.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
                holder.sizeLayout.visibility = View.GONE
                holder.dates.visibility = View.GONE
                updateProgressBarColor(holder.progressBar, R.color.blue, R.color.light_blue)
            }

            CONTINUE -> {
                holder.btnDelete.visibility = View.GONE
                holder.percentage.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
                holder.size.visibility = View.INVISIBLE
                holder.product.visibility = View.INVISIBLE
                holder.separator.visibility = View.INVISIBLE
                updateProgressBarColor(holder.progressBar, R.color.green, R.color.loadEmpty)
            }

            DOWNLOAD -> {
                holder.btnDelete.visibility = View.GONE
                holder.percentage.visibility = View.VISIBLE
                holder.textStatus.visibility = View.VISIBLE
                holder.textFileName.visibility = View.GONE
                holder.dates.visibility = View.GONE
                holder.btnCancelResume.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
                holder.size.visibility = View.GONE
                holder.product.visibility = View.GONE
                holder.separator.visibility = View.GONE
                updateProgressBarColor(holder.progressBar, R.color.green, R.color.loadEmpty)
            }

            DELETED -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
            }
        }

        // Set click listeners for buttons
        holder.btnCancelResume.setOnClickListener {
            if ((it as Button).background.constantState?.equals(
                    ContextCompat.getDrawable(
                        (it as Button).context,
                        R.drawable.play
                    )?.constantState
                ) == true
            ) {
                if (isInternetAvailable(this.context)) {
                    onButtonClick(RESUME_BUTTON_CLICK, downloadData.id!!, pathAvailable)
                } else {
                    NotifyValidity(notifValidation, this.context)
                }
            } else {
                onButtonClick(CANCEL_BUTTON_CLICK, downloadData.id!!, pathAvailable)
            }
        }

        holder.btnDelete.setOnClickListener {
            triggerSpaceSignal()
            onButtonClick(DELETE_BUTTON_CLICK, downloadData.id!!, pathAvailable)

        }

        holder.btnQRCode.setOnClickListener {
            onButtonClick(QR_CODE_BUTTON_CLICK, downloadData.id!!, pathAvailable)
        }

        if (!downloadData.isUpdated) {
            holder.updated.visibility = View.VISIBLE
        } else {
            holder.updated.visibility = View.INVISIBLE
        }
        triggerDownloadSignal()

        holder.itemView.setOnClickListener {
            onButtonClick(ITEM_VIEW_CLICK, downloadData.id!!, pathAvailable)
        }
    }

    private fun updateProgressBarColor(progressBar: ProgressBar, percentageColor: Int, backgroundColor: Int) {
        val layerDrawable = progressBar.progressDrawable as LayerDrawable
        val rotateDrawable = layerDrawable.findDrawableByLayerId(R.id.loading_color_id) as RotateDrawable
        val shapeDrawable = rotateDrawable.drawable as GradientDrawable
        val backgroundDrawable = layerDrawable.findDrawableByLayerId(R.id.background) as GradientDrawable
        backgroundDrawable.setColor(ContextCompat.getColor(context, backgroundColor))
        shapeDrawable.setColor(ContextCompat.getColor(context, percentageColor))
        progressBar.progressDrawable = layerDrawable
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    override fun getItemCount(): Int {
        val sortList = asyncListDiffer.currentList.sortedByDescending {
            it.reqDate
        }
        return sortList.size
    }

    fun saveData(dataResponse: List<MapData>) {
        asyncListDiffer.submitList(dataResponse)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deliveryDate(manager: MapServiceManager, downloadData: MapData, holder: ViewHolder) {
//        TODO Way is needed to get reqDate from db, downloadData already has it?
        CoroutineScope(Dispatchers.IO).launch {
            val text: String
            val map = downloadData.id?.let { manager.service.getDownloadedMap(it) }
            if (map == null){
                text = "תאריך בקשה: לא ידוע"
            }else{
                val sdf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                val strDate = sdf.format(map.reqDate);
                text = "תאריך בקשה: ${strDate}"
            }
            withContext(Dispatchers.Main) {
                holder.demandDate.text = text
            }
        }
    }

    fun occupiedSpace(it: File): String {
        val gigabytesAvailable = it.length().toDouble() / (1024 * 1024 * 1024)
        val megabytesAvailable = it.length().toDouble() / (1024 * 1024)

        return if (gigabytesAvailable >= 1) {
            String.format("נפח: %.2f gb", gigabytesAvailable)
        } else {
            String.format("נפח: %.2f mb", megabytesAvailable)
        }
    }

    private fun NotifyValidity(notification: Toast?, context: Context) {
        notifValidation = notification
        notifValidation?.cancel()
        notifValidation = Toast.makeText(context, "ודא שה-VPN פועל", Toast.LENGTH_LONG)
        notifValidation?.show()
    }

    fun formatDate(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date: Date = inputFormat.parse(inputDate)
        return outputFormat.format(date)
    }

    companion object {
        const val RESUME_BUTTON_CLICK = 1
        const val CANCEL_BUTTON_CLICK = 2
        const val QR_CODE_BUTTON_CLICK = 3
        const val DELETE_BUTTON_CLICK = 4
        const val UPDATE_BUTTON_CLICK = 5
        const val ITEM_VIEW_CLICK = 6
    }


}

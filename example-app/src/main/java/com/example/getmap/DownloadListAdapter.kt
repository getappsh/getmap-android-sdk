package com.example.getmap


import MapDataMetaData
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
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
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


class DownloadListAdapter(
    private val onButtonClick: (Int, String, Any?) -> Unit,
    private val manager: MapServiceManager,
    private val context: Context,
) :
    RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {


    var availableUpdate: Boolean = false
    var tracker: Tracker? = null

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
        tracker = MatomoTracker.getTracker(this.context)
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
            val region = jsonText.region[0]
            holder.size.text = occupiedSpace(geo)
            holder.product.text = "תוצר: ${
                jsonText.id.subSequence(
                    jsonText.id.length - 4,
                    jsonText.id.length
                )
            }"
            deliveryDate(manager, downloadData, holder)
            holder.textFileName.text = "${region} - ${endName}"
            val startDate = jsonText.sourceDateStart.substringBefore('T')
            val endDate = jsonText.sourceDateEnd.substringBefore('T')
            var startDateFormatted = formatDate(startDate)
            var endDateFormatted = formatDate(endDate)
            val tsoulam = "צולם: "
            holder.dates.text = "${tsoulam}${endDateFormatted} - ${startDateFormatted}"

        }  else {
            CoroutineScope(Dispatchers.Default).launch {
                manager.service.getDownloadedMaps().forEach { i ->
                    val sdf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                    val stopDate = i.downloadStop
                    if (stopDate != null){
                        val a = sdf.format(stopDate)
                        holder.demandDate.text = "תאריך עצירה: ${a}"
                    }
                }
            }
        }

        holder.textStatus.text = downloadData.statusMsg
        holder.progressBar.progress = downloadData.progress

        holder.percentage.text = downloadData.progress.toString() + "%"

        holder.btnCancelResume.visibility = View.VISIBLE
        holder.btnCancelResume.isEnabled = true

        when (downloadData.deliveryState) {
            START -> {
                TrackHelper.track().event("מיפוי ענן", "ניהול בקשות").name("הורדת בול")
                    .with(tracker)
                holder.sizeLayout.visibility = View.GONE
                deliveryDate(manager, downloadData, holder)
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
            }

            DONE -> {

                TrackHelper.track().event("מיפוי ענן", "ניהול בקשות").name("בול הורד בהצלחה")
                    .with(tracker)
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
            }

            ERROR -> {
                holder.textFileName.text = "ההורדה נכשלה"
                holder.dates.visibility = View.GONE
                holder.btnDelete.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.dates.text = LocalDate.now().toString()
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
                holder.sizeLayout.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
            }

            CANCEL -> {
                holder.dates.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
                holder.textStatus.text = "בוטל - הורדה מחדש תתחיל מ-0%"
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
            }

            PAUSE -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.textStatus.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
                holder.sizeLayout.visibility = View.GONE
                holder.dates.visibility = View.GONE
            }

            CONTINUE -> {
                holder.btnDelete.visibility = View.GONE
                holder.percentage.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
                holder.size.visibility = View.INVISIBLE
                holder.product.visibility = View.INVISIBLE
                holder.separator.visibility = View.INVISIBLE
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
                onButtonClick(RESUME_BUTTON_CLICK, downloadData.id!!, pathAvailable)
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

    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }

    fun saveData(dataResponse: List<MapData>) {
        asyncListDiffer.submitList(dataResponse)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun deliveryDate(manager: MapServiceManager, downloadData: MapData, holder: ViewHolder) {
        CoroutineScope(Dispatchers.IO).launch {
            manager.service.getDownloadedMaps().forEach { i ->
                val sdf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                if (i.id == downloadData.id) {
                    val firstOffsetDateTime = downloadData.downloadStart
                    val downloadDone = downloadData.downloadDone
                    if (firstOffsetDateTime != null) {
                        val a = sdf.format(firstOffsetDateTime)
                        holder.demandDate.text = "תאריך בקשה: ${a}"
                    }else {
                        val currDate = sdf.format(downloadDone)
                        holder.demandDate.text = "תאריך סיום: ${currDate}"
                    }
                }

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

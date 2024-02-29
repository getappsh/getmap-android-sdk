package com.example.example_app


import MapDataMetaData
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState.*
import java.io.File
import java.time.LocalDate


class DownloadListAdapter(
    private val onButtonClick: (Int, String, Any?) -> Unit,
    private val pathAvailable: String
) :
    RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {


    //Create and define the signal listener
    interface OnSignalListener {
        fun onSignal()
    }

    private var signalListener: OnSignalListener? = null

    fun setOnSignalListener(listener: MainActivity) {
        signalListener = listener
    }

    private fun sendSignal() {
        if (signalListener != null) {
            signalListener!!.onSignal()
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
        val btnUpdate: Button = itemView.findViewById(R.id.btnUpdate)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val downloadData = asyncListDiffer.currentList[position]
        Log.i(
            "vsdnhilofherszofhezofezhioflezhfiollzefhzuofhezuofhezjofgdszuikzerf",
            "onBindViewHolder: ${downloadData.jsonName}"
        )
        val directory: File = File(
            //path for olar
//            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + downloadData.jsonName
//            "sdcard/Documents" +
//                    File.separator
            pathAvailable
        )

        if (directory.exists()) {
            val files: Array<File> = directory.listFiles()!!

            for (file in files.iterator())
                if (file.name == downloadData.jsonName) {
                    val text = file.readText()
                    val json_text = Gson().fromJson(text, MapDataMetaData::class.java)
                    holder.textFileName.text = json_text.productName
                    val start_date = json_text.creationDate.substringBefore('T')
                    val update_date = json_text.updateDate.substringBefore('T')
                    val tsoulam = "צולם: "
                    val minus = "-"
//                    var json_text = JSONObject(text) json_text.get("creationDate").toString().substringBefore('T')}
//                    - ${json_text.get("updateDate").toString().substringBefore('T')
//                    holder.textFileName.text = json_text.get("productName").toString()
                    holder.dates.text = "${tsoulam}${start_date} ${minus} ${update_date}"
                }
        }

        holder.textStatus.text = downloadData.statusMsg
        holder.progressBar.progress = downloadData.progress

        holder.percentage.text = downloadData.progress.toString() + "%"

        holder.btnCancelResume.visibility = View.VISIBLE
        holder.btnCancelResume.isEnabled = true

        when (downloadData.deliveryState) {
            START -> {
                holder.btnDelete.visibility = View.GONE
                holder.textStatus.visibility = View.VISIBLE
                holder.percentage.visibility = View.VISIBLE
                holder.textFileName.visibility = View.INVISIBLE
                holder.dates.visibility = View.INVISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
            }

            DONE -> {
                holder.percentage.visibility = View.GONE
                holder.textStatus.visibility = View.GONE
                holder.textFileName.visibility = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.dates.visibility = View.VISIBLE
                holder.btnCancelResume.visibility = View.GONE
                holder.progressBar.progress = 0
                sendSignal()
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.VISIBLE


            }

            ERROR -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.dates.text = LocalDate.now().toString()
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
            }

            CANCEL -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
            }

            PAUSE -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.play)
                holder.btnQRCode.visibility = View.GONE
            }

            CONTINUE -> {
                holder.btnDelete.visibility = View.GONE
                holder.percentage.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
            }

            DOWNLOAD -> {
                holder.btnDelete.visibility = View.GONE
                holder.percentage.visibility = View.VISIBLE
                holder.textStatus.visibility = View.VISIBLE
                holder.textFileName.visibility = View.INVISIBLE
                holder.dates.visibility = View.INVISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
            }

            DELETED -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.setBackgroundResource(R.drawable.square)
                holder.btnQRCode.visibility = View.GONE
                sendSignal()
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
            onButtonClick(DELETE_BUTTON_CLICK, downloadData.id!!, pathAvailable)

        }

        holder.btnQRCode.setOnClickListener {
            onButtonClick(QR_CODE_BUTTON_CLICK, downloadData.id!!, pathAvailable)
        }

        if (downloadData.isUpdated) {
            holder.btnUpdate.visibility = View.GONE
        } else {
            holder.btnUpdate.visibility = View.VISIBLE
        }
        holder.btnUpdate.setOnClickListener {
            onButtonClick(UPDATE_BUTTON_CLICK, downloadData.id!!, pathAvailable)
        }

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

    companion object {
        const val RESUME_BUTTON_CLICK = 1
        const val CANCEL_BUTTON_CLICK = 2
        const val QR_CODE_BUTTON_CLICK = 3
        const val DELETE_BUTTON_CLICK = 4
        const val UPDATE_BUTTON_CLICK = 5
        const val ITEM_VIEW_CLICK = 6
    }


}

package com.example.example_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ngsoft.getapp.sdk.models.MapDeliveryState.*
import com.ngsoft.getapp.sdk.models.MapDownloadData

class DownloadListAdapter(private val onButtonClick: (Int, String) -> Unit) : RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textFileName: TextView = itemView.findViewById(R.id.textFileName)
        val textStatus: TextView = itemView.findViewById(R.id.textStatus)
        val textError: TextView = itemView.findViewById(R.id.textError)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val btnCancelResume: Button = itemView.findViewById(R.id.btnCancelResume)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val btnQRCode: Button = itemView.findViewById(R.id.btnQRCode)
    }


    private val diffUtil = object : DiffUtil.ItemCallback<MapDownloadData>() {
        override fun areItemsTheSame(oldItem: MapDownloadData, newItem: MapDownloadData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MapDownloadData, newItem: MapDownloadData): Boolean {
            return oldItem.toString() == newItem.toString()
        }

    }
    private val asyncListDiffer = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val downloadData = asyncListDiffer.currentList[position]

        holder.textFileName.text = downloadData.fileName
        holder.textStatus.text = downloadData.statusMessage
        holder.textError.text = downloadData.errorContent
        holder.progressBar.progress = downloadData.downloadProgress

        holder.btnCancelResume.visibility = View.VISIBLE
        holder.btnCancelResume.isEnabled = true

        when(downloadData.deliveryStatus){
            START -> {
                holder.btnDelete.visibility = View.GONE
                holder.btnCancelResume.text = "Cancel"
                holder.btnQRCode.visibility = View.GONE
            }
            DONE -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.isEnabled = false
                holder.btnCancelResume.text = "Cancel"
                holder.btnQRCode.visibility = View.VISIBLE
            }
            ERROR -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.text = "Resume"
                holder.btnQRCode.visibility = View.GONE
            }
            CANCEL -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.text = "Resume"
                holder.btnQRCode.visibility = View.GONE
            }
            PAUSE -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.text = "Resume"
                holder.btnQRCode.visibility = View.GONE
            }
            CONTINUE -> {
                holder.btnDelete.visibility = View.GONE
                holder.btnCancelResume.text = "Cancel"
                holder.btnQRCode.visibility = View.GONE
            }
            DOWNLOAD -> {
                holder.btnDelete.visibility = View.GONE
                holder.btnCancelResume.text = "Cancel"
                holder.btnQRCode.visibility = View.GONE
            }
            DELETED -> {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnCancelResume.text = "Cancel"
                holder.btnQRCode.visibility = View.GONE

            }
        }

        // Set click listeners for buttons
        holder.btnCancelResume.setOnClickListener {
            if ((it as Button).text == "Resume" ){
                onButtonClick(RESUME_BUTTON_CLICK, downloadData.id!!)
            }else{
                onButtonClick(CANCEL_BUTTON_CLICK, downloadData.id!!)
            }
        }

        holder.btnDelete.setOnClickListener {
            onButtonClick(DELETE_BUTTON_CLICK, downloadData.id!!)

        }

        holder.btnQRCode.setOnClickListener {
            onButtonClick(QR_CODE_BUTTON_CLICK, downloadData.id!!)
        }
    }
    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }
    fun saveData(dataResponse: List<MapDownloadData>){
        asyncListDiffer.submitList(dataResponse)
    }

    companion object {
        const val RESUME_BUTTON_CLICK = 1
        const val CANCEL_BUTTON_CLICK = 2
        const val QR_CODE_BUTTON_CLICK = 3
        const val DELETE_BUTTON_CLICK = 4    }


}

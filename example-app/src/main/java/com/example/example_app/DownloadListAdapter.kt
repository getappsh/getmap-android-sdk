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
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData

class DownloadListAdapter(private val onDelete: (String) -> Unit,
                          private val onCancel: (String) -> Unit,
                          private val onResume: (String) -> Unit,
                          private val generateQrCode: (String) -> Unit,
    ) : RecyclerView.Adapter<DownloadListAdapter.ViewHolder>() {

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

        // Update UI elements with data from MapDownloadData
        holder.textFileName.text = downloadData.fileName
        holder.textStatus.text = downloadData.statusMessage
        holder.textError.text = downloadData.errorContent
        holder.progressBar.progress = downloadData.downloadProgress


        holder.btnDelete.isEnabled = !(downloadData.deliveryStatus == MapDeliveryState.START ||
                downloadData.deliveryStatus == MapDeliveryState.DOWNLOAD ||
                downloadData.deliveryStatus == MapDeliveryState.CONTINUE)

        if (downloadData.deliveryStatus == MapDeliveryState.DONE){
            holder.btnQRCode.visibility = View.VISIBLE
        }else{
            holder.btnQRCode.visibility = View.GONE

        }

        if (downloadData.deliveryStatus == MapDeliveryState.CANCEL ||
            downloadData.deliveryStatus == MapDeliveryState.ERROR ||
            downloadData.deliveryStatus == MapDeliveryState.PAUSE){
            holder.btnCancelResume.text = "Resume"
        }else{
            holder.btnCancelResume.text = "Cancel"

        }
        // Set click listeners for buttons
        holder.btnCancelResume.setOnClickListener {
            // TODO: Handle cancel/resume button click
            if ((it as Button).text == "Resume" ){
                onResume.invoke(downloadData.id!!)
            }else{
                onCancel.invoke(downloadData.id!!)
            }
        }

        holder.btnDelete.setOnClickListener {
            // TODO: Handle delete button click
            // You may want to confirm the deletion before proceeding
            // and update your data accordingly
//            callback.invoke(downloadData)
            onDelete.invoke(downloadData.id!!)
        }

        holder.btnQRCode.setOnClickListener {
            generateQrCode(downloadData.id!!)
        }
    }
    override fun getItemCount(): Int {
        return asyncListDiffer.currentList.size
    }
    fun saveData(dataResponse: List<MapDownloadData>){
        asyncListDiffer.submitList(dataResponse)
    }


}

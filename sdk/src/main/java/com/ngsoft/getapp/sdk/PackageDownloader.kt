package com.ngsoft.getapp.sdk

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import java.io.File


@SuppressLint("UnspecifiedRegisterReceiverFlag")
internal class PackageDownloader(private val context: Context, private val downloadDirectory: String) {
    companion object{
        fun getFileNameFromUri(url: String): String {
            return url.substring( url.lastIndexOf('/') + 1, url.length)
        }

    }

    private val TAG = "PackageDownloader"

    private val downloadManager =
        context.getSystemService(DownloadManager::class.java)

    private var downloadCompletedHandler: ((Long)->Unit)? = null

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if(id != -1L) {
                Log.d(TAG, "Download with ID = $id finished!")
                downloadCompletedHandler?.invoke(id!!)
            }
        }
    }

    init {
        //context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
    }

    protected fun finalize() {
        Log.d(TAG, "PackageDownloader finalizer - unregistering receiver...")
        context.unregisterReceiver(broadCastReceiver)
    }

    fun downloadFile(url: String, onDownloadCompleted: (Long) -> Unit): Long {
        downloadCompletedHandler = onDownloadCompleted
        val fileName= getFileNameFromUri(url)
        val request = DownloadManager.Request(url.toUri())
            .setMimeType(parseMimeType(url))

            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(fileName)

            //.addRequestHeader("Authorization", "Bearer <token>")

            .setDestinationInExternalPublicDir(downloadDirectory, fileName)

        return downloadManager.enqueue(request)
    }


    //https://gist.github.com/tolmachevroman/2c3358faad90aa802dfb
    //https://gist.github.com/typebrook/cbb06d0b6c8cfaf4b3698eca44d5413a

    @SuppressLint("Range")
    fun queryProgress(downloadId: Long): Int{
        var progress = 0
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        if (cursor.moveToFirst()) {
            when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_RUNNING -> {
                    val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (totalBytes > 0) {
                        val downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        progress = (downloadedBytes * 100 / totalBytes).toInt()
                    }
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                }

                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {}
                DownloadManager.STATUS_FAILED -> progress = -1
            }
        }

        cursor.close()
        return progress
    }

    private fun parseMimeType(url: String): String {
        val file = File(url)
        val map = MimeTypeMap.getSingleton()
        val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
        var type = map.getMimeTypeFromExtension(ext)
        type = type ?: "*/*"
        return type
    }


}
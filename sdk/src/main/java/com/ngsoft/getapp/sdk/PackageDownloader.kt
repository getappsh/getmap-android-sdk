package com.ngsoft.getapp.sdk

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import java.io.File

@SuppressLint("UnspecifiedRegisterReceiverFlag")
internal class PackageDownloader(private val context: Context, private val downloadDirectory: String) {

    private val downloadManager =
        context.getSystemService(DownloadManager::class.java)

    private var downloadCompletedHandler: ((Long)->Unit)? = null

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            //if(intent?.action == ACTION_DOWNLOAD_COMPLETE) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if(id != -1L) {
                    println("Download with ID=$id finished!")
                    downloadCompletedHandler?.invoke(id!!)
                }
           // }
        }
    }

    init {
        //context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
    }

    protected fun finalize() {
        println("PackageDownloader finalizer - unregistering receiver...")
        context.unregisterReceiver(broadCastReceiver)
    }

    fun downloadFile(url: String, onDownloadCompleted: (Long) -> Unit): Long {
        downloadCompletedHandler = onDownloadCompleted
        val fileName= getFileNameFromUri(url);
        val request = DownloadManager.Request(url.toUri())
            .setMimeType(parseMimeType(url))

            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(fileName)

            //.addRequestHeader("Authorization", "Bearer <token>")

            .setDestinationInExternalPublicDir(
                downloadDirectory,
                //Environment.DIRECTORY_DOWNLOADS,
                fileName)

        return downloadManager.enqueue(request)
    }

    private fun parseMimeType(url: String): String {
        val file = File(url)
        val map = MimeTypeMap.getSingleton()
        val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
        var type = map.getMimeTypeFromExtension(ext)
        type = type ?: "*/*"
        return type;
    }

    private fun getFileNameFromUri(url: String): String {
        return url.substring( url.lastIndexOf('/') + 1, url.length);
    }

}
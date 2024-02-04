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
import com.ngsoft.getapp.sdk.utils.FileUtils
import timber.log.Timber
import java.io.File


@SuppressLint("UnspecifiedRegisterReceiverFlag")
internal class PackageDownloader(private val context: Context, private val downloadDirectory: String) {

    data class DownloadInfo(
        val downloadId: Long,
        val status: Int,
        val totalBytes: Long,
        val downloadBytes: Long,
        val reason: String,
        val reasonCode: Int?,
        val fileName: String?
    )
    private val _tag = "PackageDownloader"

    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    private var downloadCompletedHandler: ((Long)->Unit)? = null

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if(id != -1L) {
                Timber.d("Download with ID = $id finished!")
                downloadCompletedHandler?.invoke(id!!)
            }
        }
    }

    init {
        //context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        context.registerReceiver(broadCastReceiver, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
    }

    protected fun finalize() {
        Timber.d("PackageDownloader finalizer - unregistering receiver...")
        context.unregisterReceiver(broadCastReceiver)
    }

    fun downloadFile(url: String, onDownloadCompleted: (Long) -> Unit): Long {
        downloadCompletedHandler = onDownloadCompleted
        val fileName= FileUtils.getFileNameFromUri(url)
        val request = DownloadManager.Request(url.toUri())
            .setMimeType(parseMimeType(url))

            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(fileName)

            //.addRequestHeader("Authorization", "Bearer <token>")

            .setDestinationInExternalPublicDir(downloadDirectory, fileName)

        return downloadManager.enqueue(request)
    }

    fun cancelDownload(vararg ids: Long): Int{
        return downloadManager.remove(*ids)
    }
    fun isDownloadDone(downloadId: Long?): Boolean{
        val info = downloadId?.let { queryStatus(it) }
        return info == null || info.status == DownloadManager.STATUS_SUCCESSFUL
    }
    fun isDownloadFailed(downloadId: Long?): Boolean{
        val info = downloadId?.let { queryStatus(it) }
        return info == null || info.status == DownloadManager.STATUS_FAILED
    }

    @SuppressLint("Range")
    fun queryStatus(downloadId: Long): DownloadInfo? {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        if (cursor.moveToFirst()){

            val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val downloadedBytes = if (totalBytes <=  0) 0 else cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val reasonCode = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
            val reason = getReasonErrorMessage(reasonCode)
            var fileName: String? = null
            val fileUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            if (fileUri != null){
                fileName = FileUtils.getFileNameFromUri(fileUri)
            }
            return DownloadInfo(
                downloadId = downloadId,
                status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)),
                totalBytes =  totalBytes,
                downloadBytes = downloadedBytes,
                reason = reason,
                reasonCode= reasonCode,
                fileName = fileName
            )
        }
        cursor.close()
        return null
    }
    private fun getReasonErrorMessage(reason: Int): String{
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download. Please try again."
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device was found."
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists. Download canceled."
            DownloadManager.ERROR_FILE_ERROR -> "Storage issue. Please check your storage."
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Error receiving or processing data at the HTTP level."
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> context.getString(R.string.error_not_enough_space)
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects. Download canceled."
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code. Download canceled."
            DownloadManager.ERROR_UNKNOWN -> "Unknown error occurred during download."
            DownloadManager.PAUSED_UNKNOWN -> "Download paused for an unknown reason."
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Download paused. Waiting for network connectivity."
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Download paused due to a network error. Retrying soon."
            else -> context.getString(R.string.error_access_to_the_server, reason)
        }
    }
    @SuppressLint("Range")
    fun queryProgress(downloadId: Long): Pair<Long, Long> {
        var downloadedBytes = 0L
        var totalBytes = 0L

        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
        if (cursor.moveToFirst()) {
            when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_RUNNING -> {
                    totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if(totalBytes > 0)
                        downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    else
                        totalBytes = 0
                }
                DownloadManager.STATUS_FAILED -> {
                    downloadedBytes = -1
                }
            }
        }

        cursor.close()
        return Pair(downloadedBytes, totalBytes)
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
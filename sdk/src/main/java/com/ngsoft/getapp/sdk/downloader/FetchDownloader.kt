package com.ngsoft.getapp.sdk.downloader

import android.content.Context
import com.ngsoft.getapp.sdk.ServiceConfig
import com.ngsoft.getapp.sdk.utils.FileUtils
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Request
import timber.log.Timber
import java.nio.file.Paths


object FetchDownloader{

    private lateinit var downloadDir: String
    fun init(context: Context) {
        downloadDir = ServiceConfig.getInstance(context).downloadPath

        if (Fetch.Impl.getDefaultFetchConfiguration() == null) {
            val fetchConfiguration = FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(100)
                .setNotificationManager(object : DefaultFetchNotificationManager(context) {
                    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                        return Fetch.getDefaultInstance()
                    }
                })
                .enableRetryOnNetworkGain(true)
                .setAutoRetryMaxAttempts(3)
                .enableLogging(true)
                .build()
            Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration)
        }
    }

    fun Fetch.downloadFile(url: String, fileName: String? = null, groupId: Int? = null): Int{
        Timber.d("Download file")

        val name = fileName ?: FileUtils.getFileNameFromUri(url)

        val file = Paths.get(downloadDir, name)
        val request = Request(url, file.toString())

        groupId?.let { request.groupId = groupId }

        this.enqueue(request,
            { updatedRequest: Request? -> },
            { error: Error? ->
                Timber.e("Error: ${error.toString()}")
            })

        return request.id
    }

    fun Error.message(): String {
        return when (this) {
            Error.UNKNOWN -> "Unknown error occurred."
            Error.NONE -> "No error occurred."
            Error.FILE_NOT_CREATED -> "Failed to create the file on disk."
            Error.CONNECTION_TIMED_OUT -> "Connection timed out."
            Error.UNKNOWN_HOST -> "Unknown host."
            Error.HTTP_NOT_FOUND -> "HTTP resource not found."
            Error.WRITE_PERMISSION_DENIED -> "Write permission denied."
            Error.NO_STORAGE_SPACE -> "No storage space available."
            Error.NO_NETWORK_CONNECTION -> "No network connection."
            Error.EMPTY_RESPONSE_FROM_SERVER -> "Empty response from server."
            Error.REQUEST_ALREADY_EXIST -> "Request already exists."
            Error.DOWNLOAD_NOT_FOUND -> "Download not found."
            Error.FETCH_DATABASE_ERROR -> "Fetch database error."
            Error.REQUEST_WITH_ID_ALREADY_EXIST -> "Request with ID already exists."
            Error.REQUEST_WITH_FILE_PATH_ALREADY_EXIST -> "Request with file path already exists."
            Error.REQUEST_NOT_SUCCESSFUL -> "Request not successful."
            Error.UNKNOWN_IO_ERROR -> "Unknown IO error."
            Error.FILE_NOT_FOUND -> "File not found."
            Error.FETCH_FILE_SERVER_URL_INVALID -> "Fetch file server URL invalid."
            Error.INVALID_CONTENT_HASH -> "Invalid content hash."
            Error.FAILED_TO_UPDATE_REQUEST -> "Failed to update request."
            Error.FAILED_TO_ADD_COMPLETED_DOWNLOAD -> "Failed to add completed download."
            Error.FETCH_FILE_SERVER_INVALID_RESPONSE -> "Fetch file server invalid response."
            Error.REQUEST_DOES_NOT_EXIST -> "Request does not exist."
            Error.ENQUEUE_NOT_SUCCESSFUL -> "Enqueue not successful."
            Error.COMPLETED_NOT_ADDED_SUCCESSFULLY -> "Completed download not added successfully."
            Error.ENQUEUED_REQUESTS_ARE_NOT_DISTINCT -> "Enqueued requests are not distinct."
            Error.FAILED_TO_RENAME_INCOMPLETE_DOWNLOAD_FILE -> "Failed to rename incomplete download file."
            Error.FAILED_TO_RENAME_FILE -> "Failed to rename file."
            Error.FILE_ALLOCATION_FAILED -> "File allocation failed."
            Error.HTTP_CONNECTION_NOT_ALLOWED -> "HTTP connection not allowed."
        }
    }
}
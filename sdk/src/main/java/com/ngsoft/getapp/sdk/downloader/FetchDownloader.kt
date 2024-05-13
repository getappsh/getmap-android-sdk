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



class FetchDownloader (private val context: Context) {




    private lateinit var fetch: Fetch

    init {
        if (Fetch.Impl.getDefaultFetchConfiguration() == null){
            val fetchConfiguration = FetchConfiguration.Builder(context)
                .setDownloadConcurrentLimit(100)
                .setNotificationManager(object : DefaultFetchNotificationManager(context) {
                    override fun getFetchInstanceForNamespace(namespace: String): Fetch {
                        return fetch
                    }
                })
                .enableRetryOnNetworkGain(true)
                .setAutoRetryMaxAttempts(3)
                .enableLogging(true)
                .build()
            Fetch.Impl.setDefaultInstanceConfiguration(fetchConfiguration)
            fetch = Fetch.Impl.getDefaultInstance()
        }
        fetch = Fetch.Impl.getDefaultInstance()
    }

    fun downloadFile(url: String, fileName: String? = null, groupId: Int? = null): Int{
        Timber.d("Download file")

        val name = fileName ?: FileUtils.getFileNameFromUri(url)

        val file = Paths.get(ServiceConfig.getInstance(context).downloadPath, name)
        val request = Request(url, file.toString())

        groupId?.let { request.groupId = groupId }

        fetch.enqueue(request,
            { updatedRequest: Request? ->


            },
            { error: Error? ->
                Timber.e("Error: ${error.toString()}")

            }
        )

        return request.id
    }


}
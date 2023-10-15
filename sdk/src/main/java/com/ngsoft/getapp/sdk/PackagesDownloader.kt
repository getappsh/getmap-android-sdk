package com.ngsoft.getapp.sdk

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

data class PackageDownloadProgress(
    val fileName: String,
    val progress: Int,
    val isCompleted: Boolean
)

data class DownloadProgress(
    val packagesProgress: List<PackageDownloadProgress>,
    val totalProgress: Int,
    val isCompleted: Boolean
)

internal class PackagesDownloader(context: Context, downloadDirectory: String) {
    private data class DownloadTrack(
        val fileName: String,
        var progress: Int,
        var isCompleted: Boolean
    )

    private val TAG = "PackagesDownloader"
    private val downloader = PackageDownloader(context, downloadDirectory)
    private var downloadProgressHandler: ((DownloadProgress)->Unit)? = null

    fun downloadFiles(files2download: List<String>, onProgress: (DownloadProgress)->Unit) {
        downloadProgressHandler = onProgress

        //var tmr: Timer? = null

        val downloads = HashMap<Long, DownloadTrack>()

        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(TAG,"completion for download id = $it...")
            downloads[it]?.isCompleted = true

            var total = 0
            var completed = 0
            val packages = mutableListOf<PackageDownloadProgress>()
            downloads.forEach { (_, v) ->
                total++
                if(v.isCompleted) completed++
                packages.add(PackageDownloadProgress(v.fileName, v.progress, v.isCompleted))
            }

//            isCompleted = total == completed
//            if(isCompleted) tmr?.cancel()

            val progress = DownloadProgress(packages, ((1.0f * completed)/total * 100).roundToInt(), total == completed)
            downloadProgressHandler?.invoke(progress)
        }

        for (file in files2download){
            val downloadId = downloader.downloadFile(file, downloadCompletionHandler)
            Log.d(TAG,"adding downloadId = $downloadId...")
            downloads[downloadId] = DownloadTrack(downloader.getFileNameFromUri(file),0, false)
        }

//        val tmr = timer(initialDelay = 0, period = 100 ) {
//            downloads.forEach { (k, v) ->
//                val progress = downloader.queryProgress(k)
//                v.progress = progress
//                println("$k = $progress %")
//            }
//        }

    }

}
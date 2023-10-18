package com.ngsoft.getapp.sdk

import android.content.Context
import android.util.Log
import kotlin.math.roundToInt

/**
 * Package download progress
 *
 * @property fileName tile package's file name
 * @property isCompleted completion flag
 * @constructor Create empty Package download progress
 */

data class PackageDownloadProgress(
    val fileName: String,
    //val progress: Int,
    val isCompleted: Boolean
)

/**
 * Download progress
 *
 * @property packagesProgress list of download progress descriptors
 * @property totalProgress whole download progress (N{downloaded} of N{total} as percentage)
 * @property isCompleted completion flag
 * @constructor Create empty Download progress
 */
data class DownloadProgress(
    val packagesProgress: List<PackageDownloadProgress>,
    val totalProgress: Int,
    val isCompleted: Boolean
)

internal class PackagesDownloader(context: Context, downloadDirectory: String, private var downloader: PackageDownloader?) {
    init {
        if(downloader == null)
            downloader = PackageDownloader(context, downloadDirectory)
    }

    private data class DownloadTrack(
        val fileName: String,
        var progress: Int,
        var isCompleted: Boolean
    )

    private val TAG = "PackagesDownloader"
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
                packages.add(PackageDownloadProgress(v.fileName,
                    //v.progress,
                    v.isCompleted))
            }

//            isCompleted = total == completed
//            if(isCompleted) tmr?.cancel()

            val progress = DownloadProgress(packages, ((1.0f * completed)/total * 100).roundToInt(), total == completed)
            downloadProgressHandler?.invoke(progress)
        }

        for (file in files2download){
            val downloadId = downloader?.downloadFile(file, downloadCompletionHandler)
            Log.d(TAG,"adding downloadId = $downloadId...")
            downloads[downloadId!!] = DownloadTrack(PackageDownloader.getFileNameFromUri(file),0, false)
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
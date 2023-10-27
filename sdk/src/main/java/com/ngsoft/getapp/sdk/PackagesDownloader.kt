package com.ngsoft.getapp.sdk

import android.content.Context
import android.util.Log
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.math.roundToInt

/**
 * Package download progress
 *
 * @property fileName of downloaded package
 * @property progress of download in %
 * @property isCompleted completion flag
 * @constructor Create empty Package download progress
 */
data class PackageDownloadProgress(
    val fileName: String,
    val progress: Int,
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

    fun downloadFiles(files2download: List<String>, onProgress: (DownloadProgress)->Unit) {

        var tmr: Timer? = null
        var isCompleted = false
        var totalProgress = 0
        val downloads = HashMap<Long, DownloadTrack>()

        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(TAG,"completion for download id = $it...")
            downloads[it]?.isCompleted = true
            downloads[it]?.progress = 100

            var total = 0
            var completed = 0
            val packages = mutableListOf<PackageDownloadProgress>()
            downloads.forEach { (_, v) ->
                total++
                if(v.isCompleted) completed++
                packages.add(PackageDownloadProgress(v.fileName, v.progress, v.isCompleted))
            }

            totalProgress = ((1.0f * completed)/total * 100).roundToInt()
            isCompleted = total == completed
            if(isCompleted) {
                Log.d(TAG,"stopping progress watcher...")
                tmr?.cancel()
            }

            val progress = DownloadProgress(packages, totalProgress, isCompleted)
            onProgress.invoke(progress)
        }

        for (file in files2download){
            val downloadId = downloader?.downloadFile(file, downloadCompletionHandler)
            Log.d(TAG,"adding downloadId = $downloadId...")
            downloads[downloadId!!] = DownloadTrack(PackageDownloader.getFileNameFromUri(file),0, false)
        }

        Log.d(TAG,"queued ${downloads.count()} downloads...")
        Log.d(TAG,"starting progress watcher...")

        tmr = timer(initialDelay = 100, period = 500 ) {
            val packages = mutableListOf<PackageDownloadProgress>()
            downloads.forEach { (k, v) ->
                val fileProgress = downloader?.queryProgress(k)
                v.progress = fileProgress!!
                packages.add(PackageDownloadProgress(v.fileName, v.progress, v.isCompleted))
            }

            val progress = DownloadProgress(packages, totalProgress, isCompleted)
            onProgress.invoke(progress)
        }

    }

}
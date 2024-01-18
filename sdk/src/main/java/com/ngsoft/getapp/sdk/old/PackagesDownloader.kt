package com.ngsoft.getapp.sdk.old

import android.content.Context
import android.util.Log
import com.ngsoft.getapp.sdk.PackageDownloader
import com.ngsoft.getapp.sdk.utils.FileUtils
import java.util.Timer
import kotlin.concurrent.timer

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
 * @property totalProgress whole download progress
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

    private val _tag = "PackagesDownloader"

    fun downloadFiles(files2download: List<String>, onProgress: (DownloadProgress)->Unit) {

        var tmr: Timer? = null
        var isCompleted = false
        var totalProgress = 0
        val downloads = HashMap<Long, DownloadTrack>()

        val downloadCompletionHandler: (Long) -> Unit = {
            Log.d(_tag,"completion for download id = $it...")
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

            isCompleted = total == completed
            if(isCompleted) {
                totalProgress = 100
                Log.d(_tag,"stopping progress watcher...")
                tmr?.cancel()
            }

            val progress = DownloadProgress(packages, totalProgress, isCompleted)
            onProgress.invoke(progress)
        }

        for (file in files2download){
            val downloadId = downloader?.downloadFile(file, downloadCompletionHandler)
            Log.d(_tag,"adding downloadId = $downloadId...")
            downloads[downloadId!!] = DownloadTrack(FileUtils.getFileNameFromUri(file),0, false)
        }

        Log.d(_tag,"queued ${downloads.count()} downloads...")
        Log.d(_tag,"starting progress watcher...")

        tmr = timer(initialDelay = 100, period = 500 ) {
            var downloadedBytes = 0L
            var totalBytes = 0L
            val packages = mutableListOf<PackageDownloadProgress>()
            downloads.forEach { (k, v) ->
                val fileProgress = downloader?.queryProgress(k)
                if (fileProgress != null) {
                    if(fileProgress.second > 0) {
                        val progress = (fileProgress.first * 100 / fileProgress.second).toInt()
                        v.progress = progress
                    }
                    downloadedBytes += fileProgress.first
                    totalBytes += fileProgress.second
                }

                packages.add(PackageDownloadProgress(v.fileName, v.progress, v.isCompleted))
            }

            if(totalBytes > 0)
                totalProgress = (downloadedBytes * 100 / totalBytes).toInt()

            val progress = DownloadProgress(packages, totalProgress, isCompleted)
            onProgress.invoke(progress)
        }

    }

}
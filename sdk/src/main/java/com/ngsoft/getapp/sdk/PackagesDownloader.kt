package com.ngsoft.getapp.sdk

import android.content.Context

data class PackageDownloadProgress(
    val fileName: String,
    val progress: Float,
    val isCompleted: Boolean
)

data class DownloadProgress(
    val packagesProgress: List<PackageDownloadProgress>,
    val totalProgress: Float,
    val isCompleted: Boolean
)

internal class PackagesDownloader(context: Context, downloadDirectory: String) {
    private data class DownloadTrack(
        val fileName: String,
        var progress: Float,
        var isCompleted: Boolean
    )

    private val downloader = PackageDownloader(context, downloadDirectory)
    private var downloadProgressHandler: ((DownloadProgress)->Unit)? = null

    fun downloadFiles(files2download: List<String>, onProgress: (DownloadProgress)->Unit) {
        downloadProgressHandler = onProgress

        val downloads = HashMap<Long, DownloadTrack>()

        val downloadCompletionHandler: (Long) -> Unit = {
            println("processing download completion for id =$it...")
            downloads[it]?.isCompleted = true

            var total = 0
            var completed = 0
            val packages = mutableListOf<PackageDownloadProgress>()
            downloads.forEach { (_, v) ->
                total++
                if(v.isCompleted) completed++
                packages.add(PackageDownloadProgress(v.fileName, v.progress, v.isCompleted))
            }

            val progress = DownloadProgress(packages, completed/total * 100.0f, total == completed)
            downloadProgressHandler?.invoke(progress)
        }

        for (file in files2download){
            val downloadId = downloader.downloadFile(file, downloadCompletionHandler)
            println("adding downloadId id =$downloadId...")
            downloads[downloadId] = DownloadTrack(getFileNameFromUri(file),0.0f, false)
        }

    }

    private fun getFileNameFromUri(url: String): String {
        return url.substring( url.lastIndexOf('/') + 1, url.length)
    }

}
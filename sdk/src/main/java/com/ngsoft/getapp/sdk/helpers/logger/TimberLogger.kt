package com.ngsoft.getapp.sdk.helpers.logger

import android.os.Environment
import android.util.Log
import timber.log.Timber
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber.*

internal object TimberLogger {
     fun initTimber(){
         Timber.uprootAll()
         val maxFileSize = 1024 * 1024 * 2// 2 MB
         val maxFiles = 5


         val fileTree = FileLoggerTree.Builder()
             .withFileName("file%g.log")
             .withDirName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/GetApp/Logs")
             .withSizeLimit(maxFileSize)
             .withFileLimit(maxFiles)
             .withMinPriority(Log.WARN)
             .appendToFile(true)
             .build()
         Timber.plant(
             DebugTree(),
             fileTree
         )
     }


    fun getBugReportTree(): FileLoggerTree {
        val fileTree = FileLoggerTree.Builder()
            .withFileName("report%g.log")
            .withDirName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/GetApp/bug-report")
            .withMinPriority(Log.VERBOSE)
            .withFileLimit(1)
            .withSizeLimit(1024 * 1024 * 10)
            .appendToFile(true)
            .build()

        return fileTree
    }


}
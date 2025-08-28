package com.ngsoft.getapp.sdk.helpers.logger

import android.os.Environment
import android.util.Log
import timber.log.Timber
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber.*

internal object TimberLogger {
    private var initialized = false
    private var fileTree: FileLoggerTree? = null

    @Synchronized
    fun initTimber() {
        if (initialized) return
        initialized = true

        Timber.plant(DebugTree())

        fileTree = FileLoggerTree.Builder()
            .withFileName("file%g.log")
            .withDirName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/GetApp/Logs")
            .withSizeLimit(2 * 1024 * 1024)
            .withFileLimit(5)
            .withMinPriority(Log.WARN)
            .appendToFile(true)
            .build()

        fileTree?.apply { Timber.plant(this) }
    }


    fun getFileTree(): FileLoggerTree? {
        return fileTree
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
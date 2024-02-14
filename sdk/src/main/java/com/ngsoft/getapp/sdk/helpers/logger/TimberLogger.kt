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
             .withDirName(Environment.getExternalStorageDirectory().toString() + "/GetApp/All-Logs")
             .withSizeLimit(maxFileSize)
             .withFileLimit(maxFiles)
             .withMinPriority(Log.VERBOSE)
             .appendToFile(true)
             .build()
         Timber.plant(
             DebugTree(),
             fileTree
         )
     }


}
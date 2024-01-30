package com.ngsoft.getapp.sdk.helpers

import android.os.Environment
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlobalExceptionHandler: Thread.UncaughtExceptionHandler {

    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    private val logDirectory =
        File(Environment.getExternalStorageDirectory().toString() + "/GetApp/Logs")

    private val logFileName = "error_log.txt"

    private val maxFileSizeBytes = 1024 * 1024 // 1 MB
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        logExceptionToFile(throwable)
        defaultExceptionHandler?.uncaughtException(thread, throwable)
    }

    private fun logExceptionToFile(throwable: Throwable) {
        try {
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val logFile = File(logDirectory, logFileName)

            if (logFile.length() > maxFileSizeBytes) {
                trimLogFile(logFile)
            }


            val writer = PrintWriter(FileWriter(logFile, true))
            writer.println("Timestamp: $timestamp")
            throwable.printStackTrace(writer)

            val methodName = StringBuilder("stackTrace: ")
            val stacktrace = throwable.stackTrace
            stacktrace.forEach { methodName.append(it.methodName + "->") }
            writer.println(methodName)

            writer.close()
        } catch (e: Exception) {
            Timber.e("Error writing exception to file: ${e.message}")
        }


    }


    private fun trimLogFile(logFile: File) {
        val content = logFile.readText()
        val startIndex = content.indexOf("\n", content.length / 2)
        logFile.writeText(content.substring(startIndex + 1))


    }
}
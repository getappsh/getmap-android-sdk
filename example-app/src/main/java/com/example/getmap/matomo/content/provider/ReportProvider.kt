package com.example.getmap.matomo.content.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

class ReportProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "com.example.getmap.matomo.content.provider"
        private const val REPORTS_PATH = "reports"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$REPORTS_PATH")

        private const val REPORTS = 1
        private const val REPORT_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, REPORTS_PATH, REPORTS)
            addURI(AUTHORITY, "$REPORTS_PATH/#", REPORT_ID)
        }

        private lateinit var databaseHelper: SQLiteOpenHelper
    }

    override fun onCreate(): Boolean {
        databaseHelper = ReportDatabaseHelper(context as Context)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val db = databaseHelper.readableDatabase
        return when (uriMatcher.match(uri)) {
            REPORTS -> db.query(ReportDatabaseHelper.TABLE_REPORTS, projection, selection, selectionArgs, null, null, sortOrder)
            REPORT_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(ReportDatabaseHelper.TABLE_REPORTS, projection, "${ReportDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()), null, null, sortOrder)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = databaseHelper.writableDatabase
        val id = db.insert(ReportDatabaseHelper.TABLE_REPORTS, null, values)
        if (id > 0) {
            val returnUri = ContentUris.withAppendedId(CONTENT_URI, id)
            context?.contentResolver?.notifyChange(returnUri, null)
            return returnUri
        }
        throw SQLException("Failed to insert row into $uri")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val db = databaseHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            REPORTS -> db.update(ReportDatabaseHelper.TABLE_REPORTS, values, selection, selectionArgs)
            REPORT_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(ReportDatabaseHelper.TABLE_REPORTS, values, "${ReportDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = databaseHelper.writableDatabase
        return when (uriMatcher.match(uri)) {
            REPORTS -> db.delete(ReportDatabaseHelper.TABLE_REPORTS, selection, selectionArgs)
            REPORT_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(ReportDatabaseHelper.TABLE_REPORTS, "${ReportDatabaseHelper.COLUMN_ID}=?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            REPORTS -> "vnd.android.cursor.dir/$AUTHORITY.$REPORTS_PATH"
            REPORT_ID -> "vnd.android.cursor.item/$AUTHORITY.$REPORTS_PATH"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}

package com.example.getmap.matomo.content.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportUtils {

    fun insertReport(contentResolver: ContentResolver, report: Report) {
        val values = ContentValues().apply {
            put(ReportDatabaseHelper.COLUMN_NAME, report.name)
            put(ReportDatabaseHelper.COLUMN_TYPE, report.type.toString())
            put(ReportDatabaseHelper.COLUMN_PATH, report.path)
            put(ReportDatabaseHelper.COLUMN_TITLE, report.title)
            put(ReportDatabaseHelper.COLUMN_CATEGORY, report.category)
            put(ReportDatabaseHelper.COLUMN_ACTION, report.action)
            put(ReportDatabaseHelper.COLUMN_VALUE, report.value)
            put(ReportDatabaseHelper.COLUMN_DIMID, report.dimId)
            put(ReportDatabaseHelper.COLUMN_DIMVALUE, report.dimValue)
        }
        contentResolver.insert(ReportProvider.CONTENT_URI, values)
    }

    fun readReports(contentResolver: ContentResolver): List<Report> {
        val reports = mutableListOf<Report>()
        val projection = arrayOf(
            ReportDatabaseHelper.COLUMN_ID,
            ReportDatabaseHelper.COLUMN_NAME,
            ReportDatabaseHelper.COLUMN_TYPE,
            ReportDatabaseHelper.COLUMN_PATH,
            ReportDatabaseHelper.COLUMN_TITLE,
            ReportDatabaseHelper.COLUMN_CATEGORY,
            ReportDatabaseHelper.COLUMN_ACTION,
            ReportDatabaseHelper.COLUMN_VALUE,
            ReportDatabaseHelper.COLUMN_DIMID,
            ReportDatabaseHelper.COLUMN_DIMVALUE,
            ReportDatabaseHelper.COLUMN_CREATED_AT
        )

        val cursor = contentResolver.query(
            ReportProvider.CONTENT_URI,
            projection,
            null,
            null,
            null)

        cursor?.use { c ->
            while (c.moveToNext()) {
                val report = getReport(c)
                reports.add(report)
            }
        }
        return reports
    }

    fun deleteReport(contentResolver: ContentResolver, id: Long) {
        val selection = "${ReportDatabaseHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        contentResolver.delete(ReportProvider.CONTENT_URI, selection, selectionArgs)
    }

    @SuppressLint("ConstantLocale")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun stringToDate(dateString: String?): Date? {
        return dateString?.let {
            dateFormat.parse(it)
        }
    }

    private fun getReport(cursor: Cursor): Report {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_ID))
        val type = VariantReportEnum.fromString(
            cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_TYPE))
        )
        val path = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_PATH))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_TITLE))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_CATEGORY))
        val action = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_ACTION))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_NAME))
        val value = cursor.getFloat(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_VALUE))
        val dimId = cursor.getInt(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_DIMID))
        val dimValue = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_DIMVALUE))
        val createdAtString = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_CREATED_AT))
        val createdAt = stringToDate(createdAtString)

        return Report(id, type, path, title, category, action, name, value, dimId, dimValue, createdAt)
    }
}

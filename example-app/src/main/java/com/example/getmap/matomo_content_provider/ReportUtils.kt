package com.example.getmap.matomo_content_provider

import Report
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.completecontentprovider.VariantReport

object ReportUtils {

    fun insertReport(db: SQLiteDatabase, report: Report) {
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
        db.insert(ReportDatabaseHelper.TABLE_REPORTS, null, values)
    }

    fun getReport(cursor: Cursor): Report {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_ID))
        val type = VariantReport.fromString(
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    ReportDatabaseHelper.COLUMN_TYPE
                )
            )
        )
        val path = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_PATH))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_TITLE))
        val category = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_CATEGORY))
        val action = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_ACTION))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_NAME))
        val value = cursor.getFloat(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_VALUE))
        val dimId = cursor.getInt(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_DIMID))
        val dimValue = cursor.getString(cursor.getColumnIndexOrThrow(ReportDatabaseHelper.COLUMN_DIMVALUE))

        return Report(id, type, path, title, category, action, name, value, dimId, dimValue)
    }
}

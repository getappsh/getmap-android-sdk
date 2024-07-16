package com.example.getmap.matomo.provider

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReportDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "reports.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_REPORTS = "reports"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "type"
        const val COLUMN_PATH = "path"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_ACTION = "action"
        const val COLUMN_VALUE = "value"
        const val COLUMN_DIMID = "dimId"
        const val COLUMN_DIMVALUE = "dimValue"
        const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_REPORTS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_TYPE TEXT CHECK ($COLUMN_TYPE IN ('Event', 'Screen')) NOT NULL,"
                + "$COLUMN_PATH TEXT,"
                + "$COLUMN_TITLE TEXT,"
                + "$COLUMN_CATEGORY TEXT,"
                + "$COLUMN_ACTION TEXT,"
                + "$COLUMN_VALUE FLOAT,"
                + "$COLUMN_DIMID INTEGER,"
                + "$COLUMN_DIMVALUE TEXT,"
                + "$COLUMN_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP)") // Add the new column with default value
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REPORTS")
        onCreate(db)
    }
}
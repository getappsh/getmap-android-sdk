package com.elbit.system_test

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Manifest declaration example:
 *
 * ```
 * <uses-permission android:name="com.getmap.matomo.provider.READ_ONLY" />
 * <uses-permission android:name="com.getmap.matomo.provider.WRITE_ONLY" />
 * ```
 * ```
 * <queries>
 *   <provider
 *     android:authorities="com.getmap.matomo.provider" />
 * </queries>
 * ```
 */
object ConsumerUtils {

    private const val AUTHORITY = "com.getmap.matomo.provider"
    private const val REPORTS_PATH = "reports"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$REPORTS_PATH")

    const val COLUMN_NAME = "name"
    const val COLUMN_TYPE = "type"
    const val COLUMN_PATH = "path"
    const val COLUMN_TITLE = "title"
    const val COLUMN_CATEGORY = "category"
    const val COLUMN_ACTION = "action"
    const val COLUMN_VALUE = "value"
    const val COLUMN_DIMID = "dimId"
    const val COLUMN_DIMVALUE = "dimValue"

    suspend fun dispatchBaseEvent(contentResolver: ContentResolver, category: String, action: String, value: String?, name: String?, path: String?, dimId: String?, dimValue: String?){
        withContext(Dispatchers.IO){
            val values = ContentValues().apply {
                put(COLUMN_TYPE, "Event")
                put(COLUMN_CATEGORY, category)
                put(COLUMN_ACTION, action)

                name?.let { put(COLUMN_NAME, it)}
                path?.let { put(COLUMN_PATH, it)}
                value?.let { put(COLUMN_VALUE, it)}

                if (dimId != null && dimValue != null){
                    put(COLUMN_DIMID, dimId)
                    put(COLUMN_DIMVALUE, dimValue)

                }
            }
            contentResolver.insert(CONTENT_URI, values)
        }
    }

    suspend fun dispatchScreenEvent(contentResolver: ContentResolver, path: String, title: String?,  dimId: String?, dimValue: String?){
        withContext(Dispatchers.IO){
            val values = ContentValues().apply {
                put(COLUMN_TYPE, "Screen")
                put(COLUMN_PATH, path)
                title?.let { put(COLUMN_TITLE, it) }
                if (dimId != null && dimValue != null) {
                    put(COLUMN_DIMID, dimId)
                    put(COLUMN_DIMVALUE, dimValue)
                }
            }
            contentResolver.insert(CONTENT_URI, values)
        }

    }
}
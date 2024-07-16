package com.example.getmap.matomo

import android.content.ContentResolver
import com.example.getmap.matomo.content.provider.Report
import com.example.getmap.matomo.content.provider.ReportUtils
import com.example.getmap.matomo.content.provider.VariantReportEnum
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportProcessor {

    fun process(tracker: Tracker, contentResolver: ContentResolver) {
        val reports = ReportUtils.readReports(contentResolver)
        reports.forEach { report ->
            buildEvent(report)?.let { tracker.track(it) }
            ReportUtils.deleteReport(contentResolver, report.id)
        }

    }

    private fun buildEvent(report: Report): TrackMe? {
        val trackBuilder = if (report.dimId != null && report.dimValue != null) {
            TrackHelper.track().dimension(report.dimId, report.dimValue)
        } else {
            TrackHelper.track()
        }

        val trackMe = when (report.type) {
            VariantReportEnum.Event -> {
                report.category ?: return null
                report.action ?: return null
                trackBuilder.event(report.category, report.action)
                    .apply {
                        report.path?.let { path(it) }
                        report.name?.let { name(it) }
                        report.value?.let { value(it) }
                    }
                    .build()
            }
            VariantReportEnum.Screen -> {
                trackBuilder.screen(report.path ?: return null)
                    .apply {
                        report.title?.let { title(it) }
                    }
                    .build()
            }
        }

        trackMe?.set(
            QueryParams.DATETIME_OF_REQUEST,
            SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US).format(report.createdAt ?: Date())
        )

        return trackMe
    }
}
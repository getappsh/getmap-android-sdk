package com.example.getmap.matomo.provider

import android.content.Context
import androidx.loader.content.CursorLoader

class ReportLoader(context: Context) : CursorLoader(
    context,
    ReportProvider.CONTENT_URI,
    null,
    null,
    null,
    null
)

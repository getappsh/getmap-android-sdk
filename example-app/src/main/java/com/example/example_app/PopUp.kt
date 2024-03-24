package com.example.example_app

import android.content.Context
import com.example.example_app.matomo.MatomoTracker
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.ngsoft.getapp.sdk.models.MapData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

@RequiresApi(Build.VERSION_CODES.R)
class PopUp : DialogFragment() {
    var textM: String = ""
    var mapId = ""
    var type = ""
    lateinit var handler: (MapData) -> Unit
    var tracker: Tracker? = null
    var demand = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.pop_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tracker = this.context?.let { MatomoTracker.getTracker(it) }
        val instance = MapServiceManager.getInstance()
        val service = instance.service
        super.onViewCreated(view, savedInstanceState)
        val buttonDelete = view.findViewById<Button>(R.id.buttonApply)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val textView = view.findViewById<TextView>(R.id.textViewMessage)
        textView.text = textM
        buttonDelete.setOnClickListener {
            if (type == "delete") {
                CoroutineScope(Dispatchers.IO).launch {
                    for (data in service.getDownloadedMaps()) {

                        if (data.id == mapId && data.statusMsg != "הסתיים") {
                            TrackHelper.track().dimension(1, mapId)
                                .event("מיפוי ענן", "ניהול בולים")
                                .name("מחיקת בקשה").with(tracker)
                            break

                        } else {
                            TrackHelper.track().dimension(1, mapId)
                                .event("מיפוי ענן", "ניהול בקשות")
                                .name("מחיקת בול").with(tracker)
                            break
                        }
                    }
                }

                GlobalScope.launch(Dispatchers.IO) {
                    service.deleteMap(mapId)
//                    TrackHelper.track().event("deleteButton", "delete-map").with(tracker)
                }
            } else if (type == "update") {
                TrackHelper.track().dimension(1, "כלל הבולים שהורדו")
                    .event("מיפוי ענן", "ניהול בולים").name("עדכון כלל הבולים").with(tracker)

                GlobalScope.launch(Dispatchers.IO) {
                    service.getDownloadedMaps().forEach { mapData ->
                        if (!mapData.isUpdated) {
                            service.downloadUpdatedMap(
                                mapData.id!!,
                                handler
                            )
                        }
                    }
//                        TrackHelper.track().event("Sync-bboxs", "fetch-inventory").with(tracker)
                }
            } else if (type == "updateOne") {
                TrackHelper.track().dimension(1, mapId).event("מיפוי ענן", "ניהול בולים")
                    .name("עדכון בול").with(tracker)
                CoroutineScope(Dispatchers.IO).launch {
                    service.downloadUpdatedMap(mapId, handler)
                }
            } else if (type == "cancelled") {
                TrackHelper.track().dimension(1, mapId).event("מיפוי ענן", "ניהול בקשות")
                    .name("עצירה").with(tracker)
                GlobalScope.launch(Dispatchers.IO) {
                    service.cancelDownload(mapId)
                }
            }
            dismiss()
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }
}

package com.example.getmap

import com.example.getmap.matomo.MatomoTracker
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.ngsoft.getapp.sdk.models.MapData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

@RequiresApi(Build.VERSION_CODES.R)
class PopUp : DialogFragment() {
    var textM: String = ""
    var mapId = ""
    var type = ""
    var bullName = ""
    lateinit var handler: (MapData) -> Unit
    var tracker: Tracker? = null
    var demand = false
    lateinit var recyclerView: RecyclerView

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

        buttonCancel.setOnClickListener {
            if (type == "delete") {

                Log.i("bull name", bullName)
                TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), bullName)
                    .event("מיפוי ענן", "ניהול בולים")
                    .name("מחיקת בול - ביטול מחיקה").with(tracker)
            }

            dismiss()
        }

        buttonDelete.setOnClickListener {
            if (type == "delete") {
                Log.i("bull name", bullName)
                CoroutineScope(Dispatchers.IO).launch {
                    val data = service.getDownloadedMap(mapId)
                    if (data != null) {
                        if (data.statusMsg != "הסתיים") {
                            TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), bullName)
                                .event("מיפוי ענן", "ניהול בקשות")
                                .name("מחיקת בקשה").with(tracker)
                            return@launch

                        } else {
                            TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), bullName)
                                .event("מיפוי ענן", "ניהול בולים")
                                .name("מחיקת בול").with(tracker)
                            return@launch
                        }
                    } else {
                        Toast.makeText(
                            this@PopUp.context,
                            "The map does not exist",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                GlobalScope.launch(Dispatchers.IO) {
                    service.deleteMap(mapId)
//                    TrackHelper.track().event("deleteButton", "delete-map").with(tracker)
                }
            } else if (type == "update") {
                TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), "כלל הבולים שהורדו")
                    .event("מיפוי ענן", "ניהול בולים").name("עדכון כלל הבולים").with(tracker)

                GlobalScope.launch(Dispatchers.IO) {

                    service.getDownloadedMaps().forEach { mapData ->
                        if (!mapData.isUpdated) {
                            service.downloadUpdatedMap(
                                mapData.id!!)
                        }
                    }
                    recyclerView.smoothScrollToPosition(0)
//                        TrackHelper.track().event("Sync-bboxs", "fetch-inventory").with(tracker)
                }
            } else if (type == "updateOne") {
                TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), bullName).event("מיפוי ענן", "ניהול בולים")
                    .name("עדכון בול").with(tracker)
                CoroutineScope(Dispatchers.IO).launch {
                    service.downloadUpdatedMap(mapId)
                    recyclerView.smoothScrollToPosition(0)
                }
            } else if (type == "cancelled") {
                TrackHelper.track().dimension(service.config.matomoDimensionId.toInt(), bullName).event("מיפוי ענן", "ניהול בקשות")
                    .name("עצירה").with(tracker)
                GlobalScope.launch(Dispatchers.IO) {
                    service.cancelDownload(mapId)
                }
            }
            dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
        dialog?.setCancelable(false)
    }
}

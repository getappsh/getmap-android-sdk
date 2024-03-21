package com.example.example_app

import android.app.AlertDialog
import android.content.Context
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
    lateinit var handler : (MapData) -> Unit
    var tracker: Tracker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
         return inflater.inflate(R.layout.pop_up, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val instance = MapServiceManager.getInstance()
        val service = instance.service
        super.onViewCreated(view, savedInstanceState)
        val buttonDelete = view.findViewById<Button>(R.id.buttonApply)
        val buttonCancel = view.findViewById<Button>(R.id.buttonCancel)
        val textView = view.findViewById<TextView>(R.id.textViewMessage)
        val syncButton = view.findViewById<Button>(R.id.Sync)
        textView.text = textM
        buttonDelete.setOnClickListener {
            if (type == "delete") {
                GlobalScope.launch(Dispatchers.IO) {
                    service.deleteMap(mapId)
//                    TrackHelper.track().event("deleteButton", "delete-map").with(tracker)
                }
            } else if (type == "update") {
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

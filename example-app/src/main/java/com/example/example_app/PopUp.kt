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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
class PopUp : DialogFragment() {

    var textM: String = ""
    var mapId = ""


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
        textView.text = textM
        buttonDelete.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                service.deleteMap(mapId)
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

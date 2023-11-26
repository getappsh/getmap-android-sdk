package com.getapp.technician

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.getapp.technician.mockserver.MockServer
import com.getapp.technician.utils.FileHelper
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.qualifiedName
    private val PICK_DISCOVERY_FILE = 1


    private lateinit var failedValidation: SwitchCompat
    private lateinit var fastDownload: SwitchCompat
    private lateinit var selectDiscovery: Button

    lateinit var mockServer: MockServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        failedValidation = findViewById(R.id.failed_validation)
//        fastDownload = findViewById(R.id.fast_download)
        selectDiscovery = findViewById<Button>(R.id.select_discovery)


        thread {
            mockServer = MockServer.getInstance(this)
//            runOnUiThread {
//                failedValidation.isChecked = mockServer.config.failedValidation
//                fastDownload.isChecked = mockServer.config.fastDownload
//            }
        }
//
//        failedValidation.setOnCheckedChangeListener { _, isChecked ->
//            mockServer.config.failedValidation = isChecked
//        }
//
//        fastDownload.setOnCheckedChangeListener { _, isChecked ->
//            mockServer.config.fastDownload = isChecked
//        }

        selectDiscovery.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"

            }


            startActivityForResult(intent, PICK_DISCOVERY_FILE)
        }

        if (!checkStoragePermissions()){
            requestForStoragePermissions()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_DISCOVERY_FILE && resultCode == RESULT_OK) {
            if (data != null) {

                val selectedFileUri = data.data

                Log.d(TAG, "onActivityResult - selected discovery: $selectedFileUri")
                handleSelectDiscoveryFile(selectedFileUri)


            }
        }
    }
    private fun handleSelectDiscoveryFile(uri: Uri?){
        if (uri == null){
            selectDiscovery.setText("default")
            return
        }
        val filePath = FileHelper.getRealPathFromURI(this, uri)!!
        val fileName = filePath.substring( filePath.lastIndexOf('/') + 1, filePath.length)
        selectDiscovery.setText(fileName)
        Log.d(TAG, "handleSelectDiscoveryFile - file path: $filePath")
        mockServer.config.discoveryPath = filePath
    }




    fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11 (R) or above
            Environment.isExternalStorageManager()
        } else {
            //Below android 11
            val write =
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read =
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private val STORAGE_PERMISSION_CODE = 23

    private val storageActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    //Android is 11 (R) or above
                    if (Environment.isExternalStorageManager()) {
                        //Manage External Storage Permissions Granted
                        Log.d(TAG, "onActivityResult: Manage External Storage Permissions Granted")
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Storage Permissions Denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    //Below android 11
                }
            })
    private fun requestForStoragePermissions() {
        //Android is 11 (R) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

//    fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>?,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults)
//        if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (grantResults.size > 0) {
//                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
//                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
//                if (read && write) {
//                    Toast.makeText(
//                        this@MainActivity,
//                        "Storage Permissions Granted",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    Toast.makeText(
//                        this@MainActivity,
//                        "Storage Permissions Denied",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

}
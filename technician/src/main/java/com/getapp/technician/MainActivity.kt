package com.getapp.technician

import GetApp.Client.models.CreateImportResDto
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
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
    private val PICK_MAP_FILE = 2
    private val PICK_JSON_FILE = 3


    private lateinit var failedValidation: SwitchCompat
    private lateinit var fastDownload: SwitchCompat
    private lateinit var selectDiscovery: Button
    private lateinit var selectMap: Button
    private lateinit var textJson: TextView
    private lateinit var selectJson: Button
    private lateinit var importCreate: RadioGroup

    lateinit var mockServer: MockServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        failedValidation = findViewById(R.id.failed_validation)
//        fastDownload = findViewById(R.id.fast_download)
        selectDiscovery = findViewById<Button>(R.id.select_discovery)
        importCreate = findViewById(R.id.radio_import_group)
        selectMap = findViewById(R.id.select_map_file)
        textJson = findViewById(R.id.json_text_view)
        selectJson = findViewById(R.id.select_json_file)


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

        selectMap.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"

            }
            startActivityForResult(intent, PICK_MAP_FILE)
        }

        selectJson.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"

            }
            startActivityForResult(intent, PICK_JSON_FILE)
        }

        importCreate.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radio_import_success){
                mockServer.config.importCreateStatus = CreateImportResDto.Status.inProgress
            }else if(checkedId == R.id.radio_import_error){
                mockServer.config.importCreateStatus = CreateImportResDto.Status.error
            }
        }

        if (!checkStoragePermissions()){
            requestForStoragePermissions()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null){
            return
        }
        val selectedFileUri = data.data
        when(requestCode){
            PICK_DISCOVERY_FILE -> {
                Log.d(TAG, "onActivityResult - selected discovery: $selectedFileUri")
                handleSelectDiscoveryFile(selectedFileUri)
            }
            PICK_MAP_FILE -> {
                Log.d(TAG, "onActivityResult - selected map file: $selectedFileUri")
                handleSelectMapFile(selectedFileUri)

            }
            PICK_JSON_FILE -> {
                Log.d(TAG, "onActivityResult - selected json file: $selectedFileUri")
                handleSelectJsonFile(selectedFileUri)
            }
        }

    }
    private fun handleSelectDiscoveryFile(uri: Uri?){
        if (uri == null){
            selectDiscovery.text = "default"
            mockServer.config.discoveryPath = null
            return
        }
        val filePath = FileHelper.getRealPathFromURI(this, uri)!!
        val fileName = filePath.substring( filePath.lastIndexOf('/') + 1, filePath.length)
        selectDiscovery.text = fileName
        Log.d(TAG, "handleSelectDiscoveryFile - file path: $filePath")
        mockServer.config.discoveryPath = filePath
    }
    private fun handleSelectMapFile(uri: Uri?){
        if (uri == null){
            selectMap.text = "default"
            mockServer.config.mapPath = null
            mockServer.config.jsonPath = null
            textJson.visibility = View.GONE
            selectJson.visibility = View.GONE
            return
        }

        val filePath = FileHelper.getRealPathFromURI(this, uri)!!
        val fileName = filePath.substring( filePath.lastIndexOf('/') + 1, filePath.length)
        selectMap.text = fileName
        Log.d(TAG, "handleSelectMapFile - file path: $filePath")
        mockServer.config.mapPath = filePath

        textJson.visibility = View.VISIBLE
        selectJson.visibility = View.VISIBLE
    }


    private fun handleSelectJsonFile(uri: Uri?){
        if (uri == null){
            selectJson.text = "default"
            mockServer.config.jsonPath = null
            return
        }
        val filePath = FileHelper.getRealPathFromURI(this, uri)!!
        val fileName = filePath.substring( filePath.lastIndexOf('/') + 1, filePath.length)
        selectJson.text = fileName
        Log.d(TAG, "handleSelectMapFile - file path: $filePath")
        mockServer.config.jsonPath = filePath

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
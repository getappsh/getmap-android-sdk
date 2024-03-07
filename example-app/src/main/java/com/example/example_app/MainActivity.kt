package com.example.example_app

import android.os.StatFs
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import com.arcgismaps.geometry.Point
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.R)
class MainActivity : AppCompatActivity(), DownloadListAdapter.OnSignalListener {

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var mapServiceManager: MapServiceManager
    private var progressDialog: ProgressDialog? = null

    //    private lateinit var service: GetMapService
    private lateinit var updateDate: LocalDateTime
    private lateinit var selectedProduct: DiscoveryItem

    //    private lateinit var selectedProductView: TextView
    private lateinit var deliveryButton: Button
    private lateinit var scanQRButton: Button
    private lateinit var pathSd: String
    private lateinit var syncButton: ImageButton

    //    private lateinit var mapServiceManager: MapServiceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadListAdapter: DownloadListAdapter

    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (mapServiceManager.isInit) {
            CoroutineScope(Dispatchers.Default).launch { mapServiceManager.service.synchronizeMapData() }
        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapServiceManager = MapServiceManager.getInstance()

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            val uri = Uri.fromParts("package", this.packageName, null)
            intent.data = uri
            startForResult.launch(intent)
        }
        val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
        availableSpace.text = GetAvailableSpaceInSdCard()

        //Get the path of SDCard
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;
        val volume =
            if (storageList[1].directory?.absoluteFile != null) storageList[1].directory?.absoluteFile else Toast.makeText(
                applicationContext,
                "Please insert a SdCard !",
                Toast.LENGTH_SHORT
            ).show()
        val pathSd = ("${volume}/com.asio.gis/gis/maps/raster/מיפוי ענן")
        if (!mapServiceManager.isInit) {
            var url = Pref.getInstance(this).baseUrl
            if (url.isEmpty()) url =
                "https://api-asio-getapp-2.apps.okd4-stage-getapp.getappstage.link"
            val cfg = Configuration(
                url,
//            "http://getapp-test.getapp.sh:3000",
//            "http://192.168.2.26:3000",
                "rony@example.com",
                "rony123",
//            File("/storage/1115-0C18/com.asio.gis").path,
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
                pathSd,
                16,

                null
            )

            try {
                mapServiceManager.initService(applicationContext, cfg)
            } catch (_: Exception) {
            }
        }
//
//        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
//        service.setOnInventoryUpdatesListener {
//            val data = it.joinToString()
//            runOnUiThread { Toast.makeText(this, data, Toast.LENGTH_LONG).show() }
//            Log.d(TAG, "onCreate - setOnInventoryUpdatesListener: $data")
//
//        }


        dismissLoadingDialog()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        downloadListAdapter = DownloadListAdapter({ bId, mapId, pathSd ->
            when (bId) {
                DownloadListAdapter.RESUME_BUTTON_CLICK -> onResume(mapId)
                DownloadListAdapter.CANCEL_BUTTON_CLICK -> onCancel(mapId)
                DownloadListAdapter.DELETE_BUTTON_CLICK -> onDelete(mapId)
                DownloadListAdapter.QR_CODE_BUTTON_CLICK -> generateQrCode(mapId)
                DownloadListAdapter.UPDATE_BUTTON_CLICK -> updateMap(mapId)
                DownloadListAdapter.ITEM_VIEW_CLICK -> itemViewClick(
                    mapId,
                    downloadListAdapter.availableUpdate
                )
            }
        }, pathSd)
        //Set the adapter to listen to changes
        downloadListAdapter.setOnSignalListener(this)

        recyclerView.adapter = downloadListAdapter
        //Separator code between each download, optional
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                baseContext,
                layoutManager.orientation
            )
        )
        mapServiceManager.service.getDownloadedMapsLive().observe(this, Observer {
            Log.d(TAG, "onCreate - data changed ${it.size}")
            downloadListAdapter.saveData(it)
        })

        val discovery = findViewById<Button>(R.id.discovery)
        discovery.setOnClickListener {
            this.onDiscovery()

        }

//        deliveryButton = findViewById<Button>(R.id.delivery)
//        deliveryButton.isEnabled = false
//        deliveryButton.setOnClickListener {
//            this.onDelivery()
//        }

        CoroutineScope(Dispatchers.Default).launch { mapServiceManager.service.synchronizeMapData() }
        syncButton = findViewById<ImageButton>(R.id.Sync)
        syncButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                mapServiceManager.service.synchronizeMapData()
            }
        }

        scanQRButton = findViewById<Button>(R.id.scanQR)
        scanQRButton.setOnClickListener {
            barcodeLauncher.launch(ScanOptions())
        }

        Thread {
            mapServiceManager.service.getDownloadedMaps().forEach {
                mapServiceManager.service.registerDownloadHandler(it.id!!, downloadStatusHandler)
            }
        }.start()

        val settingButton = findViewById<ImageButton>(R.id.SettingsButton)
        settingButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra(
                "pathSd",
                pathSd
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        mapServiceManager = MapServiceManager.getInstance()
//        Log.d("a", "sa")
    }

    private fun onDiscovery() {
        Log.d(TAG, "onDiscovery");
        showLoadingDialog("פותח את המפה")
        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties("dummy product", "1,2,3,4", false)
            try {

                val products = mapServiceManager.service.getDiscoveryCatalog(props)
                Log.d(TAG, "discovery products: " + products);
                products.forEach { product ->
                    Log.d(
                        "products1",
                        "Id : ${product.id} And Coordinates : ${product.footprint}"
                    )
                }
                launch(Dispatchers.Main) {
                    // Display the response in an AlertDialog
                    dismissLoadingDialog()
                    DiscoveryProductsManager.getInstance().updateProducts(products)
                    val intent = Intent(this@MainActivity, MapActivity::class.java)
                    startActivity(intent)
//                    discoveryDialogPicker(products)

                }
            } catch (e: Exception) {
                // Handle any exceptions here
                Log.e(TAG, "error: " + e);
                launch(Dispatchers.Main) {
                    dismissLoadingDialog()
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()

                }
            }

        }

    }

    private fun onDelivery(first: Point, second: Point) {
        Log.d(TAG, "onDelivery: ");
        GlobalScope.launch(Dispatchers.IO) {

//            service.purgeCache()

            val props = MapProperties(
                "selectedProduct.id",
//                "34.46641783,31.55079535, 34.47001187,31.55095355, 34.4700189, 31.553150863,34.46641783, 31.55318508, 34.46641783, 31.55079535",
//                    "34.50724201341369,31.602641553384572,34.5180453565571,31.59509118055151,34.50855899068993,31.5815177494226,34.497755647546515,31.589068122255644,34.50724201341369,31.602641553384572",
//                "34.47956403,31.52202192,34.51125354,31.54650531",
//                "34.33390512,31.39424661,34.33937683,31.39776391",// json dose not exist on s3 for this bBox
                "${first.y},${first.x},${second.y},${second.x}",

                false
            )
            val id = mapServiceManager.service.downloadMap(props, downloadStatusHandler);
            if (id == null) {
                this@MainActivity.runOnUiThread {
                    // This is where your UI code goes.
                    Toast.makeText(
                        applicationContext,
                        "The map already exists, please choose another Bbox",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
//            val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
//            availableSpace.text = GetAvailableSpaceInSdCard()

            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
        }

    }

    fun GetAvailableSpaceInSdCard(): String {

        val externalFilesDirs = getExternalFilesDirs(null)
        var sdCardDirectory: File? = null

        for (file in externalFilesDirs) {
            if (Environment.isExternalStorageRemovable(file)) {
                sdCardDirectory = file
                break
            }
        }
        sdCardDirectory?.let {

            val stat = StatFs(it.absolutePath)
            val bytesAvailable: Long = stat.blockSizeLong * stat.availableBlocksLong
            val gigabytesAvailable = bytesAvailable.toDouble() / (1024 * 1024 * 1024)
            val megabytesAvailable = bytesAvailable.toDouble() / (1024 * 1024)

            return if (gigabytesAvailable >= 1) {
                String.format("מקום פנוי להורדה: %.2f GB", gigabytesAvailable)
            } else {
                String.format("מקום פנוי להורדה: %.2f MB", megabytesAvailable)
            }
        }
        return "Not Found"
    }

    private fun discoveryDialogPicker(products: List<DiscoveryItem>) {
        Log.d(TAG, "dialogPicker")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose product")

        val productsStrings = products.map { it.productName }.toTypedArray()
        val checkedItem = -1
        builder.setSingleChoiceItems(productsStrings, checkedItem) { dialog, which ->
            selectedProduct = products[which]
            Log.d(TAG, "dialogPicker: selected item " + selectedProduct.productName)

//            selectedProductView.text = ("Selected Product:\n" + selectedProduct.productName)
            deliveryButton.isEnabled = true
            updateDate = selectedProduct.ingestionDate!!.toLocalDateTime()

        }

        builder.setPositiveButton("OK") { dialog, which ->

        }
//        builder.setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun onDelete(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.deleteMap(id)
        }
    }

    private fun onCancel(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.cancelDownload(id)
        }
    }

    private fun onResume(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.resumeDownload(id, downloadStatusHandler)
        }

    }

    // Function that will update the AvailableSpace
    override fun onSignal() {
        val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
        availableSpace.text = GetAvailableSpaceInSdCard()
    }


    private fun generateQrCode(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val qrCode = mapServiceManager.service.generateQrCode(id, 1000, 1000)
                runOnUiThread { showQRCodeDialog(qrCode) }
            } catch (e: Exception) {
                runOnUiThread { showErrorDialog(e.message.toString()) }
            }

        }
    }

    private fun updateMap(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.downloadUpdatedMap(id, downloadStatusHandler)
        }
    }

    private fun itemViewClick(id: String, availableUpdate: Boolean) {
        if (availableUpdate) {
            CoroutineScope(Dispatchers.IO).launch {
                mapServiceManager.service.downloadUpdatedMap(id, downloadStatusHandler)
            }
        } else {

            GlobalScope.launch(Dispatchers.IO) {
                val map = mapServiceManager.service.getDownloadedMap(id)
                val str = map?.let {
                    "The id is =${it.id}, \n" +
//                        "footprint=${it.footprint}, \n" +
//                        "fileName=${it.fileName}, \n" +
//                        "jsonName=${it.jsonName}, \n" +
//                        "deliveryStatus=${it.deliveryStatus}, \n" +
//                        "url=${it.url}, \n" +
                            "The status is:${it.statusMsg}, \n" +
                            "The download is:${it.progress}, \n" +
//                        "errorContent=${it.errorContent}, \n" +
                            "The bbox is updated:${it.isUpdated}, \n "
                }.toString()
                runOnUiThread { showDialog(str) }
            }
        }


    }

    private fun showErrorDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun showDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showLoadingDialog(title: String, id: String? = null) {

//        var percentages = findViewById<TextView>(R.id.Percentages)
////        var percentages= R.id.Percentages
//
////        percentages = i
//        Log.i("PROGRESSBAR", "showLoadingDialog: ")
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle(title)
        progressDialog?.setMessage("מפה בטעינה...") // Set the message to be displayed
        progressDialog?.setCancelable(false) // Prevent users from dismissing the dialog
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER) // Use a spinner-style progress indicator
        if (id != null) {
            progressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, "בטל",
                DialogInterface.OnClickListener { dialog, which ->
                    mapServiceManager.service.cancelDownload(id)
                    progressDialog?.dismiss() //dismiss dialog
                })
        }
        progressDialog?.show()

    }

    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }


    private fun showQRCodeDialog(qrCodeBitmap: Bitmap) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_qr_code, null)

        val imageViewQRCode: ImageView = dialogView.findViewById(R.id.imageViewQRCode)
        imageViewQRCode.setImageBitmap(qrCodeBitmap)

        builder.setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    mapServiceManager.service.processQrCodeData(result.contents) {
                        Log.d(TAG, "on data change: $it")
                    }
                } catch (e: Exception) {
                    runOnUiThread { showErrorDialog(e.message.toString()) }
                }

            }
        }
    }
}
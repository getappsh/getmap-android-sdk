package com.example.example_app

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.qualifiedName

    private var progressDialog: ProgressDialog? = null

    private lateinit var service: GetMapService
    private lateinit var updateDate: LocalDateTime
    private lateinit var selectedProduct: DiscoveryItem

    private lateinit var selectedProductView: TextView
    private lateinit var deliveryButton: Button
    private lateinit var scanQRButton: Button

    private lateinit var syncButton: Button

    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadListAdapter: DownloadListAdapter




    private val downloadStatusHandler :(MapDownloadData) -> Unit = { data ->
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Environment.isExternalStorageManager()){
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            val uri = Uri.fromParts("package", this.packageName, null)
            intent.data = uri
            startActivity(intent)
        }


        val cfg = Configuration(
            "http://getapp-test.getapp.sh:3000",
//            "http://192.168.2.26:3000",
//            "http://getapp-dev.getapp.sh:3000",
//            "http://localhost:3333",
            "rony@example.com",
            "rony123",
//            File("/storage/1115-0C18/com.asio.gis").path,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
            16,
            null
        )

        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
        service.setOnInventoryUpdatesListener {
            val data = it.joinToString()
            runOnUiThread{Toast.makeText(this, data, Toast.LENGTH_LONG).show()}
            Log.d(TAG, "onCreate - setOnInventoryUpdatesListener: $data")

        }
        dismissLoadingDialog()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        downloadListAdapter = DownloadListAdapter(){bId, mapId ->
            when(bId){
                DownloadListAdapter.RESUME_BUTTON_CLICK -> onResume(mapId)
                DownloadListAdapter.CANCEL_BUTTON_CLICK -> onCancel(mapId)
                DownloadListAdapter.DELETE_BUTTON_CLICK -> onDelete(mapId)
                DownloadListAdapter.QR_CODE_BUTTON_CLICK -> generateQrCode(mapId)
                DownloadListAdapter.UPDATE_BUTTON_CLICK -> updateMap(mapId)
                DownloadListAdapter.ITEM_VIEW_CLICK -> itemViewClick(mapId)
            }
        }
        recyclerView.adapter = downloadListAdapter

        service.getDownloadedMapsLive().observe(this, Observer {
            Log.d(TAG, "onCreate - data changed ${it.size}")
            downloadListAdapter.saveData(it)
        })

        selectedProductView = findViewById<TextView>(R.id.selectedProduct)

        val discovery = findViewById<Button>(R.id.discovery)
        discovery.setOnClickListener {
            this.onDiscovery()
        }


        deliveryButton = findViewById<Button>(R.id.delivery)
        deliveryButton.isEnabled = false
        deliveryButton.setOnClickListener {
            this.onDelivery()
        }

        syncButton = findViewById<Button>(R.id.d_test)

        syncButton.setOnClickListener{
            GlobalScope.launch(Dispatchers.IO) {
                service.synchronizeMapData()
            }
        }


        scanQRButton = findViewById<Button>(R.id.scanQR)
        scanQRButton.setOnClickListener {
            barcodeLauncher.launch(ScanOptions())
        }

        Thread{
            service.getDownloadedMaps().forEach {
                service.registerDownloadHandler(it.id!!, downloadStatusHandler)
            }
        }.start()


    }

    private fun onDiscovery(){
        Log.d(TAG, "onDiscovery");
        showLoadingDialog("Discovery")
        GlobalScope.launch (Dispatchers.IO) {
            val props = MapProperties("dummy product","1,2,3,4",false)
            try {
                val products = service.getDiscoveryCatalog(props)
                Log.d(TAG, "discovery products: " + products);

                launch(Dispatchers.Main) {
                    // Display the response in an AlertDialog
                    dismissLoadingDialog()
                    discoveryDialogPicker(products)

                }
            }catch (e: Exception) {
                // Handle any exceptions here
                Log.e(TAG, "error: "+ e );
                launch(Dispatchers.Main) {
                    dismissLoadingDialog()

                }
            }

        }

    }


    private fun onDelivery(){
        Log.d(TAG, "onDelivery: ");
        GlobalScope.launch(Dispatchers.IO){

//            service.purgeCache()

            val props = MapProperties(
                selectedProduct.id,
//                "34.46641783,31.55079535, 34.47001187,31.55095355, 34.4700189, 31.553150863,34.46641783, 31.55318508, 34.46641783, 31.55079535",
//                    "34.50724201341369,31.602641553384572,34.5180453565571,31.59509118055151,34.50855899068993,31.5815177494226,34.497755647546515,31.589068122255644,34.50724201341369,31.602641553384572",
//                "34.47956403,31.52202192,34.51125354,31.54650531",
//                "34.33390512,31.39424661,34.33937683,31.39776391",// json dose not exist on s3 for this bBox
                "34.46087927,31.48921097,34.47834067,31.50156331",
                false
            )
            val id = service.downloadMap(props, downloadStatusHandler);

            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
        }

    }

    private fun discoveryDialogPicker(products: List<DiscoveryItem>){
        Log.d(TAG, "dialogPicker")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose product")

        val productsStrings = products.map { it.productName }.toTypedArray()
        val checkedItem = -1
        builder.setSingleChoiceItems(productsStrings, checkedItem) { dialog, which ->
            selectedProduct = products[which]
            Log.d(TAG, "dialogPicker: selected item " + selectedProduct.productName)

            selectedProductView.text = ("Selected Product:\n" + selectedProduct.productName)
            deliveryButton.isEnabled = true
            updateDate = selectedProduct.ingestionDate!!.toLocalDateTime()

        }

        builder.setPositiveButton("OK") { dialog, which ->

        }
//        builder.setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun onDelete(id: String){
        GlobalScope.launch(Dispatchers.IO) {
            service.deleteMap(id)
        }
    }

    private fun onCancel(id: String){
        GlobalScope.launch(Dispatchers.IO) {
            service.cancelDownload(id)
        }
    }

    private fun onResume(id: String){
        GlobalScope.launch(Dispatchers.IO) {
            service.resumeDownload(id, downloadStatusHandler)
        }
    }

    private fun generateQrCode(id: String){
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val qrCode = service.generateQrCode(id, 1000, 1000)
                runOnUiThread { showQRCodeDialog(qrCode) }
            }catch (e: Exception){
                runOnUiThread { showErrorDialog(e.message.toString()) }
            }

        }
    }

    private fun updateMap(id: String){
        GlobalScope.launch(Dispatchers.IO) {
            service.downloadUpdatedMap(id,  downloadStatusHandler)
        }
    }

    private fun itemViewClick(id: String){
        Log.d(TAG, "itemViewClick - id $id")
        GlobalScope.launch(Dispatchers.IO) {
            val map = service.getDownloadedMap(id)
            val str = map?.let {
                        "id=${it.id}, \n" +
//                        "footprint=${it.footprint}, \n" +
//                        "fileName=${it.fileName}, \n" +
//                        "jsonName=${it.jsonName}, \n" +
//                        "deliveryStatus=${it.deliveryStatus}, \n" +
//                        "url=${it.url}, \n" +
                        "statusMessage=${it.statusMessage}, \n" +
                        "downloadProgress=${it.downloadProgress}, \n" +
//                        "errorContent=${it.errorContent}, \n" +
                        "isUpdated=${it.isUpdated}, \n " +
                        "downloadStart=${it.downloadStart}, \n" +
                        "downloadStop=${it.downloadStop}, \n" +
                        "downloadDone=${it.downloadDone}"
            }.toString()
            runOnUiThread { showDialog(str) }
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


    private fun showDialog(msg: String){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        val dialog = builder.create()
        dialog.show()
    }
    private fun showLoadingDialog(title: String, id: String? = null) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle(title)
        progressDialog?.setMessage("Loading...") // Set the message to be displayed
        progressDialog?.setCancelable(false) // Prevent users from dismissing the dialog
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER) // Use a spinner-style progress indicator
        if(id != null){
            progressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Download",
                DialogInterface.OnClickListener { dialog, which ->
                    this.service.cancelDownload(id)
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
                try{
                    service.processQrCodeData(result.contents){
                        Log.d(TAG, "on data change: $it")
                    }
                }catch (e: Exception){
                    runOnUiThread { showErrorDialog(e.message.toString()) }
                }

            }
        }
    }
}
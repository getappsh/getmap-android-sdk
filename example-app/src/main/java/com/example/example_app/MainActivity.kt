package com.example.example_app

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName

    private var progressDialog: ProgressDialog? = null

    private var downloadDialog: Dialog? = null
    private var progressBar: ProgressBar? = null

    private lateinit var service: GetMapService
    private lateinit var updateDate: LocalDateTime
    private lateinit var selectedProduct: DiscoveryItem

    private lateinit var selectedProductView: TextView
    private lateinit var deliveryButton: Button

    private lateinit var downoadnTestButton: Button
    
    private var downloadId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cfg = Configuration(
            "http://getapp-dev.getapp.sh:3000",
//            "http://localhost:3333",
//            "http://192.168.2.26:3000",
            "rony@example.com",
            "rony123",
            //currently downloads file to a path within the public external storage directory
            Environment.DIRECTORY_DOWNLOADS,
            16,
            5,5,
            null
        )


        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
        dismissLoadingDialog()

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

        downoadnTestButton = findViewById<Button>(R.id.d_test)

        downoadnTestButton.setOnClickListener{

            GlobalScope.launch(Dispatchers.IO) {
                var downloads = service.getDownloadedMaps()
                Log.d(TAG, "onCreate - downloads size before ${downloads.size}")
                service.cleanDownloads()

                downloads = service.getDownloadedMaps()
                Log.d(TAG, "onCreate - downloads size after ${downloads.size}")

            }
//            service.cancelDownload("1")
//            try {
//                GlobalScope.launch(Dispatchers.IO){
//                    val map = service.getDownloadedMap("1")
//                    Log.d(TAG, "onCreate: ${map.toString()}")
//                    val res = service.getDownloadedMaps()
//                    Log.d(TAG, "onCreate: ${res.toString()}")
//                }
////                service.deleteMap(downloadId!!)
//
//
//            }catch (e: Exception){
//                Log.e(TAG, "onCreate - delete map, error: ${e.message.toString()}", )
//            }
//            val downloader = PackageDownloader(this, Environment.DIRECTORY_DOWNLOADS)
//
//            GlobalScope.launch(Dispatchers.IO){
//
//                var completed = false
//                var downloadId: Long = -1
//                val downloadCompletionHandler: (Long) -> Unit = {
//                    println("processing download ID=$it completion event...")
//                    completed = it == downloadId
//                }
//
//                downloadId = downloader.downloadFile(
//                    //"http://getmap-dev.getapp.sh/api/Download/OrthophotoBest_jordan_crop_1_0_12_2023_08_17T14_43_55_716Z.gpkg",
////                    "http://getmap-dev.getapp.sh/api/Download/dwnld-test123.gpkg",
//                    "https://download.maps.pkz.even/api/raster/v1/downloads/6ef8eac0889a49e6a291a1807a097e6d/Orthophoto_O_aza_w84geo_Oct23_gpkg_19_0_2_1_0_19_2023_11_12T14_38_52_992Z.gpkg",
//                    downloadCompletionHandler
//                )
//            }
        }

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
//                "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
//                "34.46264631,31.48939470,34.46454410,31.49104920",
                "34.47146482,31.55712952,34.48496631,31.56652669",
//                "34.46087927,31.48921097,34.47834067,31.50156334"
                false
            )
            val downloadStatusHandler :(MapDownloadData) -> Unit = { data ->
                Log.d(TAG, "onDelivery data id: ${data.id}")
                runOnUiThread {
                    progressDialog?.setMessage("Loading... \nstatus: ${data.statusMessage} \nprogress: ${data.downloadProgress} \nerror: ${data.errorContent}")

                }

                Log.d(TAG, "onDelivery: status ${data.deliveryStatus}, progress ${data.downloadProgress} heb status ${data.statusMessage}, reason ${data.errorContent}");
                if (data.deliveryStatus == MapDeliveryState.DONE ||
                    data.deliveryStatus == MapDeliveryState.ERROR ||
                    data.deliveryStatus == MapDeliveryState.CANCEL ){
                    Log.d(TAG, "onDelivery: ${data.deliveryStatus}")
                    dismissLoadingDialog();
//                showMessageDialog(delivered.toString())
                    runOnUiThread{
                        Toast.makeText(this@MainActivity, data.errorContent, Toast.LENGTH_LONG).show()

                    }
                }
            }
            val id = service.downloadMap(props, downloadStatusHandler);
            downloadId = id
            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
            GlobalScope.launch(Dispatchers.Main){
                showLoadingDialog("Download file id: $id", id)
            }

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

            selectedProductView.setText("Selected Product: " + selectedProduct.productName)
            deliveryButton.isEnabled = true
            updateDate = selectedProduct.ingestionDate!!.toLocalDateTime()

        }


// add OK and Cancel buttons
        builder.setPositiveButton("OK") { dialog, which ->

        }
        builder.setNegativeButton("Cancel", null)

// create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun showMessageDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Response Data")

        // Set the response data as the message in the AlertDialog
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

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

    // Call this function to dismiss the loading dialog
    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }
}